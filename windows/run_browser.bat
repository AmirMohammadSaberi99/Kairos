@echo off
set "PATH=C:\Program Files\nodejs;%PATH%"
cd /d "%~dp0"
echo Starting Kairos TypeScript Development Server...
npm run dev -- --open
