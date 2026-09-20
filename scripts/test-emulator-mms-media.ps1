[CmdletBinding()]
param(
    [string]$AvdName = "PixelPhone-API35",
    [string]$Serial = "emulator-5554",
    [string]$Sender = "5558675309",
    [int]$TimeoutSeconds = 180,
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$KeepEmulator
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$sdkRoot = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} elseif ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$emulator = Join-Path $sdkRoot "emulator\emulator.exe"
$packageName = "com.caelh.textblock.debug"
$receiver = "dev.octoshrimpy.quik.debug.MmsFixtureReceiver"
$apkDirectory = Join-Path $repoRoot "presentation\build\outputs\apk\debug"
$startedEmulator = $false

function Invoke-Adb {
    param([string[]]$CommandArgs)
    $output = & $adb -s $Serial @CommandArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $($CommandArgs -join ' ')`n$output"
    }
    return ($output | Out-String).Trim()
}

function Wait-Until {
    param(
        [scriptblock]$Condition,
        [string]$FailureMessage,
        [int]$Seconds = 20
    )
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        if (& $Condition) { return }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw $FailureMessage
}

if (!(Test-Path -LiteralPath $adb) -or !(Test-Path -LiteralPath $emulator)) {
    throw "Android SDK tools were not found under $sdkRoot"
}

$devicePattern = "(?m)^$([regex]::Escape($Serial))\s+device\b"
if ((& $adb devices | Out-String) -notmatch $devicePattern) {
    Start-Process -FilePath $emulator -ArgumentList @(
        "-avd", $AvdName, "-no-window", "-no-audio", "-no-boot-anim",
        "-gpu", "swiftshader_indirect"
    ) -WindowStyle Hidden
    $startedEmulator = $true
}

try {
    Wait-Until -Seconds $TimeoutSeconds -FailureMessage "Emulator $Serial did not finish booting" -Condition {
        $state = (& $adb -s $Serial get-state 2>$null | Out-String).Trim()
        if ($state -ne "device") { return $false }
        return ((& $adb -s $Serial shell getprop sys.boot_completed 2>$null | Out-String).Trim() -eq "1")
    }

    if (!$SkipBuild) {
        $javaHome = $env:JAVA_HOME
        if (!$javaHome) {
            $javaHome = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" |
                Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
        }
        if (!$javaHome) { throw "JDK 17 was not found" }
        $env:JAVA_HOME = $javaHome
        $env:Path = "$javaHome\bin;$env:Path"

        Push-Location $repoRoot
        try {
            $buildOutput = & .\gradlew.bat :presentation:assembleDebug 2>&1
            if (($buildOutput | Out-String) -notmatch "BUILD SUCCESSFUL") {
                throw "Gradle build failed`n$($buildOutput | Select-Object -Last 100 | Out-String)"
            }
        } finally {
            Pop-Location
        }
    }

    $apk = Get-ChildItem -LiteralPath $apkDirectory -Filter "*.apk" |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
    if (!$apk) { throw "No debug APK found under $apkDirectory" }
    if (!$SkipInstall) {
        Invoke-Adb -CommandArgs @("install", "-r", $apk) | Out-Null
    }

    Invoke-Adb -CommandArgs @("shell", "pm", "grant", $packageName, "android.permission.POST_NOTIFICATIONS") | Out-Null
    Invoke-Adb -CommandArgs @("shell", "cmd", "role", "add-role-holder", "android.app.role.SMS", $packageName) | Out-Null
    Invoke-Adb -CommandArgs @("shell", "monkey", "-p", $packageName, "-c", "android.intent.category.LAUNCHER", "1") | Out-Null
    Start-Sleep -Seconds 2
    Invoke-Adb -CommandArgs @("logcat", "-c") | Out-Null

    $expectedMimeTypes = [ordered]@{
        jpeg = "image/jpeg"
        png = "image/png"
        gif = "image/gif"
        webp = "image/webp"
        malformed = "image/jpeg"
    }
    $runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
    $injectedUris = @()

    foreach ($fixture in $expectedMimeTypes.GetEnumerator()) {
        $broadcast = Invoke-Adb -CommandArgs @(
            "shell", "am", "broadcast", "-W",
            "-a", "com.caelh.textblock.debug.action.INJECT_MMS_FIXTURE",
            "-n", "$packageName/$receiver",
            "--es", "kind", $fixture.Key,
            "--es", "sender", $Sender,
            "--es", "runId", $runId
        )
        if ($broadcast -notmatch 'result=0') {
            throw "Fixture $($fixture.Key) broadcast failed`n$broadcast"
        }
        $messageId = ""
        Wait-Until -FailureMessage "Fixture $($fixture.Key) did not create an MMS provider row" -Condition {
            $messages = Invoke-Adb -CommandArgs @(
                "shell", "content", "query", "--uri", "content://mms/inbox",
                "--projection", "_id:m_id"
            )
            $fixtureRow = [regex]::Match(
                $messages,
                "(?m)_id=(\d+), m_id=textblock-fixture-$([regex]::Escape($runId))-$([regex]::Escape($fixture.Key))"
            )
            if (!$fixtureRow.Success) { return $false }
            $script:messageId = $fixtureRow.Groups[1].Value
            return $true
        }
        $messageUri = "content://mms/inbox/$messageId"
        $injectedUris += $messageUri

        Wait-Until -FailureMessage "Fixture $($fixture.Key) parts were not persisted" -Condition {
            $parts = Invoke-Adb -CommandArgs @("shell", "content", "query", "--uri", "content://mms/$messageId/part")
            return $parts -match [regex]::Escape("ct=$($fixture.Value)") -and
                $parts -match [regex]::Escape("ct=text/plain")
        }
    }

    Wait-Until -Seconds 30 -FailureMessage "MMS fixtures did not reach the TextBlock conversation UI" -Condition {
        Invoke-Adb -CommandArgs @(
            "shell", "am", "start", "-W", "-a", "android.intent.action.VIEW",
            "-d", "smsto:$Sender", "-p", $packageName
        ) | Out-Null
        Start-Sleep -Seconds 1
        Invoke-Adb -CommandArgs @("shell", "uiautomator", "dump", "/sdcard/textblock-mms.xml") | Out-Null
        $ui = Invoke-Adb -CommandArgs @("shell", "cat", "/sdcard/textblock-mms.xml")
        return $ui -match 'resource-id="[^"]*:id/thumbnail"' -and
            $ui -match 'TextBlock (jpeg|png|gif|webp) MMS fixture'
    }

    $logcat = Invoke-Adb -CommandArgs @("logcat", "-d", "-v", "brief")
    if ($logcat -match "FATAL EXCEPTION") {
        throw "A fatal exception occurred during the MMS fixture run"
    }

    [pscustomobject]@{
        Result = "PASS"
        Device = $Serial
        Package = $packageName
        Sender = $Sender
        InjectedMms = $injectedUris.Count
        MediaTypes = ($expectedMimeTypes.Values -join ", ")
        ConversationThumbnailVisible = $true
        CaptionVisible = $true
        FatalExceptions = 0
    } | Format-List
} finally {
    if ($startedEmulator -and !$KeepEmulator) {
        & $adb -s $Serial emu kill | Out-Null
    }
}
