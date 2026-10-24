@echo off
title Servidor Web Ximasak
cd /d "%~dp0"
echo ========================================================
echo Abriendo la pagina de Ximasak en tu navegador...
echo ========================================================
start http://localhost:4200
node server.js
pause
