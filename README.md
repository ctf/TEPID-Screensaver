#CTF Screensaver

##Build
Gradle tasks: 
- `jar` build jar
- `windows` build exe
- `msi` build msi (make sure you are running on Windows and Wix is installed before running this)

##Run
The executable follows the Windows screensaver standard for command line flags. To run as a screensaver, execute `ctfscreen.jar /s`; to run in a window for debugging, execute `ctfscreen.jar /w`.

##Known issues
- Running in fullscreen mode (`/s`) scales badly on non-1080p displays