@echo off
chcp 65001 >nul
title 极致串流 - JizhiStream

echo ========================================
echo     极致串流 JizhiStream Desktop
echo ========================================
echo.

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到Java，请安装JDK 17+
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

if not exist "desktop\build\libs" (
    echo [信息] 首次运行，正在构建项目...
    call gradlew.bat :desktop:run
) else (
    call gradlew.bat :desktop:run
)

if %errorlevel% neq 0 (
    echo.
    echo [错误] 启动失败，请检查Java版本是否为17+
    pause
)
