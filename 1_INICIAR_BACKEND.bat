@echo off
title Backend Spring Boot - SIRVS Ximasak
echo ========================================================
echo Iniciando Backend Spring Boot en http://localhost:8080
echo ========================================================
cd /d "%~dp0ximasak_full\backend"
call .\mvnw.cmd spring-boot:run
pause
