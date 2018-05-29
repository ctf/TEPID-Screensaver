# CTF Screensaver

## Build
Gradle tasks: 
- `jar` build jar
- `windows` build jar and package it with a ctfscreen.scr launcher and libs in `ctfscreen-data` for copying to `System32`
- `copyConfigs` copies your config files from somewhere else, so they get bundled into the JAR
- `msi` build msi (make sure you are running on Windows and Wix is installed before running this)

## Run
The executable follows the Windows screensaver standard for command line flags. To run as a screensaver, execute `ctfscreen.jar /s`; to run in a window for debugging, execute `ctfscreen.jar /w`.

## Known issues
- Running in fullscreen mode (`/s`) scales badly on non-1080p displays

## Configuration Options
Configurations are defined in the common config files. The configs use the TEPID standard configuration system. This allows them to be defined once, outside of the project's file tree, and used across all TEPID programs. The Gradle task copyConfigs will copy the configs from the project property "tepid_config_dir", which can be set in a number of ways:
    - As a standard project property
    - As a project property passed by command line ("-Ptepid_config_dir=CONFIG_DIR")
    - As an environment variable "tepid_config_dir"
A lookup for the environment variable only occurs if the project property is not defined in one of the other two ways.

### URL.properties
Both of these options have testing and production
- Server URL : the URL for the application root of the server with all the rest endpoints
- Web URL : the URL for the web server, typically the same but without the slug targetting the tomcat webapp

### screensaver.properties
- OFFICE_REGEX : regex string to match office computers, which will have office relevant options like an avatar and internal announcements
- GRAVATAR_SEARCH_TERMS : search terms to help narrow down the results for the avatar picture in case a gravatar is not found, such as your organisation name
- GOOGLE_CUSTOM_SEARCH_KEY : the custom part of the url containing your key and the cx
- ICS_CALENDAR_ADDRESS : address of ics to pull events from
- REPORT_MALFUNCTIONING_COMPUTER_TEXT : completes the sentence to "Report this malfunctioning computer to", displayed when a computer cannot contact the network
- BACKGROUND_PICTURE_LOCATION : location for the background picture
- ANNOUNCEMENT_SLIDE_LOCATION : directory containing the announcement slides
