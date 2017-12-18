CTF Screensaver on Windows
====================
Usually all you have to do is copy a build of `ctfscreen.jar` and all its libs into a sub-directory named `ctfscreen-data` and run `ctfscreen.scr /s`. 

`ctfscreen.scr` is a light-weight native runner for `ctfscreen.jar`. It is based on WinRun4J. Changes to the configuration of the runner are made by changing the values in `ctfscreen_config.ini` and then running `updateini.bat`, which uses `RCEDIT64.exe` to splice the contents of the config file into the `.scr` binary. `ctfscreen.jar` cannot be installed as a screensaver in Windows without the `.scr` runner. All libraries are located in `ctfscreen-data`, which is specified along with the class containing the main method in `ctfscreen_config.ini`. 

###Wix
`wix.bat` runs Wix to build an MSI, it expects the version number to be set in an environment variable by Gradle. If you are running without Gradle, uncomment the line `set PRODUCTVERSION=x.x.x`. Make sure Wix is installed and you are running on Windows. `installer.wxs` contains the xml skeleton of the installer, and `wix.bat` automatically harvests the contents of `ctfscreen-data` into another `.wxs` file which is compiled in with it. If you have files outside of `ctfscreen-data` which you want to be copied into the MSI, make sure you add them manually in `installer.wxs`. 