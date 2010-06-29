SETLOCAL ENABLEDELAYEDEXPANSION
REM ##
REM # Configure these variables to match your environment
REM # If you have system-wide variables for FELIX_HOME and M2_REPO then you
REM # should not have to make any changes to this file.
REM ##

REM # Make sure the following two path entries do *not* contain spaces
REM # SET FELIX_HOME=C:\Libraries\felix-framework-2.0.0
REM # SET M2_REPO=C:\Users\johndoe\.m2\repository
REM # SET OPENCAST_LOGDIR=%FELIX_HOME%\logs

REM # To enable the debugger on the vm, enable all of the following options
SET DEBUG_PORT=8000
SET DEBUG_SUSPEND=n
REM SET DEBUG_OPTS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=%DEBUG_PORT%,server=y,suspend=%DEBUG_SUSPEND%

REM ##
REM # Only change the lines below if you know what you are doing
REM ##

SET MAVEN_ARG=-DM2_REPO=%M2_REPO%
SET FELIX_FILEINSTALL_OPTS=-Dfelix.fileinstall.dir=%FELIX_HOME%\load
SET PAX_CONFMAN_OPTS=-Dbundles.configuration.location=%FELIX_HOME%\conf
SET PAX_LOGGING_OPTS=-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN -Dopencast.logdir=%OPENCAST_LOGDIR%
SET UTIL_LOGGING_OPTS=-Djava.util.logging.config.file=%FELIX_HOME%\conf\services\java.util.logging.properties
SET FELIX_CACHE=%FELIX_HOME%\felix-cache
SET GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"

REM # Make sure matterhorn bundles are reloaded
if exist %FELIX_CACHE% (
	echo "Removing cached matterhorn bundles from %FELIX_CACHE%"
	for /f %%f in ('dir %FELIX_CACHE%\bundle.location /s /b') do (
		set linefound=0
		for /f %%i in ('findstr /b /l "file:" %%f') do (
			set linefound=1
		)
		if !linefound! equ 1 (
			set file=%%f
			rmdir /s /q !file:~0,-16!
		)
	)
)

REM # Finally start felix
java %DEBUG_OPTS% %GRAPHICS_OPTS% %MAVEN_ARG% %FELIX_FILEINSTALL_OPTS% %PAX_CONFMAN_OPTS% %PAX_LOGGING_OPTS% %UTIL_LOGGING_OPTS% -jar %FELIX_HOME%\bin\felix.jar %FELIX_CACHE%  
ENDLOCAL