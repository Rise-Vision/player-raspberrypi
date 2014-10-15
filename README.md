# player-raspberrypi

## Introduction

player-raspberrypi is a set of components; the Installer, Rise Player and Rise Cache. Together, these components are used to show digital signage on a public display.

The Installer is responsible for downloading and installing Rise Player, Rise Cache and Chrome onto the target machine.

Rise Player is responsible for launching Viewer in Chrome, to display HTML content from Rise Vision. In addition, Player will run a local server on port 9449 that is used for communication with Viewer.

Rise Cache will run a local server on port 9494 and serves as a proxy for downloading and serving videos requested by the Video Widget running in Viewer.

Rise Player and Rise Cache works in conjunction with [Rise Vision](http://www.risevision.com), the [digital signage management application](http://rva.risevision.com/) that runs on [Google Cloud](https://cloud.google.com).


At this time Chrome is the only browser that this project and Rise Vision supports.

Built With

 - Java 1.7
 - [Eclipse](http://www.eclipse.org/downloads/)
 
## Development

### Local Development Environment Setup and Installation

#### Installer

For Raspbian, the installer is a shell script. To edit, open rvplayer-installerraspbian.sh file with any text editor.

#### Rise Player and Rise Cache

To build Java projects, you will need Eclipse on your machine. In Eclipse create a new workspace. Import the RiseCache from /rise-cache and RisePlayer from /player into Eclipse.

1. Select File menu
2. Select Import
3. Under General, select "Existing Projects into Workspace"
4. Select the root directory for whichever project you want to import. 

#### To Debug Player project in Eclipse

1. Right click on player project in project Explorer
2. Select Debug as
3. Select Java Application
4. Select Main - com.risevision.riseplayer
5. Select OK

#### To Debug Rise Cache project in Eclipse

1. Right click on rise-cache project in project Explorer
2. Select Debug as
3. Select Java Application
4. Select Main - com.risevision.risecache
5. Select OK

#### When you are ready, build and export the projects as .jar files. From Eclipse,

1. Right Click on project in project Explorer
2. Click Export
3. From the Java option, select "Runnable Jar File"
4. Select correct Launch Configuration created during debug steps above
5. Under export destination, Export
 - RisePlayer as RisePlayer.jar
 - RiseCache as RiseCache.jar

### Run Local

#### Installer

In Raspbian, run the command "sudo ~./rvplayer-installerraspbian.sh" in terminal window.

When launched, Installer connects to the Rise Vision Server and request Component version numbers. If a Component is missing or version number is different, the Component is downloaded.

For testing, it's recommended to set the version of your Component to be equal to the version number on the Server to prevent your Component from being updated from the server. Copy the new updated component in the application folder manually and launch the installer.

The URL's below can be used to confirm current versions of each component.

- Raspbian: https://rvacore-test.appspot.com/v2/player/components?os=rsp

#### Rise Player and Rise Cache

Rise Player and Rise Cache are both .jar's and can be ran by right clicking on the file and running with Java Runtime.

Rise Vision Player requires a Display ID or Claim ID to connect the Display to the Rise Vision Platform.

1. From the [Rise Vision Platform](http://rva.risevision.com/) click on Displays
2. Select Add Display and give it a name.
3. Click save.
4. Copy the Display ID and enter it in the Rise Vision Player on startup.

The Display ID can also be changed in the the "RiseDisplayNetworkII.ini" within the application folder.

## Dependencies

All dependencies like Chromium and Java are downloaded and installed by the installer.

## Submitting Issues 
If you encounter problems or find defects we really want to hear about them. If you could take the time to add them as issues to this Repository it would be most appreciated. When reporting issues please use the following format where applicable:

**Reproduction Steps**

1. did this
2. then that
3. followed by this (screenshots / video captures always help)

**Expected Results**

What you expected to happen.

**Actual Results**

What actually happened. (screenshots / video captures always help)

## Contributing
All contributions are greatly appreciated and welcome! If you would first like to sound out your contribution ideas please post your thoughts to our [community](http://community.risevision.com), otherwise submit a pull request and we will do our best to incorporate it


## Resources
If you have any questions or problems please don't hesitate to join our lively and responsive community at http://community.risevision.com.

If you are looking for user documentation on Rise Vision please see http://www.risevision.com/help/users/

If you would like more information on developing applications for Rise Vision please visit http://www.risevision.com/help/developers/. 

**Facilitator**

[Alan Clayton](https://github.com/alanclayton "Alan Clayton")
