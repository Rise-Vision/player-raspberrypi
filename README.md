# player-raspberrypi

## Introduction

player-raspberrypi consists of components to run the presentation on Raspbian - Raspberry Pi.

- Installer shell script to configure, launch and auto-update the components
- Cache Java App for maintaining local cache for video files
- Player Java App responsible for launching [Viewer](https://github.com/Rise-Vision/viewer) in Chrome.

player-raspberrypi works in conjunction with [Rise Vision](http://www.risevision.com), the [digital signage management application](http://rva.risevision.com/) that runs on [Google Cloud](https://cloud.google.com).

At this time Chrome is the only browser that this project and Rise Vision supports.

## Built With
- *Java 1.7*
- *Shell script*

## Development 

### Local Development Environment Setup and Installation
The Java projects RiseCache in /rise-cache and RisePlayer in /player folders in repository can be build using eclipse.

You will need to export these to Runnable jar file

- *RisePlayer to RisePlayer.jar and*
- *RiseCache to RiseCache.jar*

Installer script raspbian-installer/rvplayer-installerraspbian.sh in repository can be edited using any editor."


### Run Local
player-raspberrypi can run locally on Raspbian. It can be installed by running command "sudo rvplayer-installerraspbian.sh" in terminal window. The script will download the required components and configure machine.

####Components:

- *Installer*
- *RisePlayer*
- *RiseCache*
- *Chromium*

####Important configuration steps for testing/running on your raspbian machine

Installer script upon launch connects to CORE server and request for components version numbers, if component is missing on local machine or version number is different, the particular component is downloaded.

For your testing its recommended that the version of your updated component should match with the version number set on the Core server otherwise Installer script will replace your copy with the version set on the server.

The Core server URL is coded in the script, you can update the CORE_URL variable in script to connect to test "https://rvacore-test.appspot.com" or production "https://rvaserver2.appspot.com" or local Core server.

Similary update the SHOW_URL to connect to test "http://viewer-test.appspot.com" or production "http://rvashow.appspot.com" Viewer server

Installer script uses following URL to check for current component version numbers $CORE_URL/v2/player/components?os=rsp

> 
**Url**: https://rvacore-test.appspot.com/v2/player/components?os=rsp
**Returns**:
PlayerVersion=2.0.036.rsp.1
PlayerURL=http://storage.googleapis.com/raspbian/player/RisePlayer_Rsp_1.zip
InstallerVersion=2.2.0030rsp.1
InstallerURL=https://rvacore-test.appspot.com/player/download?os=rsp
BrowserVersion=22.0.1229.94
BrowserURL=http://storage.googleapis.com/raspbian/chromium/chrome-linux-raspbian-22.0.1229.94.zip
CacheVersion=1.0.009.rsp.1
CacheURL=http://storage.googleapis.com/raspbian/cache/RiseCache_Rsp_1.zip
JavaVersion=
JavaURL=

If you are making changes to installer script, copy the updated script to file /home/pi/rvplayer/rvplayer and make sure script rvplayer has execute permissions and the installer version number set in variable VERSION match the InstallerVersion set on CORE

If you are making changes to RisePlayer.jar, copy the updated jar file to folder /home/pi/rvplayer/ and the RisePlayer version number set in java application should macth the PlayerVersion set on CORE

If you are making changes to RiseCache.jar, copy the updated jar file to folder /home/pi/rvplayer/RiseCache and the RiseCache version number set in java application should macth the CacheVersion set on CORE

raspbian /home/pi/rvplayer folder contain following:

- *chrome-linux directory - Chromium binaries downloaded by Installer*
- *RiseCache directory - Contains RiseCache.jar and downlaoded video files*
- *RisePlayer.jar*
- *rvplayer - Installer script*
- *chromium.log - Chrmium std err output*
- *RisePlayer.log - RisePlayer log*
- *RiseDisplayNetworkII.ini - Configuration file, created by Installer and updated by RisePlayer, contains Core Server URL, Display ID...*
- *installer.ver - contain Installer vesion number*
- *RisePlayer.ver - contain RisePlayer vesion number*
- *chromium.ver - contain Chromium vesion number*
- *RiseCache.ver - contain RiseCache vesion number*

### Dependencies

- Raspbian Wheezy 3.10
- Installer script download and install all dependent components and requires sudo
-  Chromium v 22.0.1229.94 - Downloaded and installed by Installer

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

[Byron Darlison](https://github.com/ByronDarlison "Byron Darlison")