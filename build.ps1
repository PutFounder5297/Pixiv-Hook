# Offline build for the pixiv LSPosed module (no Gradle, no network).
# Toolchain: Android Studio JBR 21 + Android SDK build-tools 37 + platform android-36.1
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jbr  = 'D:\Apps\Andriod Studio\jbr'
$sdk  = "$env:LOCALAPPDATA\Android\Sdk"
if (-not (Test-Path "$sdk\build-tools")) { $sdk = 'C:\Users\junbo\AppData\Local\Android\Sdk' }
$bt = Get-ChildItem "$sdk\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1
$bt = $bt.FullName
$androidJar = Join-Path $sdk 'platforms\android-36.1\android.jar'
if (-not (Test-Path $androidJar)) { $androidJar = Get-ChildItem "$sdk\platforms" -Directory | Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'android.jar' } | Select-Object -First 1 }
$build = Join-Path $root 'build'
$out   = Join-Path $root 'dist'

Write-Host "SDK build-tools: $bt"
Write-Host "android.jar    : $androidJar"

if (Test-Path $build) { Remove-Item $build -Recurse -Force }
New-Item -ItemType Directory -Force "$build\api", "$build\classes", "$build\dex", $out | Out-Null

function Invoke-Javac {
    param([string]$OutDir, [string[]]$JavacArgs)
    & "$jbr\bin\javac.exe" $JavacArgs -d $OutDir
    if ($LASTEXITCODE -ne 0) { throw "javac failed" }
}

# 1) compile API stubs (compile-only, never packaged)
$apiSrc = Get-ChildItem "$root\api" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$apiArgs = @('-nowarn', '-encoding', 'UTF-8', '-classpath', $androidJar) + $apiSrc
Invoke-Javac $build\api $apiArgs

# 2) compile module sources
$src = Get-ChildItem "$root\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$cp = "$androidJar;$build\api"
$modArgs = @('-source', '8', '-target', '8', '-nowarn', '-encoding', 'UTF-8', '-classpath', $cp) + $src
Invoke-Javac "$build\classes" $modArgs

# 3) dex
$classFiles = Get-ChildItem "$build\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& "$jbr\bin\java.exe" -cp "$bt\lib\d8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib $androidJar --output "$build\dex" $classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

# 4) resources
& "$bt\aapt2.exe" compile --dir "$root\res" -o "$build\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

& "$bt\aapt2.exe" link -o "$build\app-unsigned.apk" -I $androidJar `
    --manifest "$root\AndroidManifest.xml" `
    --min-sdk-version 26 --target-sdk-version 36 `
    --version-code 1 --version-name '1.0' `
    -R "$build\res.zip" --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

# 5) put classes.dex + assets into the apk
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$build\app-unsigned.apk", 'Update')
foreach ($pair in @(@('classes.dex', "$build\dex\classes.dex"), @('assets/xposed_init', "$root\assets\xposed_init"))) {
    $old = $zip.GetEntry($pair[0]); if ($old) { $old.Delete() }
    $e = $zip.CreateEntry($pair[0], [System.IO.Compression.CompressionLevel]::Optimal)
    $s = $e.Open(); $bytes = [System.IO.File]::ReadAllBytes($pair[1]); $s.Write($bytes, 0, $bytes.Length); $s.Close()
}
$zip.Dispose()

# 6) align + sign
& "$bt\zipalign.exe" -f 4 "$build\app-unsigned.apk" "$build\app-aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

$ks = Join-Path $root 'debug.keystore'
if (-not (Test-Path $ks)) {
    & "$jbr\bin\keytool.exe" -genkeypair -v -keystore $ks -storepass android -keypass android `
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=Android Debug,O=Android,C=US"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}

$final = Join-Path $out 'pixiv-adclean.apk'
& "$jbr\bin\java.exe" -jar "$bt\lib\apksigner.jar" sign --ks $ks --ks-pass pass:android --key-pass pass:android `
    --ks-key-alias androiddebugkey --out $final "$build\app-aligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

& "$jbr\bin\java.exe" -jar "$bt\lib\apksigner.jar" verify --print-certs $final | Select-Object -First 6
Write-Host "BUILT: $final"

