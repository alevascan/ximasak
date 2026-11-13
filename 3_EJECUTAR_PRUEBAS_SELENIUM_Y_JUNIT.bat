@echo off
chcp 65001 > nul
echo ============================================================================
echo   EJECUTANDO PRUEBAS UNITARIAS (JUNIT 5) Y FUNCIONALES E2E (SELENIUM)
echo   PROYECTO SIRVS - XIMASAK EIRL
echo ============================================================================

REM 1. Detectar Java automáticamente si no está en el PATH o JAVA_HOME
where java >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Users\ale\.gemini\antigravity\scratch\tools\jdk-21.0.6+7\bin\java.exe" (
        set "JAVA_HOME=C:\Users\ale\.gemini\antigravity\scratch\tools\jdk-21.0.6+7"
    ) else (
        for /d %%i in ("C:\Program Files\Java\jdk*" "C:\Program Files\Eclipse Adoptium\jdk*" "%USERPROFILE%\.jdks\*") do (
            if exist "%%~fi\bin\java.exe" set "JAVA_HOME=%%~fi"
        )
    )
    if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
)

cd /d "%~dp0ximasak_full\backend"
call .\mvnw.cmd test -Dtest="AuthServiceTest,VentaServiceTest,ClienteServiceTest,PasswordResetServiceTest,ProductoServiceTest,ComprobanteServiceTest,JwtServiceTest,SirvsSeleniumTest"

echo.
echo ============================================================================
echo   EJECUCION DE PRUEBAS FINALIZADA
echo   Las capturas generadas estan en: ximasak_full\backend\evidencias_selenium
echo ============================================================================
pause
