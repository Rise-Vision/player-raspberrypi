#!/bin/sh
# Rise Vision Player installation script

set -e

CURRENT_USER=$USER
SUDO_HOME=$HOME
USER_HOME=$HOME

if [ "$USER" = "root" ] && [ -n "$SUDO_USER" ]
then
	CURRENT_USER="$SUDO_USER"
	SUDO_HOME="/root"
	USER_HOME="/home/$SUDO_USER"
fi

echo "HOME=$HOME"
echo "USER=$USER"
echo "SUDO_USER=$SUDO_USER"
echo "SUDO_HOME=$SUDO_HOME"
echo "USER_HOME=$USER_HOME"

DISPLAY_ID="" 
CLAIM_ID="#CLAIM_ID#" 

VERSION="2.2.0030rsp"

RVPLAYER="rvplayer"
CHROMIUM="chrome"

#if [  -f /usr/bin/lsb_release ]; then
#	OS=$(lsb_release -si)
#	ARCH=$(uname -m | sed 's/x86_//;s/i[3-6]86/32/')
#	OSVER=$(lsb_release -sr)
#	if [ "$ARCH" = "64" ]
#	then
#		RISEOS="lnx64"
#	else
#		RISEOS="lnx"
#	fi
#else
	OS="rsp"
	ARCH=$(uname -m)
	OSVER=$(uname -n)
	RISEOS="rsp"
#fi

abspath=$(cd ${0%/*} && echo $PWD/${0##*/})
#abspath="$(echo "$abspath" | sed -e 's/[()& ]/\\&/g')"
CHROME_LINUX="chrome-linux"
RISE_PLAYER_LINUX="RisePlayer"
RISE_CACHE_LINUX="RiseCache"

CORE_URL="https://rvaserver2.appspot.com" # "https://rvacore-test.appspot.com" # "http://127.0.1.1/temp"
SHOW_URL="http://rvashow.appspot.com" # "http://viewer-test.appspot.com"

DISPPLAYERROR_URL="$CORE_URL/player/update"
TYPE_CHROMIUM="chromium"
TYPE_INSTALLER="installer"
TYPE_JAVA="java"
TYPE_RISE_PLAYER="RisePlayer"
TYPE_RISE_CACHE="RiseCache"

VIEWER_URL="$SHOW_URL/Viewer.html"

RISE_CACHE_URL="http://localhost:9494"
RISE_CACHE_PING_URL="$RISE_CACHE_URL/ping?callback=test"
RISE_CACHE_SHUTDOWN_URL="$RISE_CACHE_URL/shutdown"

RISE_PLAYER_URL="http://localhost:9449"
RISE_PLAYER_PING_URL="$RISE_PLAYER_URL/ping?callback=test"
RISE_PLAYER_SHUTDOWN_URL="$RISE_PLAYER_URL/shutdown"

PREFERENCES="{\"countryid_at_install\":0,\"default_search_provider\":{\"enabled\":false},\"geolocation\":{\"default_content_setting\":1},\"profile\":{\"content_settings\":{\"pref_version\":1},\"default_content_settings\": {\"geolocation\": 1},\"exited_cleanly\":true}}"
MASTER_PREFERENCES="{\"distribution\":{\"suppress_first_run_bubble\":true,\"import_history\": false,\"import_bookmarks\": false,\"import_home_page\": false,\"import_search_engine\": false,\"do_not_launch_chrome\": false,\"oem_bubble\": false}}"

TEMP_PATH="$USER_HOME/$RVPLAYER/temp"
INSTALL_PATH="$USER_HOME/$RVPLAYER"
STARTUP_SCRIPT_FILE="$USER_HOME/$RVPLAYER/$RVPLAYER"

CONFIG_PATH=".config/$RVPLAYER"
CACHE_PATH=".cache/$RVPLAYER"
PREFERENCES_PATH=".config/$RVPLAYER/Default"
MASTER_PREFERENCES_PATH="$INSTALL_PATH/$CHROME_LINUX/master_preferences"
FIRST_RUN_FILE="First Run"
PREFERENCES_FILE="Preferences"
RDNII_FILE="RiseDisplayNetworkII.ini"

TERMS_FILE="$INSTALL_PATH/$RDNII_FILE"
LOG_FILE="$INSTALL_PATH/installer0.log"
LOG_FILE1="$INSTALL_PATH/installer1.log"
LOG_FILE_MAXIMUM_SIZE=921600 #3K per day, 3Kx30=90K=921600B

DISPLAYERR_FILE="$INSTALL_PATH/display.err"

AUTOSTART_PATH="$USER_HOME/.config/autostart"
AUTOSTART_FILE="$RVPLAYER.desktop"

PARAM_INSTALLER_VERSION="InstallerVersion"
PARAM_INSTALLER_URL="InstallerURL"
PARAM_CHROMIUM_VERSION="BrowserVersion"
PARAM_CHROMIUM_URL="BrowserURL"
PARAM_JAVA_VERSION="JavaVersion"
PARAM_JAVA_URL="JavaURL"
PARAM_RISE_PLAYER_VERSION="PlayerVersion"
PARAM_RISE_PLAYER_URL="PlayerURL"
PARAM_RISE_CACHE_VERSION="CacheVersion"
PARAM_RISE_CACHE_URL="CacheURL"

SILENT=false
CLEAR_CACHE=false
FIRST_TIME=false

VALUE_NO="0"
VALUE_YES="1"

VALUE_INSTALLER_VERSION=""
VALUE_INSTALLER_URL=""
VALUE_CHROMIUM_VERSION=""
VALUE_CHROMIUM_URL=""
VALUE_JAVA_VERSION=""
VALUE_JAVA_URL=""
VALUE_RISE_CACHE_VERSION=""
VALUE_RISE_CACHE_URL=""
VALUE_RISE_PLAYER_VERSION=""
VALUE_RISE_PLAYER_URL=""

CURRENT_CHROMIUM_VERSION=""
CURRENT_JAVA_VERSION=""
CURRENT_RISE_PLAYER_VERSION=""
CURRENT_RISE_CACHE_VERSION=""


rvp_log_size_check() {
if [ -f $LOG_FILE ]
then
	actualsize=$(du -b "$LOG_FILE" | cut -f 1)
	if [ $actualsize -ge $LOG_FILE_MAXIMUM_SIZE ]
	then
	    mv $LOG_FILE $LOG_FILE1
	fi
fi
}

log() {
	echo "$1"
	echo "$(date) - $1">>${LOG_FILE}
}

rvp_update_send_displayerror_core () {
set +e
	error_code="$1"
	error_occourance=$2
	found=false
	report_to_core=false
	error_to_report=$error_code
	report_to_core_success=false
	
	line=$(cat "$DISPLAYERR_FILE" | grep -E "^$error_code,")
	if [ -n "$line" ]
	then
		found=true
		occourances=$(echo "$line" | awk -F"," '{print $2}')
		if [ -n "$occourances" ]
		then
			if [ $error_occourance = -1 ] && [ $occourances -gt -1 ]; then occourances=-1; fi
			if [ $error_occourance = 1 ] && [ $occourances -lt 1 ]; then occourances=0; fi
			occourances=$(( $occourances +  $error_occourance))
		else
			occourances=$error_occourance
		fi
		
	else
		occourances=$error_occourance
	fi
	
	if [ "$error_code" = "1005" ]  #java version not correct
	then
		if [ $occourances = 1 ]; then report_to_core=true;error_to_report=$error_code; fi
		if [ "$occourances" -gt "10" ]; then report_to_core=true;error_to_report=$error_code; fi
		if [ "$error_occourance" = -1 ]; then report_to_core=true;error_to_report=0; fi
	fi
	
	if [ -n "$DISPLAY_ID" ] && [ $report_to_core = true ]
	then
		#report display error to core
		rvp_get_response_code "$DISPPLAYERROR_URL?id=$DISPLAY_ID&st=$error_to_report"
		if [ "$response_code" = "200" ]
		then	
			log "successfully sent error reprot to core"
			report_to_core_success=true
			occourances=1
		else
			log "unable to send error reprot to core"
		fi
	fi
	
	if [ $error_to_report = 0]
	then
		#delete
		if [ $found = true ]
		then
			sed -i "s/$error_code,.*//g" "$DISPLAYERR_FILE"
		fi
	else
		if [ $found = true ]
		then
			sed -i "s/$error_code,.*/$error_code,$occourances/g" "$DISPLAYERR_FILE"
		else
			echo "$error_code,$occourances" >> $DISPLAYERR_FILE
		fi
	fi
set -e
}

rvp_exit_with_error() {

	if $SILENT
	then
		log 'Rise Vision Player failed to install some components correctly due to the following error:'
		log "$1"
	else
		log ''
		log 'Rise Vision Player failed to install correctly due to the following error:'
		log ''
		log "$1"
		log ''
		log 'If you cannot correct this error please post the details to http://community.risevision.com'

		#if $FIRST_TIME
		#then
			#show popup if zenity is installed
			if [ -e /usr/bin/zenity ]
			then

				USER_WARNING="Rise Vision Player failed to install correctly due to the following error:

$(echo $1 | sed -e 's/\\/\\\\/g' -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g')

This error has been written to this log file: ${LOG_FILE}

If you cannot correct this error please post the details to http://community.risevision.com

Press Yes to retry or No to abandon the installation."
				set +e
				env DISPLAY=:0.0 zenity --question --title "Installation Failed" --text "$USER_WARNING";
				if [ $? -eq 0 ] 
				then
				    rvp_run_installer
				else
				    exit
				fi
				set -e
			else
				echo "This error has been written to this log file: ${LOG_FILE}
If you cannot correct this error please post the details to http://community.risevision.com"
	
				read -p 'Press Y to retry or N to abandon the installation. (y/n)?' choice
				case "$choice" in 
				y|Y ) rvp_run_installer;;
				* ) exit;;
				esac	
			fi
		#else
		#	log 'Rise Vision Player failed to install some components correctly due to the following error:'
		#	log "$1"
		#fi
	fi
}

rvp_run_installer() {

	
	log "$abspath"
	
	chmod 755 "$abspath"
	chown $CURRENT_USER:$CURRENT_USER -R "$abspath"
	chown $CURRENT_USER:$CURRENT_USER -R "$INSTALL_PATH"
	
	log "Restarting Installer"
	if $SILENT 
	then 
		"$abspath" /S
	else
		"$abspath"
	fi
	

	# exit this version and let new version take over
	exit 0
}
rvp_fix_display_id() {
	
	# empty $DISPLAY_ID if it starts with #
	if [ "$(echo $DISPLAY_ID | head -c 1)" = "#" ]
	then
		DISPLAY_ID=""
		log 'reset DISPLAY_ID'
	fi
	
	# empty $CLAIM_ID if it starts with #
	if [ "$(echo $CLAIM_ID | head -c 1)" = "#" ]
	then
		CLAIM_ID=""
		log 'reset CLAIM_ID'
	fi
	
}

rvp_load_display_id() {
	
	# load $DISPLAY_ID if empty
	if [ -z "$DISPLAY_ID" ] && [ -f "$INSTALL_PATH/$RDNII_FILE" ]
	then
		set +e
		line="$(grep -F -m 1 'displayid=' $INSTALL_PATH/$RDNII_FILE)"
		DISPLAY_ID="$(echo $line | cut -d = -f 2-)"
		#remove carriage return
		DISPLAY_ID="$(echo $DISPLAY_ID | tr -d '\r')" 
		set -e
	fi
		
}

rvp_save_display_id() {

	if [ ! -f "$INSTALL_PATH/$RDNII_FILE" ] ||
		[ "$(grep viewerurl=$VIEWER_URL $INSTALL_PATH/$RDNII_FILE)"  != "viewerurl=$VIEWER_URL" ] ||
		[ "$(grep coreurl=$CORE_URL $INSTALL_PATH/$RDNII_FILE)"  != "coreurl=$CORE_URL" ]
	then
		echo "[RDNII]
displayid=$DISPLAY_ID
claimid=$CLAIM_ID
viewerurl=$VIEWER_URL
coreurl=$CORE_URL
" > $INSTALL_PATH/$RDNII_FILE
	fi
}

rvp_install_script() {

	mkdir -p $INSTALL_PATH

	if [ "$abspath" != $STARTUP_SCRIPT_FILE ]; then

		cp "$abspath" $STARTUP_SCRIPT_FILE
		chmod 755 $STARTUP_SCRIPT_FILE
		log "Startup script updated."
	fi
	
	echo $VERSION > $INSTALL_PATH/$TYPE_INSTALLER".ver"
}

rvp_get_response_code() {
	
	local URL=$1
	
	set +e
	
	response_code=$(wget --spider --server-response $URL 2>&1 | awk '/^  HTTP/{print $2}')
	
	set -e
}

rvp_get_update() {

	#rename cromium_version if exists to make it compatible with Player 1.
	if [ -f "$INSTALL_PATH/${TYPE_CHROMIUM}_version" ]
	then
		mv -u $INSTALL_PATH/$TYPE_CHROMIUM"_version" $INSTALL_PATH/$TYPE_CHROMIUM".ver"
	fi	

	CURRENT_CHROMIUM_VERSION=`cat $INSTALL_PATH/$TYPE_CHROMIUM".ver" 2>&1` || CURRENT_CHROMIUM_VERSION=""
	CURRENT_JAVA_VERSION=`cat $INSTALL_PATH/$TYPE_JAVA".ver" 2>&1` || CURRENT_JAVA_VERSION=""
	CURRENT_RISE_PLAYER_VERSION=`cat $INSTALL_PATH/$TYPE_RISE_PLAYER".ver" 2>&1` || CURRENT_RISE_PLAYER_VERSION=""
	CURRENT_RISE_CACHE_VERSION=`cat $INSTALL_PATH/$TYPE_RISE_CACHE".ver" 2>&1` || CURRENT_RISE_CACHE_VERSION=""

	update_url="$CORE_URL/v2/player/components?os=$RISEOS&id=$DISPLAY_ID"

	log "Checking for updates..."
	log $update_url

	set +e

	update_content=`wget -O - $update_url` || update_content="" rvp_exit_with_error "Update check failed $update_url"

	set -e

	upgrade_needed=$VALUE_NO

	for line in $update_content ; do

		log $line

	 	p_name="$(echo "$line" | cut -d = -f 1)"
		p_value="$(echo "$line" | cut -d = -f 2-)"
		   	
		case $p_name in

			$PARAM_INSTALLER_VERSION ) VALUE_INSTALLER_VERSION=$p_value ;;
			$PARAM_INSTALLER_URL ) VALUE_INSTALLER_URL=$p_value ;;
			$PARAM_CHROMIUM_VERSION ) VALUE_CHROMIUM_VERSION=$p_value ;;
			$PARAM_CHROMIUM_URL ) VALUE_CHROMIUM_URL=$p_value ;;
			$PARAM_JAVA_VERSION ) VALUE_JAVA_VERSION=$p_value ;;
			$PARAM_JAVA_URL ) VALUE_JAVA_URL=$p_value ;;
			$PARAM_RISE_PLAYER_VERSION ) VALUE_RISE_PLAYER_VERSION=$p_value ;;
			$PARAM_RISE_PLAYER_URL ) VALUE_RISE_PLAYER_URL=$p_value ;;
			$PARAM_RISE_CACHE_VERSION ) VALUE_RISE_CACHE_VERSION=$p_value ;;
			$PARAM_RISE_CACHE_URL ) VALUE_RISE_CACHE_URL=$p_value ;;
			
		esac
	   
	done

}

rvp_kill_rise_player() {

	set +e

	wget --spider --tries=1 $RISE_PLAYER_SHUTDOWN_URL >/dev/null 2>&1

	set -e

	sleep 3

}

rvp_kill_rise_cache() {

	set +e

	wget --spider --tries=1 $RISE_CACHE_SHUTDOWN_URL >/dev/null 2>&1

	set -e
	
	sleep 3

}

rvp_kill_chromium() {

	if ps ax | grep -v grep | grep $CHROMIUM > /dev/null
	then
		if ! $SILENT
		then
		
			#show popup if zenity is installed
			if [ -e /usr/bin/zenity ]
			then
				WARNING_TEXT='Chrome, Chromium or Java, are running and the installation program will close them to complete the Rise Vision Player setup. Please save any data and press Okay when you are ready to proceed.'
				set +e
				env DISPLAY=:0.0 zenity --warning --text "$WARNING_TEXT";				
				set -e
			else
				echo ''
				echo 'Chrome, Chromium or Java, are running and the installation program will close them to complete the Rise Vision Player setup.'
				echo 'Please save any data and press any when you are ready to proceed.'
				read -p 'Please save any data and press any when you are ready to proceed.'
			fi
		fi
	fi
		
	killall "$CHROMIUM" || log "no Chromiums to kill"
	sleep 3

	if ps ax | grep -v grep | grep $CHROMIUM > /dev/null
	then
		sleep 10

		if ps ax | grep -v grep | grep $CHROMIUM > /dev/null
		then
			killall "$CHROMIUM" || log "no Chromiums to kill"
			sleep 3
		fi	
	fi
}

rvp_reset_chromium() {

	log "Closing Chromium and clearing its cache..."

	rvp_kill_chromium
	rm -rf $SUDO_HOME/$CACHE_PATH
	rm -rf $SUDO_HOME/$CONFIG_PATH
	
	if [ $SUDO_HOME != $USER_HOME ]
	then
		rm -rf $USER_HOME/$CACHE_PATH
		rm -rf $USER_HOME/$CONFIG_PATH
	fi
}

rvp_download_and_run_installer() {

	# begin support for rollback to Player 1
	# check if installer version begins with 1
	if [ "$(echo $VALUE_INSTALLER_VERSION | head -c 2)" = "1." ] 
	then
		VALUE_INSTALLER_URL="$CORE_URL/player/download?os=$RISEOS&displayId=$DISPLAY_ID" 
	fi
	# end support for rollback to Player 1
	
	#abspath=$(cd ${0%/*} && echo $PWD/${0##*/})
	
	log "$abspath"
	
	# setting wget options
	:> wgetrc
	echo "noclobber = off" >> wgetrc
	echo "dir_prefix = ." >> wgetrc
	echo "dirstruct = off" >> wgetrc
	echo "verbose = on" >> wgetrc
	echo "progress = dot:default" >> wgetrc
	echo "output-document = $abspath" >> wgetrc
	# downloading zip
	log "Downloading Installer..."
	WGETRC=wgetrc wget $VALUE_INSTALLER_URL || rvp_exit_with_error "Installer download failed $VALUE_INSTALLER_URL"
	rm -f wgetrc
	log "Download complete."
	
	chmod 755 "$abspath"
	chown $CURRENT_USER:$CURRENT_USER -R "$abspath"
	chown $CURRENT_USER:$CURRENT_USER -R $INSTALL_PATH
	
	log "Installer updated. Restarting new installer version."
	if $SILENT 
	then 
		"$abspath" /S
	else
		"$abspath"
	fi

	# exit this version and let new version take over
	exit 0

}

rvp_download_and_unpack() {

	# $1 - download URL
	# $2 - file name
	# $3 - temp path

	mkdir -p $3
	cd $3
	
	# setting wget options
	:> wgetrc
	echo "noclobber = off" >> wgetrc
	echo "dir_prefix = ." >> wgetrc
	echo "dirstruct = off" >> wgetrc
	echo "verbose = on" >> wgetrc
	echo "progress = dot:default" >> wgetrc
	echo "output-document = $2.zip" >> wgetrc 

	# downloading zip
	log "Downloading...$1"
	rm -f "$2.zip"
	WGETRC=wgetrc wget $1 || rvp_exit_with_error "Download failed $1"
	rm -f wgetrc
	log "Download complete."

	rm -rf $2*/

	# unzipping
	unzip -bo "$2.zip" || rvp_exit_with_error "Cannot unzip $2.zip"
	
	rm -f "$2.zip"
}

rvp_install_prerequisites_sudo() {

	if [ "installinstalled" = $(dpkg -s openjdk-7-jre 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
	then
	    log "Package openjdk-7-jre is already installed"
	else
	    log "Package openjdk-7-jre is not installed, installing now"
	    apt-get -y install openjdk-7-jre --allow-unauthenticated
	    apt-get -y update
	    log "Finished installing package openjdk-7-jre"
	fi

	rvp_check_java_version
	
	if [ "$RISEOS" = "rsp" ]
	then
		# this will be effective after restart
		sed -i.bak 's/.*xserver-command.*/xserver-command=X -s 0 dpms/g' /etc/lightdm/lightdm.conf
		
		if [ "installinstalled" = $(dpkg -s libvpx1 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
		then
		    log "Package libvpx1 required by chromium is already installed"
		else
		    log "Package libvpx1 is not installed, installing now"
		    apt-get -y install libvpx1 --allow-unauthenticated
		    apt-get -y update
		    log "Finished installing package libvpx1"
		fi
		
		if [ "installinstalled" = $(dpkg -s libspeex1 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
		then
		    log "Package libspeex1 required by chromium is already installed"
		else
		    log "Package libspeex1 is not installed, installing now"
		    apt-get -y install libspeex1 --allow-unauthenticated
		    apt-get -y update
		    log "Finished installing package libspeex1"
		fi
	fi

	#if [ "installinstalled" = $(dpkg -s libxss1 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
	#then
	#    log "Package libxss1 is already installed"
	#else
	#    log "Package libxss1 is not installed, installing now"
	#    apt-get -y install libxss1 --allow-unauthenticated
	#    log "Finished installing package libxss1"
	#fi	
	
	if [ "installinstalled" = $(dpkg -s at 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
	then 
	    echo "Package at is already installed"
	else
	    echo "Package at is not installed, installing now"
	    apt-get -y install at --allow-unauthenticated
	    echo "Finished installing package at"
	fi

}

rvp_check_java_version() {
	set +e
	java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print substr ($2, 0, 4)}')
    	    echo "Default Java Version is $java_version"
	
	    REQ_JAVA_VERSION=1.7
    	    if [ "$java_version" \< "$REQ_JAVA_VERSION" ]
    	    then
		#rvp_update_send_displayerror_core "1005" 1    	    
    	    	rvp_exit_with_error "
RisePlayer requires java version 1.7 or later to be default java version.

To set default java version run \"sudo update-alternatives --config java\"
and follow the prompts.
"
#	    else
#		cat $DISPLAYERR_FILE | grep -E "^1005,"  >/dev/null 2>&1 && {
#			echo "Display Error 1005 "		    
#			#rvp_update_send_displayerror_core "1005" -1
#		}
    	    fi
	set -e
}

rvp_install_prerequisites_check() {

	if [ "installinstalled" = $(dpkg -s openjdk-7-jre 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
	then
	    log "Package openjdk-7-jre is already installed"
	    rvp_check_java_version
	else
	    rvp_exit_with_error "Package openjdk-7-jre is not installed, Run the installer again with sudo to install all the required components."
	fi

	if [ "$RISEOS" = "rsp" ]
	then
		if [ "installinstalled" = $(dpkg -s libvpx1 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
		then
		    log "Package libvpx1 required by chromium is already installed"
		else
			rvp_exit_with_error "Package libvpx1 required by chromium is not installed, Run the installer again with sudo to install all the required components."
		fi
		
		if [ "installinstalled" = $(dpkg -s libspeex1 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
		then
		    log "Package libspeex1 required by chromium is already installed"
		else
			rvp_exit_with_error "Package libspeex1 required by chromium is not installed, Run the installer again with sudo to install all the required components."
		fi
	fi

	if [ "installinstalled" = $(dpkg -s libxss1 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
	then
	    log "Package libxss1 is already installed"
	else
	    rvp_exit_with_error "Package libxss1 is not installed, Run the installer again with sudo to install all the required components."
	fi

	if [ "installinstalled" = $(dpkg -s at 2>&1 | grep 'Status: ' | awk -F' ' '{print $2$4}') ]
	then	
	    log "Package at is already installed"
	else
	    rvp_exit_with_error "Package at is not installed, Run the installer again with sudo to install all the required components."
	fi	

}

rvp_install_updates() {
	
	cd $TEMP_PATH
	find * -print | cpio -pvdmu $INSTALL_PATH
	
	chmod -R g+r,a+r,a+X $INSTALL_PATH/*
	chmod 755 $USER_HOME/$RVPLAYER/chrome-linux/chrome
	
	cd $INSTALL_PATH
	rm -rf $TEMP_PATH*		
} 

rvp_chromium_master_preferences() {
	if [ -n "$VALUE_CHROMIUM_VERSION" ]
	then
		if [ ! -f $MASTER_PREFERENCES_PATH ]		
		then
			echo "$MASTER_PREFERENCES" > $MASTER_PREFERENCES_PATH
		fi
	fi
}

rvp_chromium_hacks() {
	# fix for chrome v 25.0.1364.0 on Ubuntu v 13.04
	if [ -n "$VALUE_CHROMIUM_VERSION" ] && [ "$OS" = "Ubuntu" ]
		#[ "$OSVER" = "13.04" ] && \
		#[ "$VALUE_CHROMIUM_VERSION" = "25.0.1364.0" ] && \
	then
		if [ "$ARCH" = "32" ] && \
			[ ! -f /lib/i386-linux-gnu/libudev.so.0 ] && \
			[ -f /lib/i386-linux-gnu/libudev.so.1 ]
		then
			ln -s /lib/i386-linux-gnu/libudev.so.1 /lib/i386-linux-gnu/libudev.so.0
		fi	
		if [ "$ARCH" = "64" ] && \
			[ ! -f /lib/x86_64-linux-gnu/libudev.so.0 ] && \
			[ -f /lib/x86_64-linux-gnu/libudev.so.1 ]
		then
			ln -s /lib/x86_64-linux-gnu/libudev.so.1 /lib/x86_64-linux-gnu/libudev.so.0
		fi
	
		if [ "$ARCH" = "64" ] && \
				[ "$VALUE_CHROMIUM_VERSION" = "29.0.1547.65" ]
		then
			#chmod 755 $USER_HOME/$RVPLAYER/chrome-linux/chrome
			mkdir -p /usr/lib/chromium-browser
			cp $USER_HOME/$RVPLAYER/chrome-linux/chromium-browser-sandbox /usr/lib/chromium-browser/chromium-browser-sandbox
			chown -Rv root:root /usr/lib/chromium-browser/chromium-browser-sandbox
			chmod -Rv 4755 /usr/lib/chromium-browser/chromium-browser-sandbox
		fi

		if [ "$VALUE_CHROMIUM_VERSION" = "31.0.1626.0" ]
		then
			if [ "$ARCH" = "32" ] && \
				[ "$OSVER" = "12.10" ] && \
				[ $(dpkg -s libnss3 | grep Version: | cut -d ' ' -f 2 | cut -d '-' -f 1) \< "3.14.3" ]
			then
				add-apt-repository "deb http://security.ubuntu.com/ubuntu quantal-security main"
				apt-get update
				apt-get -y install libnss3 --allow-unauthenticated
	fi

			sandbox_path="/usr/lib/chrome/chrome_sandbox"
			#chmod 755 $USER_HOME/$RVPLAYER/chrome-linux/chrome
			mkdir -p /usr/lib/chrome
			cp $USER_HOME/$RVPLAYER/chrome-linux/chrome_sandbox $sandbox_path
			chown -Rv root:root $sandbox_path
			chmod -Rv 4755 $sandbox_path
			
			
			if(grep -q CHROME_DEVEL_SANDBOX /etc/environment)
			then
				sed -i "s%CHROME_DEVEL_SANDBOX.*%CHROME_DEVEL_SANDBOX=\"$sandbox_path\"%g" /etc/environment
			else
				echo "CHROME_DEVEL_SANDBOX=$sandbox_path" >> /etc/environment
			fi		
		fi		
	fi

	if [ -n "$VALUE_CHROMIUM_VERSION" ] && [ "$RISEOS" = "rsp" ]
	then
		#sudo chmod 755 $USER_HOME/$RVPLAYER/chrome-linux/chrome
		mkdir -p /usr/lib/chromium
		cp $USER_HOME/$RVPLAYER/chrome-linux/chromium-sandbox /usr/lib/chromium/chromium-sandbox
		chown -Rv root:root /usr/lib/chromium/chromium-sandbox
		chmod -Rv 4755 /usr/lib/chromium/chromium-sandbox
		
		if [ ! -f /usr/lib/nss ] && \
			[ -f /usr/lib/arm-linux-gnueabihf/nss/ ]
		then
			ln -s /usr/lib/arm-linux-gnueabihf/nss/ /usr/lib/nss	
		fi
		
	fi
} 
rvp_confirm_add_player_to_autostart() {

	if ! $SILENT
	then
		#show popup if zenity is installed
		if [ -e /usr/bin/zenity ]
		then
			USER_WARNING='Please click Yes to have Rise Vision Player run automatically when this computer starts, otherwise click No and Rise Vision Player will have to be manually started.
		
Thank you for installing Rise Vision Player!'
			set +e
			env DISPLAY=:0.0 zenity --question --text "$USER_WARNING"
			if [ $? -eq 0 ] 
			then
			    rvp_add_player_to_autostart
			else
			    rm -f $AUTOSTART_PATH/$AUTOSTART_FILE
			fi
			set -e
		else
			log 'Please press Yes to have Rise Vision Player run automatically when this computer starts, otherwise press No and Rise Vision Player will have to be manually started.'
			echo ''
			echo 'Thank you for installing Rise Vision Player!'
		
			read -p 'Pressing either key completes the installation and Rise Vision Player will start. (y/n)?' choice
			case "$choice" in 
			y|Y ) rvp_add_player_to_autostart;;
			* ) rm -f $AUTOSTART_PATH/$AUTOSTART_FILE;;
			esac
		fi
	fi

}

rvp_add_player_to_autostart() {

	mkdir -p $AUTOSTART_PATH
	:> $AUTOSTART_PATH/$AUTOSTART_FILE
	echo "[Desktop Entry]" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	echo "Encoding=UTF-8" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	echo "Name=Rise Vision Player" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	echo "Comment=" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	echo "Icon=" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	if [ "$USER" = "root" ]
	then 
	echo "Exec=sudo $STARTUP_SCRIPT_FILE /S" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	else
		echo "Exec=$STARTUP_SCRIPT_FILE /S" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	fi
	echo "Terminal=false" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	echo "Type=Application" >> $AUTOSTART_PATH/$AUTOSTART_FILE
	echo "Categories=" >> $AUTOSTART_PATH/$AUTOSTART_FILE

	chmod 755 $AUTOSTART_PATH/$AUTOSTART_FILE
	chown $CURRENT_USER:$CURRENT_USER -R $AUTOSTART_PATH
}

rvp_update_crontab() {

	# remove crontab if it has 'rvplayer' in it
	if [ $(expr index "$(crontab -l -u $CURRENT_USER)" "$RVPLAYER") -gt 0 ]
	then
		crontab -r
	fi

}

rvp_start_player() {

	if ps ax | grep -v grep | grep $CHROMIUM > /dev/null
	then
		log "Chromium is already running"
	else
		#set up Chromium preferences
		cd
		mkdir -p $SUDO_HOME/$CONFIG_PATH
		mkdir -p $SUDO_HOME/$PREFERENCES_PATH
		:>"$SUDO_HOME/$CONFIG_PATH/$FIRST_RUN_FILE"
		:>$SUDO_HOME/$PREFERENCES_PATH/$PREFERENCES_FILE
		echo "$PREFERENCES" >> $SUDO_HOME/$PREFERENCES_PATH/$PREFERENCES_FILE
		if [ $SUDO_HOME != $USER_HOME ]
		then
			mkdir -p $USER_HOME/$CONFIG_PATH
			mkdir -p $USER_HOME/$PREFERENCES_PATH
			:>"$USER_HOME/$CONFIG_PATH/$FIRST_RUN_FILE"
			:>$USER_HOME/$PREFERENCES_PATH/$PREFERENCES_FILE
			echo "$PREFERENCES" >> $USER_HOME/$PREFERENCES_PATH/$PREFERENCES_FILE
			chown $CURRENT_USER:$CURRENT_USER -R $USER_HOME/$CONFIG_PATH
			chown $CURRENT_USER:$CURRENT_USER -R $USER_HOME/$PREFERENCES_PATH
		fi
	fi

	#check if cache is running
	rvp_get_response_code $RISE_CACHE_PING_URL
	if [ "$response_code" = "200" ]
	then	
		log "RiseCache is already running"
	else
		#run RiseCache in non-blocking mode (background) and hide output
		#also run it as job, so it won't be killed when terminal is closed 
		echo "java -jar '$INSTALL_PATH/$RISE_CACHE_LINUX/$RISE_CACHE_LINUX.jar' >/dev/null 2>&1 &" | at now
	fi

	#check if player is running
	rvp_get_response_code $RISE_PLAYER_PING_URL
	if [ "$response_code" = "200" ]
	then	
		log "RisePlayer is already running"
	else
		log "Starting Rise Vision Player..."
		rvp_save_display_id
		#run RisePlayer in non-blocking mode (background) and hide output
		#also run it as job, so it won't be killed when terminal is closed 
		echo "export DISPLAY=:0; java -jar '$INSTALL_PATH/$RISE_PLAYER_LINUX.jar' >/dev/null 2>&1 &" | at now
	fi

}

rvp_install_java() {

	# check if RiseCache.jar and RisePlayer.jar are running
	# if not, then either java is not installed
	# or java 1.7 is not set as default

	# give Java 3 seconds to load
	sleep 3

	if ps ax | grep -v grep | grep "java" > /dev/null
	then
		log "java is installed and running"
	else
		echo '***************************************************************************'
		echo '* ATTENTION!!! RisePlayer requires java version 1.7 or later.'
		echo '* To install java open Terminal and run "sudo apt-get install openjdk-7-jre".'
		echo '* To set default java version run "sudo update-alternatives --config java".'
		echo '* Run "rvplayer-installer.sh" again after java is configured.'
		echo '***************************************************************************'

		#show popup if zenity is installed
		if [ -e /usr/bin/zenity ]
		then
			JAVA_WARNING='ATTENTION!!! RisePlayer requires java version 1.7 or later.

To install java open Terminal and run "sudo apt-get install openjdk-7-jre".

To set default java version run "sudo update-alternatives --config java".

Run "rvplayer-installer.sh" again after java is configured.'

			env DISPLAY=:0.0 zenity --warning --text "$JAVA_WARNING"
		fi

	fi

}

rvp_check_current_user() {

	# check if current user executing the installer is root, then exit
	if [ "$(whoami)" != "root" ]
	then
	
		#show popup if zenity is installed
		if [ -e /usr/bin/zenity ]
		then
			USER_WARNING='The Rise Vision Player Installation must be run with Sudo.
Press Okay to quit and then run "sudo ./rvplayer-installer.sh"'

			env DISPLAY=:0.0 zenity --warning --text "$USER_WARNING"
		else
			echo ''
			echo 'The Rise Vision Player Installation must be run with Sudo. "sudo ./rvplayer-installer.sh"'
		fi
		
		exit 0
	else
		#if( grep -q "$CURRENT_USER ALL=NOPASSWD: $INSTALL_PATH/rvplayer" /home/sudoers)
		if [ -f "/etc/sudoers.d/$RISE_PLAYER_LINUX" ]
		then
			log "rvplayer already exist in sudoers"
		else
			echo "$CURRENT_USER ALL=NOPASSWD: $INSTALL_PATH/$RVPLAYER" > "/etc/sudoers.d/$RISE_PLAYER_LINUX"
		fi
	fi
}

rvp_check_linux_version() {

	if ! $SILENT
	then
		# check if current linux version is supported or not, then exit
		SUPPORTEDVERSION=false
		if [ "$OS" = "Ubuntu" ] 
		then
			if [ "$OSVER" = "12.10" ]  ||  [ "$OSVER" = "13.10" ]
			then 
				SUPPORTEDVERSION=true 
			fi
		fi
	
		if [ "$OS" = "rsp" ] 
		then
			SUPPORTEDVERSION=true 
		fi

		if [ $SUPPORTEDVERSION = false ]
		then
			#show popup if zenity is installed
			if [ -e /usr/bin/zenity ]
			then
				USER_WARNING='The Rise Vision Player installation that you are using is not approved for this operating system. Press Yes to continue with the installation or No to abort.'
				log "$USER_WARNING"
				set +e
				env DISPLAY=:0.0 zenity --question --text "$USER_WARNING"
				if [ $? -eq 1 ] ; then
					exit
				fi
				set -e
			else
				echo 'The Rise Vision Player installation that you are using is not approved for this'
				echo 'operating system.'
				read -p 'Enter y to continue with the installation or n to abort (y/n)?' choice
				case "$choice" in 
				y|Y ) echo '';;
				* ) exit;;
				esac
			fi
		fi
	fi
}

rvp_update_playerproperties() {

	if [ "$ARCH" = "64" ] || [ "$RISEOS" = "rsp" ]
	then
		if [ ! -f "$INSTALL_PATH/$RISE_PLAYER_LINUX.properties" ]
		then
			echo "playeros="$RISEOS > $INSTALL_PATH/$RISE_PLAYER_LINUX".properties"
		else
			if(grep -q playeros= $INSTALL_PATH/$RISE_PLAYER_LINUX".properties")
			then
				sed -i "s/.*playeros.*/playeros=$RISEOS/g" $INSTALL_PATH/$RISE_PLAYER_LINUX".properties"
			else
				echo "playeros="$RISEOS >> $INSTALL_PATH/$RISE_PLAYER_LINUX".properties"
			fi
		fi		
	fi	


}

rvp_accept_terms() {

	if [ ! -f $TERMS_FILE ]
	then

		echo 'If you agree with our Terms of Service and Privacy
<a href="http://www.risevision.com/TermsOfServiceAndPrivacy/">http://www.risevision.com/TermsOfServiceAndPrivacy/</a>
please click the checkbox below and proceed with the installation. If you do not agree with our Terms of Service and 
Privacy or do not want to install Rise Vision Player please click Cancel.' > $INSTALL_PATH/license.txt 

		#show popup if zenity is installed
		if [ -e /usr/bin/zenity ]
		then

			env DISPLAY=:0.0 zenity --text-info --height 400 --width 500 --html \
				--title="License" \
				--filename=$INSTALL_PATH/license.txt  \
				--checkbox="I agree with the Terms of Service and Privacy"

			if [ $? -eq 0 ] 
			then
			    log "Terms accepted." #echo -n "" > $TERMS_FILE;;
			else
				log "Terms NOT accepted."
			    	exit
			fi
			set -e
		else
			echo ''
			echo 'By entering Yes you are agreeing to our Terms of Service and Privacy'
			echo '(http://www.risevision.com/TermsOfServiceAndPrivacy/) and '
			echo 'proceeding with the installation of the Rise Vision Player.'
			echo ''
			echo 'If you do not agree with our Terms of Service and Privacy'
			echo 'or do not want to install Rise Vision Player please enter No.'
			echo ''
	
			read -p 'Do you agree with the Terms of Service and Privacy (y/n)?' choice
			case "$choice" in 
			y|Y ) echo "Terms accepted.";; #echo -n "" > $TERMS_FILE;;
			* ) exit;;
			esac
		fi
	fi

}

mkdir -p $INSTALL_PATH

rvp_log_size_check

log "Rise Vision Player Installer ver.$VERSION running with User $USER"

# check if silent
for i
do 
	if [ "$i" = "/S" ]; then SILENT=true; fi
	if [ "$i" = "/C" ]; then CLEAR_CACHE=true; fi
done

if [ ! -f $TERMS_FILE ]
then
	FIRST_TIME=true;
fi

# set to silent if script is not running in terminal (cron in our case)
# this is required for Player 1 upgrade
#if [ ! -t 1 ]; then SILENT=true; fi

if ! $SILENT; then rvp_check_linux_version; fi

if ! $SILENT; then rvp_check_current_user; fi

rvp_fix_display_id

rvp_load_display_id

if [ -f "$INSTALL_PATH/clear_cache" ]
then 
	CLEAR_CACHE=true
	rm -f "$INSTALL_PATH/clear_cache"
fi

# if ! $SILENT; then rvp_accept_terms; fi

if [ "$USER" = "root" ]
then 
	rvp_install_prerequisites_sudo 
else
	rvp_install_prerequisites_check
fi

rvp_get_update

# check for installer upgrade

if [ -n "$VALUE_INSTALLER_VERSION" ] && [ -n "$VALUE_INSTALLER_URL" ] && [ "$VALUE_INSTALLER_VERSION" != "$VERSION" ]
then 
	rvp_download_and_run_installer
else
	log "Installer is up to date."
fi

rvp_install_script

rvp_update_playerproperties

rm -rf $TEMP_PATH/$CHROME_LINUX

upgrade_needed=$VALUE_NO

# check for Chromium upgrade

if [ "$USER" != "root" ] && [ "$OS" = "Ubuntu1" ] && [ "$ARCH" = "32" ] && [ "$VALUE_CHROMIUM_VERSION" \> "29.0.1508.0" ]
	then
		log 'Chromium version > 29.0.1508.0 requires the Rise Vision Player to be run with sudo'
		log 'Defaulting chromium version to 29.0.1508.0'
		#VALUE_CHROMIUM_VERSION='29.0.1508.0'
		VALUE_CHROMIUM_URL='http://commondatastorage.googleapis.com/chromium-linux/chrome-linux-29.0.1508.0.zip'
	fi

if [ -n "$VALUE_CHROMIUM_VERSION" ] && [ -n "$VALUE_CHROMIUM_URL" ] && [ "$VALUE_CHROMIUM_VERSION" != "$CURRENT_CHROMIUM_VERSION" ]
then
	rvp_download_and_unpack $VALUE_CHROMIUM_URL $CHROME_LINUX $TEMP_PATH
	echo $VALUE_CHROMIUM_VERSION > $INSTALL_PATH/$TYPE_CHROMIUM".ver"
	upgrade_needed=$VALUE_YES
	rvp_reset_chromium

elif $CLEAR_CACHE
then

	rvp_reset_chromium
else

	log "Chromium is up to date."
fi

# check for RisePlayer upgrade

if [ -n "$VALUE_RISE_PLAYER_VERSION" ] && [ -n "$VALUE_RISE_PLAYER_URL" ] && [ "$VALUE_RISE_PLAYER_VERSION" != "$CURRENT_RISE_PLAYER_VERSION" ]
then

	rvp_download_and_unpack $VALUE_RISE_PLAYER_URL $RISE_PLAYER_LINUX $TEMP_PATH
	echo $VALUE_RISE_PLAYER_VERSION > $INSTALL_PATH/$TYPE_RISE_PLAYER".ver"
	upgrade_needed=$VALUE_YES
else
	log "RisePlayer is up to date."
fi

# check for RiseCache upgrade

if [ -n "$VALUE_RISE_CACHE_VERSION" ] && [ -n "$VALUE_RISE_CACHE_URL" ] && [ "$VALUE_RISE_CACHE_VERSION" != "$CURRENT_RISE_CACHE_VERSION" ]
then

	rvp_download_and_unpack $VALUE_RISE_CACHE_URL $RISE_CACHE_LINUX $TEMP_PATH/$RISE_CACHE_LINUX
	echo $VALUE_RISE_CACHE_VERSION > $INSTALL_PATH/$TYPE_RISE_CACHE".ver"
	upgrade_needed=$VALUE_YES
else

	log "RiseCache is up to date."
fi

# always close Player

	rvp_kill_rise_player
	rvp_kill_chromium
	rvp_kill_rise_cache
	sleep 3

# install upgrades if necessary

if [ $upgrade_needed = $VALUE_YES ] && ([ -d $TEMP_PATH/$CHROME_LINUX ] || [ -f "$TEMP_PATH/$RISE_PLAYER_LINUX.jar" ] || [ -d $TEMP_PATH/$RISE_CACHE_LINUX ])
then 
	
	log "Installing updates..."

	rvp_install_updates

	log "Updates installed."
fi

if [ "$USER" = "root" ]; then rvp_chromium_hacks; fi
	
rvp_chromium_master_preferences

rvp_update_crontab

rvp_confirm_add_player_to_autostart

rvp_start_player

chown $CURRENT_USER:$CURRENT_USER -R $INSTALL_PATH


#rvp_install_java

