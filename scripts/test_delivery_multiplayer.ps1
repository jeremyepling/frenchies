#Requires -Version 7.0
# Two offline development clients connect only to a disposable loopback server.
param(
    [switch]$Offline,
    [ValidateSet('delivery','play','whistle','legacy','explorer')][string]$Scenario = 'delivery',
    [ValidateSet('26.1', '26.3', '26.4')][string]$Target = '26.1'
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$checkout = Split-Path $PSScriptRoot -Parent
$targetDir = Join-Path $checkout "versions/${Target}-neoforge"
$project = ":${Target}-neoforge"
$buildModdev = Join-Path $targetDir 'build/moddev'
$runId = [guid]::NewGuid().ToString('N')
$root = Join-Path $targetDir ".tmp/$Scenario-multiplayer/$runId"
$roles = 'Server', 'Owner', 'Visitor'
foreach ($role in $roles) {
    [IO.Directory]::CreateDirectory((Join-Path $root $role)) | Out-Null
}
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
@(
    'server-ip=127.0.0.1', "server-port=$port", 'online-mode=false', 'enforce-secure-profile=false',
    'level-name=world', 'level-type=minecraft:flat', 'level-seed=1234', 'generate-structures=false',
    'gamemode=survival', 'spawn-protection=0', 'view-distance=3', 'simulation-distance=3', 'max-players=2'
) | Set-Content -LiteralPath (Join-Path $root 'server/server.properties')

# Offline-mode UUIDs for the two fixed development usernames.
@(
    @{ name = 'FFOwner'; uuid = '3c356f1a-04a8-321d-a108-f35619b350df' }
    @{ name = 'FFVisitor'; uuid = '03aa91f9-0976-37de-bafa-3852f271766a' }
) | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'server/whitelist.json')

$gameOptions = (Get-Content -LiteralPath (Join-Path $checkout 'dev/options-template.txt')) `
    -replace '^pauseOnLostFocus:.*', 'pauseOnLostFocus:false' -replace '^renderDistance:.*', 'renderDistance:3' `
    -replace '^maxFps:.*', 'maxFps:30' -replace '^enableVsync:.*', 'enableVsync:false'
foreach ($role in 'Owner', 'Visitor') {
    $gameOptions | Set-Content -LiteralPath (Join-Path $root "$role/options.txt")
}

Push-Location -LiteralPath $checkout
try {
    Write-Host "Minecraft target: $Target"
    Write-Host "Multiplayer test world and logs: $root"
    $options = @('--console=plain')
    if ($Offline) { $options += '--offline' }
    & .\gradlew.bat @options "-PdeliveryTestId=$runId" "-PdeliveryTestPort=$port" "-PmultiplayerScenario=$Scenario" "${project}:playtestClasses" `
        "${project}:createDeliveryServerLaunchScript" "${project}:createDeliveryOwnerLaunchScript" "${project}:createDeliveryVisitorLaunchScript" *> (Join-Path $root 'prepare.log')
    if ($LASTEXITCODE -ne 0) { throw "Could not prepare the multiplayer launchers. See $root/prepare.log" }

    # Snapshot the generated launchers and argfiles: later Gradle runs reuse build/moddev.
    $launchDir = Join-Path $root 'launch'
    [IO.Directory]::CreateDirectory($launchDir) | Out-Null
    foreach ($role in $roles) {
        foreach ($suffix in 'RunClasspath.txt', 'RunVmArgs.txt', 'RunProgramArgs.txt', 'Log4j2.xml') {
            $name = "delivery$role$suffix"
            Copy-Item -LiteralPath (Join-Path $buildModdev $name) -Destination (Join-Path $launchDir $name)
        }
        $scriptName = "runDelivery$role.cmd"
        $script = Get-Content -Raw -LiteralPath (Join-Path $buildModdev $scriptName)
        $script.Replace($buildModdev, $launchDir) | Set-Content -LiteralPath (Join-Path $launchDir $scriptName)
    }

    $phases = if ($Scenario -in 'play', 'whistle', 'legacy') { @(1, 2) } else { @(1) }
    foreach ($phase in $phases) {
        if ($Scenario -eq 'legacy' -and $phase -eq 2) {
            # This disposable fixture owns exactly one live, sitting dog. Preserve the first run's
            # locator outside the world to reproduce upgrading a world that predates whistle saves.
            $roster = Join-Path $root 'Server/world/data/florafauna/companions.dat'
            Move-Item -LiteralPath $roster -Destination (Join-Path $root 'pre-upgrade-companions.dat')
        }
        Write-Host "Launching dedicated $Scenario phase $phase"
        Set-Content -LiteralPath (Join-Path $root 'phase') -Value $phase
        $processes = @()
        $deadline = [DateTime]::UtcNow.AddMinutes(6)
        try {
            foreach ($role in $roles) {
                $processes += Start-Process -FilePath $env:ComSpec -WorkingDirectory $launchDir `
                    -ArgumentList '/d', '/c', "runDelivery$role.cmd" -WindowStyle Hidden -PassThru `
                    -RedirectStandardOutput (Join-Path $root "$role-$phase.log") -RedirectStandardError (Join-Path $root "$role-$phase.err")
                if ($role -eq 'Server') {
                    while (!(Test-Path -LiteralPath (Join-Path $root "ready-$phase"))) {
                        if ($processes[0].HasExited -or [DateTime]::UtcNow -ge $deadline) {
                            throw "Server failed to start. See $root/Server-$phase.log"
                        }
                        Start-Sleep -Milliseconds 500
                    }
                }
            }
            while (@($processes | Where-Object { !$_.HasExited }).Count) {
                if (@($processes | Where-Object HasExited).Count -and !(Test-Path -LiteralPath (Join-Path $root "phase-$phase.result"))) {
                    throw "A Minecraft process exited before completing phase $phase. See $root"
                }
                if ([DateTime]::UtcNow -ge $deadline) { throw "Multiplayer phase $phase timed out. See $root" }
                Start-Sleep -Milliseconds 500
            }
            $result = Get-Content -Raw -LiteralPath (Join-Path $root "phase-$phase.result")
            if ($result -ne 'PASS') { throw "Multiplayer phase $phase failed: $result" }
            Write-Host "Multiplayer phase $phase passed."
        } finally {
            foreach ($process in $processes) {
                if (!$process.HasExited) { $process.Kill($true) }
                $process.Dispose()
            }
        }
    }
} finally { Pop-Location }
