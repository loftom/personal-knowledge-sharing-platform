param(
    [string]$BaseUrl = 'http://localhost:8080/api',
    [string]$MySqlContainer = 'knowledge-platform-mysql',
    [string]$MySqlDatabase = 'knowledge_platform',
    [string]$MySqlUser = 'root',
    [string]$MySqlPassword = 'root'
)

# 委托给 MySQL 版本的演示数据初始化脚本
& (Join-Path $PSScriptRoot 'setup_demo_environment_mysql.ps1') `
    -BaseUrl $BaseUrl `
    -MySqlContainer $MySqlContainer `
    -MySqlDatabase $MySqlDatabase `
    -MySqlUser $MySqlUser `
    -MySqlPassword $MySqlPassword
