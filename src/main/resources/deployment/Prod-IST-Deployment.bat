@echo off
echo ======================= Application Deployment =======================
SET APP_PROFILE=prod_ist
echo =================== Starting MongoDB Server ==========================
start cmd /k call "C:\Program Files\MongoDB\Server\6.0\bin\mongod.exe"
echo Final working directory set to : %CURRENT_DIRECTORY%
REM =============================== Set Directory =========================
SET BASE_PATH=C:\Deployment
SET CURRENT_DIRECTORY=%BASE_PATH%\%APP_PROFILE%

echo Final working directory set to : %CURRENT_DIRECTORY%

REM ================ Pull Build and Deploy Option =========================

SET PROJECT_ROOT_DIR=%CURRENT_DIRECTORY%
SET "PROJECT_DIRECTORY=%PROJECT_ROOT_DIR%\algotrade"

echo PWD : %PROJECT_DIRECTORY%

REM ================ Delete Old Directory And Create Fresh Directory =======
echo Deleting directory : %PROJECT_ROOT_DIR%
rmdir /s /q %PROJECT_ROOT_DIR%
echo 
echo 
echo Creating fresh directory : %PROJECT_ROOT_DIR%
mkdir %PROJECT_ROOT_DIR%
echo Fresh Directory Created : %PROJECT_ROOT_DIR%

cd %PROJECT_ROOT_DIR%
git clone git@github.com:thakaresureshss/algotrade.git
cd %PROJECT_DIRECTORY%
git checkout master
echo Building Maven Project from Directory:  %PROJECT_DIRECTORY%	
call mvn clean install -DskipTests=true

REM ================ Starting application =======

cd %PROJECT_DIRECTORY%\target
echo JAR Location :%PWD%

echo ======== Starting Application With Profile %APP_PROFILE% =========
java -jar algotrade.jar --spring.profiles.active=%APP_PROFILE%
pause