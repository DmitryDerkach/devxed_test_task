# One command to verify the whole repository (Windows PowerShell 5.1+ / PowerShell 7):
#   1. Task 1 - JUnit tests of the packaging selector
#   2. Task 2 - REST Assured tests of the promo-code API
#   3. Task 2 - starts the reference service and runs the Postman collection with Newman
# Requirements: JDK 17+ (Maven is downloaded by the wrapper), Node.js 18+ for step 3.
# Double-click run-all.cmd, or:  powershell -ExecutionPolicy Bypass -File .\run-all.ps1
param([int]$Port = $(if ($env:PORT) { [int]$env:PORT } else { 8080 }))

$ErrorActionPreference = 'Continue'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
$Root = $PSScriptRoot
$T1 = Join-Path $Root 'Task 1\Additional\Automation\packaging-selector'
$T2 = Join-Path $Root 'Task 2\Additional\Automation\promo-code-api'
$Logs = Join-Path $Root '.run-logs'
$Newman = 'newman@6.2.1'
New-Item -ItemType Directory -Force $Logs | Out-Null

$CHECK = [char]0x2714; $CROSS = [char]0x2718; $ARROW = [char]0x25B6
function Ok($m)   { Write-Host "  $CHECK " -ForegroundColor Green -NoNewline; Write-Host $m }
function Bad($m)  { Write-Host "  $CROSS " -ForegroundColor Red -NoNewline; Write-Host $m }
function Warn($m) { Write-Host "  ! " -ForegroundColor Yellow -NoNewline; Write-Host $m }
function Info($m) { Write-Host "    $m" -ForegroundColor DarkGray }
function Step($m) { Write-Host ""; Write-Host "$ARROW $m" -ForegroundColor Cyan }

Write-Host ""
Write-Host "  +----------------------------------------------+" -ForegroundColor White
Write-Host "  |  devxed_test_task - run everything           |" -ForegroundColor White
Write-Host "  +----------------------------------------------+" -ForegroundColor White

# ------------------------------------------------------------------ prerequisites
Step 'Checking prerequisites'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Bad 'Java not found. Install JDK 17+ (e.g. https://adoptium.net) and run again.'; exit 1
}
$jv = (cmd /c 'java -version 2>&1' | Select-Object -First 1)
$major = if ($jv -match 'version "(\d+)') { [int]$Matches[1] } else { 0 }
if ($major -lt 17) { Bad "JDK $major found, 17+ is required."; exit 1 }
Ok "JDK $major"
$haveNode = [bool](Get-Command npx -ErrorAction SilentlyContinue)
if ($haveNode) { Ok "Node.js $(node -v)" } else { Warn 'Node.js not found - the Postman/Newman step will be skipped (install from https://nodejs.org)' }

$results = New-Object System.Collections.ArrayList
$failed = $false

function Count-Surefire($dir) {
    $t = 0; $f = 0
    Get-ChildItem (Join-Path $dir 'target\surefire-reports\TEST-*.xml') -ErrorAction SilentlyContinue | ForEach-Object {
        $s = ([xml](Get-Content $_.FullName -Raw)).testsuite
        $t += [int]$s.tests; $f += [int]$s.failures + [int]$s.errors
    }
    return @($t, $f)
}

function Run-Maven($name, $dir, $log) {
    $logPath = Join-Path $Logs $log
    Info "mvnw test  (log: .run-logs\$log)"
    $sw = [Diagnostics.Stopwatch]::StartNew()
    cmd /c "cd /d `"$dir`" && .\mvnw.cmd -B -ntp test > `"$logPath`" 2>&1"
    $rc = $LASTEXITCODE
    $t, $f = Count-Surefire $dir
    if ($rc -eq 0) {
        Ok "${name}: $t tests, 0 failed  ($([int]$sw.Elapsed.TotalSeconds) s)"
        [void]$results.Add(@('OK', $name, "$t tests, 0 failed"))
    } else {
        Bad "${name}: $t tests, $f failed - last lines of the log:"
        Get-Content $logPath -Tail 30 | ForEach-Object { Write-Host "      $_" }
        [void]$results.Add(@('FAIL', $name, "$t tests, $f failed")); $script:failed = $true
    }
}

# ------------------------------------------------------------------ Task 1
Step 'Task 1 - Packaging selector (JUnit 5 + AssertJ + mutation check)'
Run-Maven 'Task 1 - Java tests' $T1 'task1-maven.log'

# ------------------------------------------------------------------ Task 2
Step 'Task 2 - Promo-code API (REST Assured + mutation check)'
Run-Maven 'Task 2 - Java tests' $T2 'task2-maven.log'

Step 'Task 2 - Postman collection (Newman against the reference service)'
if (-not $haveNode) {
    Warn 'skipped (no Node.js)'; [void]$results.Add(@('SKIP', 'Task 2 - Postman / Newman', 'skipped - Node.js not installed'))
} else {
    $busy = $false
    try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', $Port); $c.Close(); $busy = $true } catch {}
    if ($busy) { Bad "port $Port is already in use - free it or run: .
un-all.ps1 -Port 8090"; exit 1 }
    Info "starting the service on http://localhost:$Port  (log: .run-logs\service.log)"
    $env:PORT = "$Port"
    $svcLog = Join-Path $Logs 'service.log'
    $svc = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', ".\mvnw.cmd -B -ntp -q compile exec:java > `"$svcLog`" 2>&1" `
        -WorkingDirectory $T2 -WindowStyle Hidden -PassThru
    $up = $false
    for ($i = 0; $i -lt 90; $i++) {
        try { $c = New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1', $Port); $c.Close(); $up = $true; break } catch {}
        if ($svc.HasExited) { break }
        Start-Sleep -Seconds 1
    }
    if (-not $up) {
        Bad 'the service did not start - see .run-logs\service.log'
        Get-Content $svcLog -Tail 20 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "      $_" }
        [void]$results.Add(@('FAIL', 'Task 2 - Postman / Newman', 'service did not start')); $failed = $true
    } else {
        Ok 'service is up'
        $nlog = Join-Path $Logs 'newman.log'; $njson = Join-Path $Logs 'newman.json'
        Info "npx $Newman run ...  (log: .run-logs\newman.log)"
        $pmDir = Join-Path $Root 'Task 2\Additional'
        cmd /c "cd /d `"$pmDir`" && npx --yes $Newman run Postman/Promo_Code_API.postman_collection.json -e Postman/Local.postman_environment.json --env-var baseUrl=http://localhost:$Port --reporters cli,json --reporter-json-export `"$njson`" --color off > `"$nlog`" 2>&1"
        $rc = $LASTEXITCODE
        $rq = '?'; $as = '?'; $af = '?'
        try { $st = (Get-Content $njson -Raw -Encoding UTF8 | ConvertFrom-Json).run.stats; $rq = $st.requests.total; $as = $st.assertions.total; $af = $st.assertions.failed } catch {}
        if ($rc -eq 0) {
            Ok "Newman: $rq requests, $as assertions, 0 failed"
            [void]$results.Add(@('OK', 'Task 2 - Postman / Newman', "$rq requests, $as assertions, 0 failed"))
        } else {
            Bad "Newman: $af of $as assertions failed - see .run-logs\newman.log"
            Get-Content $nlog -Tail 30 | ForEach-Object { Write-Host "      $_" }
            [void]$results.Add(@('FAIL', 'Task 2 - Postman / Newman', "$af of $as assertions failed")); $failed = $true
        }
    }
    # stop the service: kill the whole process tree (cmd -> mvnw -> java)
    cmd /c "taskkill /PID $($svc.Id) /T /F >nul 2>&1"
}

# ------------------------------------------------------------------ summary
Write-Host ""
Write-Host '  Summary' -ForegroundColor White
Write-Host '  ----------------------------------------------------------'
foreach ($r in $results) {
    switch ($r[0]) {
        'OK'   { Write-Host "  $CHECK  " -ForegroundColor Green -NoNewline }
        'FAIL' { Write-Host "  $CROSS  " -ForegroundColor Red -NoNewline }
        default { Write-Host '  !  ' -ForegroundColor Yellow -NoNewline }
    }
    Write-Host ('{0,-28} {1}' -f $r[1], $r[2])
}
Write-Host '  ----------------------------------------------------------'
if (-not $failed) { Write-Host '  All green. ' -ForegroundColor Green -NoNewline; Write-Host 'Logs and reports: .run-logs\, *\target\surefire-reports\' }
else { Write-Host '  Something failed - see the logs in .run-logs\' -ForegroundColor Red }
Write-Host ''
if ($env:GITHUB_STEP_SUMMARY) {
    $md = @('### devxed_test_task - test run (Windows)', '', '| | Suite | Result |', '|:-:|---|---|')
    foreach ($r in $results) { $e = @{ OK = ':white_check_mark:'; FAIL = ':x:'; SKIP = ':warning:' }[$r[0]]; $md += "| $e | $($r[1]) | $($r[2]) |" }
    Add-Content -Path $env:GITHUB_STEP_SUMMARY -Value $md -Encoding UTF8
}
if ($failed) { exit 1 } else { exit 0 }
