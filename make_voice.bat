@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
set "ZIPAI_TTS_PY=python"
if exist ".venv\Scripts\python.exe" set "ZIPAI_TTS_PY=.venv\Scripts\python.exe"
"%ZIPAI_TTS_PY%" tools\generate_presentation_audio.py
set "ZIPAI_TTS_EXIT=%ERRORLEVEL%"
pause
exit /b %ZIPAI_TTS_EXIT%
