@echo off
cd /d "%~dp0"
echo.
echo JipGuHae Design System local server starting...
echo Open this URL in your browser:
echo http://localhost:8000/ui_kits/web/index.html
echo.
start "" "http://localhost:8000/ui_kits/web/index.html"
python -m http.server 8000
if errorlevel 1 (
  py -m http.server 8000
)
