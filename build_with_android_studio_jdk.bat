@echo off
setlocal

:: 设置Android Studio的JBR为JAVA_HOME
set JAVA_HOME=D:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

:: 设置Java版本为17
set GRADLE_OPTS=-Dorg.gradle.java.home="%JAVA_HOME%"

echo Using JAVA_HOME: %JAVA_HOME%
echo Java version:
java -version

echo.
echo Building project with Java 17 compatibility...
cd /d "d:\school-work\juniorYear\android\NotePad"
call gradlew.bat assembleDebug

pause