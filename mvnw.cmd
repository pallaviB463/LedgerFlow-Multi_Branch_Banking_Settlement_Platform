@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script for Windows
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"

@REM Determine the Java command
set JAVA_EXE=java
if not "%JAVA_HOME%"=="" set JAVA_EXE=%JAVA_HOME%\bin\java

@REM Run Maven via the wrapper
"%JAVA_EXE%" ^
  %MVNW_JAVA_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%~dp0." ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=%ERRORLEVEL%
exit /B %ERROR_CODE%

:end
