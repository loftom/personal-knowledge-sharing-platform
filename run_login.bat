@echo off
curl.exe -s -X POST -H "Content-Type: application/json" -d "{\"username\":\"reader_xu\",\"password\":\"User@123456\"}" http://localhost:8080/api/auth/login -o d:\code\java\personal-knowledge-sharing-platform\login.json
type d:\code\java\personal-knowledge-sharing-platform\login.json


