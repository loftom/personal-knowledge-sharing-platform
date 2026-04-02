@echo off
set "ROOT=%~dp0"
curl.exe -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"reader_xu\",\"password\":\"User@123456\"}" http://localhost:8080/api/auth/login -o "%ROOT%login.json"
type "%ROOT%login.json"


