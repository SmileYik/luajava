param(
    [string]$Path
)

Write-Host "Working Directory: $PATH"

# Find and load the latest installed Visual Studio's developer environment
function Enter-VsDevShell {
    # Visual Studio version.
    $vsYears = @("2017", "2019", "2022")

    # current system arch
    $nativeArch = if ([Environment]::Is64BitOperatingSystem) { "x64" } else { "x86" }

    # arch priority
    $archPriorityList = if ($nativeArch -eq "x64") { @("x64", "x86", "arm64") } else { @("x86", "x64", "arm64") }

    # Visual Studio installed path.
    $possibleRoots = @(
        "$env:ProgramFiles\Microsoft Visual Studio",
        "${env:ProgramFiles(x86)}\Microsoft Visual Studio"
    )
    # other Visual Studio installed path.
    $additionalRoots = @()
    $driveLetters = "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N"
    foreach ($letter in $driveLetters) {
        if (Test-Path "${letter}:\\") {
            $additionalRoots += "${letter}:\Program Files\Microsoft Visual Studio"
            $additionalRoots += "${letter}:\Program Files (x86)\Microsoft Visual Studio"
        }
    }
    $vsInstallRoots = $possibleRoots + $additionalRoots

    $latestVsDevCmd = $null
    $foundArch = $null

    foreach ($arch in $archPriorityList) {
        foreach ($year in $vsYears) {
            foreach ($root in $vsInstallRoots) {
                $basePath = Join-Path $root $year
                if (-not (Test-Path $basePath)) {
                    continue
                }

                $editions = Get-ChildItem -Path $basePath -Directory
                foreach ($edition in $editions) {
                    $vsDevCmdPath = Join-Path $edition.FullName "Common7\Tools\VsDevCmd.bat"
                    if (Test-Path $vsDevCmdPath) {
                        Write-Host "Visual Studio $year：$vsDevCmdPath (Arch：$arch)"
                        $latestVsDevCmd = $vsDevCmdPath
                        $foundArch = $arch
                        break
                    }
                }
                if ($latestVsDevCmd) { break }
            }
            if ($latestVsDevCmd) { break }
        }
        if ($latestVsDevCmd) { break }
    }

    if (-not $latestVsDevCmd) {
        Write-Host "Not found VsDevCmd.bat，please ensure installed Visual Studio." -ForegroundColor RED
        Write-Host ""
        Write-Host "  ================== ERROR ===================" -ForegroundColor RED
        Write-Host "  ===        NO!!! IT IS NOT TRUE!!        ===" -ForegroundColor RED
        Write-Host "  ===  I MISSING MY SPECIAL VISUAL STUDIO  ===" -ForegroundColor RED
        Write-Host "  ===     WHERE IS MY COMPILE SWORD?!      ===" -ForegroundColor RED
        Write-Host "  ===    WITHOUT YOU, CODE SHALL NOT LINK  ===" -ForegroundColor RED
        Write-Host "  ===   THE SHADOW OF LINKER ERROR RISES   ===" -ForegroundColor RED
        Write-Host "  ===       RUN AWAY FROM THIS HELL        ===" -ForegroundColor RED
        Write-Host "  ============================================" -ForegroundColor RED
        Write-Host ""
        Write-Host ""
        Start-Sleep -Seconds 3
        exit
    }

    Write-Host "Will use: $latestVsDevCmd"

    # 使用 cmd /c 来运行 VsDevCmd.bat，并导出环境变量
    $cmdScript = "`"$latestVsDevCmd`" -arch=$foundArch && set"
    $output = cmd /c $cmdScript

    # 将输出解析为环境变量
    foreach ($line in $output) {
        if ($line -match '^([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
    Write-Host ""
    Write-Host " ===================== WARING =======================" -ForegroundColor YELLOW
    Write-Host " ===     NOBODY CAN STOP ME COMPILING LUAJIT      ===" -ForegroundColor YELLOW
    Write-Host " ===           ESPECIALLY YOU WINDOWS!            ===" -ForegroundColor YELLOW
    Write-Host " ===      DETECTED THE HOLY VS ENVIRONMENT        ===" -ForegroundColor Green
    Write-Host " ===         VISUAL STUDIO HAS AWAKENED           ===" -ForegroundColor Green
    Write-Host " ===  VISUAL STUDIO ENVIRONMENT ($foundArch) ACTIVATED!  ===" -ForegroundColor YELLOW
    Write-Host " ====================================================" -ForegroundColor YELLOW
    Write-Host ""
}

Enter-VsDevShell
Start-Sleep -Seconds 3

cd $Path
cmd
cl /MD /O2 /c /DLUA_BUILD_AS_DLL *.c
ren lua.obj lua.o
ren luac.obj luac.o
link /DLL /IMPLIB:lua.lib /OUT:lua.dll *.obj
link /OUT:lua.exe lua.o lua.lib
link /OUT:luac.exe luac.o *.obj