@echo off
title Subiendo proyecto a GitHub...
cd /d "%~dp0"
echo ========================================================
echo Subiendo Ximasak a https://github.com/alevascan/ximasak.git
echo ========================================================
git push -u origin main
echo ========================================================
echo Si todo salio bien, tu proyecto ya esta en GitHub.
echo ========================================================
pause
