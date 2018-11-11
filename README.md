# CTF Screensaver

##Features
The CTF Screensaver is the most extra screensaver around. It has a number of cool features:

### Information Screens 
A band at the top allows for colourful informational images to be broadcast. This makes it much more likely that people might actually notive what you've put up there. It also enables you to use images and logos in the info-blasts, which might catch someone's attention. We got about 100% increase in people noticing things when we rolled out this feature.

The changing of the info screens is synchronised across devices, so that it looks nice if you have a whole lab going at once.

Slides are pulled from the ANNOUNCEMENT_SLIDE_LOCATION directory. This can easily be populated with an active directory task, a script, or even make it a protected-write+open-read directory on a network share

### TEPID tie-in
The screensaver is the perfect way to display information about the TEPID printing service to your users. It displays print queues, their status, and (if they're up) it displays the 10 most recent print jobs from each queue (displaying the name of who printed, the time, and which printer in the queue it came out of). It will also display if a job failed and the reason for the failure. 

If nothing has been printed in a queue in the past hour, a happy pusheen will be munching on popcorn. If a printer is down, a sad pusheen will be weeping. The area normally occupied by print jobs will instead be filled with a status-appropriate coloured rectangle, to aid people in easily identifying when a particular queue is completely down.

### Office Mode
The screensaver also has a special mode for your office's computers. These are the computers which your IT staff use (office computers are identified by matching their hostnames against the OFFICE_REGEX config option). Office mode enables 2 main items:
1. Calendar : a ticker on a second display will cycle through upcoming events published to an ics calendar.
1. Gravatar : the screensaver will attempt to pull a gravatar of the logged-in user's domain email. If it doesn't find one, it will attempt a Google search for the user's name and a custom search string (which can help narrow the search to probably the right person). If they have a LinkedIn account the SEO is usually good enough to get the average person to the first result. Hilarity may ensue when the search pulls up someone completely unrelated to the actual person.

There's also some mildly nonfunctional members-on-duty display. This currently doesn't work, but it could someday.

## Compatibility

Requires TEPID 2.2 minimum.

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
