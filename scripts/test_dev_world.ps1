#Requires -Version 7.0
# Run Minecraft clients against fresh copies of a closed save. Each scenario has its own copy.
[CmdletBinding(DefaultParameterSetName = 'Selected')]
param(
    [ValidateSet('26.1', '26.3', '26.4')]
    [string]$Target = '26.1',
    [Parameter(ParameterSetName = 'Selected', Position = 0)]
    [ValidateSet('frenchie', 'frontpack', 'companions', 'delivery', 'weather', 'rest')]
    [string[]]$Scenario,
    [Parameter(ParameterSetName = 'Full')]
    [switch]$All,
    [string]$SourceWorld,
    [Alias('CuriosJar')][string[]]$AccessoryJar,
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
if ($All) {
    $Scenario = @('frenchie', 'frontpack', 'companions', 'delivery', 'weather', 'rest')
} elseif (!$Scenario) {
    throw 'Choose a client scenario, e.g. -Scenario delivery, or use -All for the full gameplay suite.'
}
$checkout = Split-Path $PSScriptRoot -Parent
$targetDir = Join-Path $checkout "versions/${Target}-neoforge"
$project = ":${Target}-neoforge"
if (!$SourceWorld) {
    $SourceWorld = Join-Path $targetDir "run/client-$(Split-Path $checkout -Leaf)/saves/dev"
}
$world = (Resolve-Path -LiteralPath $SourceWorld).Path
$accessories = @($AccessoryJar | Where-Object { $_ } | ForEach-Object { (Resolve-Path -LiteralPath $_).Path })
$level = Join-Path $world 'level.dat'
if (!(Test-Path -LiteralPath $level -PathType Leaf)) { throw "Missing level.dat in $world" }
if (@($Scenario | Sort-Object -Unique).Count -ne $Scenario.Count) { throw 'Select each scenario only once.' }
$runId = [guid]::NewGuid().ToString('N')
$runRoot = Join-Path $targetDir ".tmp/world-playtests/$runId"
if ($runRoot.StartsWith($world + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The test output cannot be inside the source save.'
}
if ((Get-Item -LiteralPath $world).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or
        (Get-ChildItem -LiteralPath $world -Recurse -Force -Attributes ReparsePoint | Select-Object -First 1)) {
    throw 'Choose a self-contained save without symbolic links or junctions.'
}
$options = @('--console=plain')
if ($Offline) { $options += '--offline' }
$hash = (Get-FileHash -LiteralPath $level).Hash
Write-Host "Minecraft target: $Target"
Write-Host "Source save: $world"
Write-Host "Test worlds, logs and results: $runRoot"

Push-Location -LiteralPath $checkout
try {
    $lock = $null
    try {
        $lockPath = Join-Path $world 'session.lock'
        if (Test-Path -LiteralPath $lockPath) {
            try { $lock = [IO.File]::Open($lockPath, 'Open', 'Read', 'Read') }
            catch [IO.IOException] { throw "Close the source world before copying it: $world" }
        }
        foreach ($name in $Scenario) {
            $gameDir = Join-Path $runRoot $name
            $copy = Join-Path $gameDir 'saves/playtest'
            [IO.Directory]::CreateDirectory($copy) | Out-Null
            if ($accessories.Count) {
                $mods = [IO.Directory]::CreateDirectory((Join-Path $gameDir 'mods'))
                Copy-Item -LiteralPath $accessories -Destination $mods.FullName
            }
            & robocopy $world $copy /E /XJ /XF session.lock /R:0 /W:0 /NFL /NDL /NJH /NJS /NP | Out-Null
            if ($LASTEXITCODE -ge 8) { throw "Could not copy $world into $copy (robocopy exit $LASTEXITCODE)." }
            if ((Get-FileHash -LiteralPath (Join-Path $copy 'level.dat')).Hash -ne $hash) {
                throw 'Source save changed during the copy.'
            }
            Set-Content -LiteralPath (Join-Path $gameDir '.florafauna-playtest') -Value $name
            $gameOptions = (Get-Content -LiteralPath 'dev/options-template.txt') -replace '^pauseOnLostFocus:.*','pauseOnLostFocus:false'
            $gameOptions | Set-Content -LiteralPath (Join-Path $gameDir 'options.txt')
        }
    } finally { if ($lock) { $lock.Dispose() } }

    $failed = @()
    foreach ($name in $Scenario) {
        $gameDir = Join-Path $runRoot $name
        $phases = if ($name -in 'frenchie', 'companions', 'weather', 'rest') { @(1) } else { @(1, 2) }
        foreach ($phase in $phases) {
            Write-Host "Launching $name phase $phase ..."
            $log = Join-Path $gameDir "phase-$phase.log"
            & .\gradlew.bat @options "-PplaytestId=$runId" "-PplaytestScenario=$name" "-PplaytestPhase=$phase" "${project}:runWorldPlaytest" *> $log
            $exitCode = $LASTEXITCODE
            $resultPath = Join-Path $gameDir "phase-$phase.result"
            $result = if (Test-Path -LiteralPath $resultPath) { (Get-Content -Raw -LiteralPath $resultPath).Trim() } else { 'Missing client result' }
            Write-Host "$name phase $phase (Gradle $exitCode): $result  $log"
            if ($exitCode -ne 0 -or $result -ne 'PASS') {
                $failed += "$name phase $phase"
                break
            }
        }
    }
    if ($failed.Count) { throw "Failed playtests: $($failed -join ', '). Logs: $runRoot" }
} finally { Pop-Location }
