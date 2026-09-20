[CmdletBinding()]
param(
    [string]$AvdName = "PixelPhone-API35",
    [string]$Serial = "emulator-5554",
    [string]$Sender = "5551234567",
    [int]$MessageCount = 3,
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

function Get-UiXml {
    param([string]$RemotePath)
    Invoke-Adb -CommandArgs @("shell", "rm", "-f", $RemotePath) | Out-Null
    for ($attempt = 1; $attempt -le 5; $attempt++) {
        Invoke-Adb -CommandArgs @("shell", "uiautomator", "dump", $RemotePath) | Out-Null
        $exists = (& $adb -s $Serial shell "test -s $RemotePath && echo yes" 2>$null | Out-String).Trim()
        if ($exists -eq "yes") {
            return Invoke-Adb -CommandArgs @("shell", "cat", $RemotePath)
        }
        Start-Sleep -Seconds 1
    }
    throw "UI hierarchy was not written to $RemotePath"
}

if (!(Test-Path -LiteralPath $adb) -or !(Test-Path -LiteralPath $emulator)) {
    throw "Android SDK tools were not found under $sdkRoot"
}

$devicePattern = "(?m)^$([regex]::Escape($Serial))\s+device\b"
$devices = (& $adb devices | Out-String)
if ($devices -notmatch $devicePattern) {
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
            $buildOutput = & .\gradlew.bat :domain:testDebugUnitTest :presentation:assembleDebug 2>&1
            if (($buildOutput | Out-String) -notmatch "BUILD SUCCESSFUL") {
                throw "Gradle build failed`n$($buildOutput | Select-Object -Last 80 | Out-String)"
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
    Invoke-Adb -CommandArgs @("shell", "input", "keyevent", "HOME") | Out-Null
    Invoke-Adb -CommandArgs @("logcat", "-c") | Out-Null

    $marker = "tb-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
    $rankingTimes = @()
    for ($index = 1; $index -le $MessageCount; $index++) {
        $body = "$marker message $index"
        Invoke-Adb -CommandArgs @("emu", "sms", "send", $Sender, $body) | Out-Null

        Wait-Until -FailureMessage "SMS provider never received: $body" -Condition {
            (Invoke-Adb -CommandArgs @("shell", "content", "query", "--uri", "content://sms/inbox")) -match [regex]::Escape($body)
        }
        Wait-Until -FailureMessage "Notification never displayed: $body" -Condition {
            (Invoke-Adb -CommandArgs @("shell", "dumpsys", "notification", "--noredact")) -match [regex]::Escape($body)
        }

        $notificationDump = Invoke-Adb -CommandArgs @("shell", "dumpsys", "notification", "--noredact")
        $recordPattern = "(?s)NotificationRecord\([^\r\n]*pkg=$([regex]::Escape($packageName)).*?(?=\r?\n\s*NotificationRecord\(|\z)"
        $record = [regex]::Match($notificationDump, $recordPattern).Value
        if (!$record) { throw "TextBlock notification record was not found" }
        if ($record -match "ONLY_ALERT_ONCE") {
            throw "Message $index reused a notification with ONLY_ALERT_ONCE"
        }
        $rankingMatch = [regex]::Match($record, "mRankingTimeMs=(\d+)")
        if ($rankingMatch.Success) { $rankingTimes += [long]$rankingMatch.Groups[1].Value }
        Start-Sleep -Seconds 2
    }

    $providerRows = Invoke-Adb -CommandArgs @("shell", "content", "query", "--uri", "content://sms/inbox")
    $receivedCount = ([regex]::Matches($providerRows, [regex]::Escape($marker))).Count
    if ($receivedCount -ne $MessageCount) {
        throw "Expected $MessageCount provider rows, found $receivedCount"
    }

    $logcat = Invoke-Adb -CommandArgs @("logcat", "-d", "-v", "brief")
    $workerCompletions = ([regex]::Matches($logcat, "ReceiveSmsWorker.*finished")).Count
    if ($workerCompletions -lt $MessageCount) {
        throw "Expected $MessageCount ReceiveSmsWorker completions, found $workerCompletions"
    }
    if ($logcat -match "FATAL EXCEPTION") { throw "A fatal exception occurred during the SMS run" }

    Invoke-Adb -CommandArgs @("shell", "am", "force-stop", $packageName) | Out-Null
    Invoke-Adb -CommandArgs @("shell", "monkey", "-p", $packageName, "-c", "android.intent.category.LAUNCHER", "1") | Out-Null
    $inboxXml = ""
    for ($attempt = 1; $attempt -le 12; $attempt++) {
        Start-Sleep -Seconds 1
        $inboxXml = Get-UiXml -RemotePath "/sdcard/textblock-inbox.xml"
        if ($inboxXml -match 'resource-id="[^\"]*:id/toolbarSearch"' -and
            $inboxXml -match 'resource-id="[^\"]*:id/title"') { break }
    }
    if ($inboxXml -notmatch 'resource-id="[^\"]*:id/toolbarSearch"' -or
        $inboxXml -notmatch 'resource-id="[^\"]*:id/title"') {
        throw "Harness sender conversation was not visible in the inbox"
    }
    $toolbarBoundsPattern = 'resource-id="[^\"]*:id/toolbar"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $toolbarBounds = [regex]::Match($inboxXml, $toolbarBoundsPattern)
    $windowState = Invoke-Adb -CommandArgs @("shell", "dumpsys", "window")
    $statusBarFrame = [regex]::Match(
        $windowState,
        'type=statusBars frame=\[0,0\]\[\d+,(\d+)\] visible=true'
    )
    if (!$toolbarBounds.Success -or !$statusBarFrame.Success) {
        throw "Could not measure the inbox toolbar and visible status bar"
    }
    $toolbarTop = [int]$toolbarBounds.Groups[2].Value
    $statusBarInset = [int]$statusBarFrame.Groups[1].Value
    if ($toolbarTop -lt $statusBarInset) {
        throw "Inbox toolbar overlaps the status bar (toolbar=$toolbarTop, inset=$statusBarInset)"
    }

    Invoke-Adb -CommandArgs @("shell", "input", "tap", "50", "$($toolbarTop + 50)") | Out-Null
    $drawerXml = ""
    $drawerRowPattern = 'resource-id="[^\"]*:id/inbox"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    for ($attempt = 1; $attempt -le 5; $attempt++) {
        Start-Sleep -Seconds 1
        $drawerXml = Get-UiXml -RemotePath "/sdcard/textblock-drawer.xml"
        if ($drawerXml -match $drawerRowPattern) { break }
    }
    $drawerRowBounds = [regex]::Match($drawerXml, $drawerRowPattern)
    if (!$drawerRowBounds.Success) { throw "Navigation drawer did not open" }
    $drawerFirstRowTop = [int]$drawerRowBounds.Groups[2].Value
    if ($drawerFirstRowTop -lt $statusBarInset) {
        throw "Navigation drawer overlaps the status bar (row=$drawerFirstRowTop, inset=$statusBarInset)"
    }
    Invoke-Adb -CommandArgs @("shell", "input", "keyevent", "BACK") | Out-Null
    Start-Sleep -Seconds 1

    Invoke-Adb -CommandArgs @("shell", "input", "tap", "500", "250") | Out-Null
    $messageBoundsPattern = 'resource-id="[^\"]*:id/message"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $conversationXml = ""
    for ($attempt = 1; $attempt -le 8; $attempt++) {
        Start-Sleep -Seconds 1
        $conversationXml = Get-UiXml -RemotePath "/sdcard/textblock-conversation.xml"
        if ($conversationXml -match $messageBoundsPattern) { break }
    }
    $beforeBounds = [regex]::Match($conversationXml, $messageBoundsPattern)
    if (!$beforeBounds.Success) { throw "Message composer was not visible before opening the keyboard" }
    $beforeBottom = [int]$beforeBounds.Groups[4].Value

    Invoke-Adb -CommandArgs @("shell", "settings", "put", "secure", "show_ime_with_hard_keyboard", "1") | Out-Null
    $tapX = [math]::Floor(([int]$beforeBounds.Groups[1].Value + [int]$beforeBounds.Groups[3].Value) / 2)
    $tapY = [math]::Floor(([int]$beforeBounds.Groups[2].Value + $beforeBottom) / 2)
    Invoke-Adb -CommandArgs @("shell", "input", "tap", "$tapX", "$tapY") | Out-Null
    Start-Sleep -Seconds 2
    $draft = "keyboard_$marker"
    Invoke-Adb -CommandArgs @("shell", "input", "text", $draft) | Out-Null
    Start-Sleep -Seconds 1
    $keyboardXml = Get-UiXml -RemotePath "/sdcard/textblock-keyboard.xml"
    $afterBounds = [regex]::Match($keyboardXml, $messageBoundsPattern)
    if (!$afterBounds.Success -or $keyboardXml -notmatch [regex]::Escape($draft)) {
        throw "Message composer or typed draft was hidden after opening the keyboard"
    }
    $afterBottom = [int]$afterBounds.Groups[4].Value
    $keyboardLiftPixels = $beforeBottom - $afterBottom
    $inputMethodState = Invoke-Adb -CommandArgs @("shell", "dumpsys", "input_method")
    if ($inputMethodState -notmatch "mInputShown=true" -or $keyboardLiftPixels -lt 300) {
        throw "Composer did not move far enough above the software keyboard (lift=$keyboardLiftPixels px)"
    }

    [pscustomobject]@{
        Result = "PASS"
        Device = $Serial
        Package = $packageName
        InjectedSms = $MessageCount
        ProviderRows = $receivedCount
        WorkerCompletions = $workerCompletions
        NotificationRankingTimes = ($rankingTimes -join ", ")
        OnlyAlertOnce = $false
        KeyboardComposerLiftPixels = $keyboardLiftPixels
        TypedDraftVisible = $true
        StatusBarInsetPixels = $statusBarInset
        InboxToolbarTopPixels = $toolbarTop
        DrawerFirstRowTopPixels = $drawerFirstRowTop
    } | Format-List
} finally {
    if ($startedEmulator -and !$KeepEmulator) {
        & $adb -s $Serial emu kill | Out-Null
    }
}
