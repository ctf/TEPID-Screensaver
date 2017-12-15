#CTF Screensaver

To build, run `gradlew jar`

To build MSI, copy `ctfscreen.jar` into `windows/ctfscreen-data`, open `CTF Screensaver.aip` with Advanced Installer, increment version number, and press `Build`

The executable follows the Windows screensaver standard for command line flags. To run as a screensaver, execute `ctfscreen.jar /s`; to run in a window for debugging, execute `ctfscreen.jar /w`.

##Known issues
- Running in fullscreen mode (`/s`) scales badly on non-1080p displays
- Advanced Installer is proprietary and only has a GUI 😭
