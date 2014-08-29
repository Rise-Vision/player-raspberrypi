###player-raspberrypi###
**Copyright © 2010 - August 27, 2014 Rise Vision Incorporated.**

Use of this software is governed by the GPLv3 license (reproduced in the LICENSE file).

player-raspberrypi is a set of shell script, Java Apps, responsible for launching Viewer in Chrome, to display HTML content from Rise Vision - our digital signage management application. More information about Viewer can be found in the Rise-Vision/viewer repository

Included in this repository are the installation scripts for Raspberry Pi and java applications Player and Cache. When ran, the installer will download and install the Player - “RisePlayer.jar”, rise-cache - “RiseCache.jar”, Chrome and Java 7. 


When installation has completed, the Java App’s, RisePlayer.jar and RiseCahce.jar, are launched. 

RisePlayer.jar is responsible for launching Chrome. In addition, RisePlayer.jar will run a local server on port 9449 that is used for communication with Viewer.

RiseCache.jar will run a local server on port 9494 and serves as a proxy for downloading and serving videos requested by the HTML running in viewer.

**How to get started?** 
All source code for the Project is included in this repository and organized as follows:

 * raspbian-installer: a Linux shell script that will download and installs Player, Rise Cache and the Player dependencies on Raspberry Pi OS. 

 * player: the Java source code for the Player application.

 * rise-cache: the Java source code for Rise Cache


**To develop on your local machine:**

player and rise-cache are Java Applications. As such, any IDE can be used, requires JDK 1.7  or higher.


raspbian-installer is shell script and require nothing more than a text editor. 
Installer connects to server, inquires about new versions for player, rise-cache, chromium and java. If available, download and install. 
And it perform some checks and configurations, launch player and rise-cache java applications.
Player launches the presentation by opening Rise Viewer in Chromium.



You can create a Display id by visiting http://risevision.com, you will need to register if you didn't have already and know its FREE.

If you have any questions or problems please don't hesitate to join our lively and responsive community athttp://community.risevision.com.


If you are looking for user documentation on Rise Vision please see http://www.risevision.com/help/users/


If you would like more information on developing applications for Rise Vision please visit http://www.risevision.com/help/developers/.


And if you are **considering contributing to this open source project**, our favourite option, we have 3 good reasons why we released this code under version 3 of the GNU General Public License, and we think they are 3 good reasons for why you should get involved too:

* Together we can make something far better than we could on our own.

* If you want to use our code to make something that is specific to you, and that doesn’t fit with what we want to do, we don’t want to get in your way. Take our code and make just what you need.

* We know that some of you nervous types worry about what happens if our company gets taken out in the zombie apocalypse. We get it, and neither one of us wants to deal with that delicate question of software escrow agreements for the “just in case we kick the bucket scenario”. No worries! We made it easy. No fuss, no cost, no lawyers! We published the software here. Have at it.


3 compelling reasons for why you should actively join our project.

Together we can make something better than either of us could on our own.

If you have something completely different in mind, no problem, take our code, fork it, and make what you need, but respect the open source movement, and our license, and keep it open.


Become a zombie crusader!


Are we missing something? Something could be better? Jump in, branch our code, make what you want, and send us a Pull Request. If it fits for both of us then of course we will accept it, maybe with a tweak or two, test it, and deploy it. If it doesn’t fit, no worries, just fork our code and create your own specialized application for your specific needs. Or, if you’re just feeling paranoid, download the code, and put it under your mattress.


**Either way, welcome to our project!**

