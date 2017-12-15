CTF Screensaver on Windows
====================
Usually all you have to do is copy a build of `ctfscreen.jar` into the `ctfscreen-data` directory and run `ctfscreen.scr /s`. 

`ctfscreen.scr` is a light-weight native runner for `ctfscreen.jar`. It is based on WinRun4J. Changes to the configuration of the runner are made by changing the values in `ctfscreen_config.ini` and then running `updateini.bat`, which uses `RCEDIT64.exe` to splice the contents of the config file into the `.scr` binary. `ctfscreen.jar` cannot be installed as a screensaver in Windows without the `.scr` runner. All libraries are located in `ctfscreen-data`, which is specified along with the class containing the main method in `ctfscreen_config.ini`. 
