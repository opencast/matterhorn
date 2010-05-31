#!/bin/bash

###################################################
# Configure capture agent for use with Matterhorn #
###################################################

# Variables section ####################################################################################################################

# General variables

MAX_PASSWD_ATTEMPTS=3                                       # Maximum number of attempts to stablish the matterhorn user password
CA_SUBDIR=capture-agent                                     # Name for the directory where the matterhorn-related files will be stored

export USERNAME=matterhorn                                  # Default name for the matterhorn user 

export START_PATH=$PWD                                      # Path from where this script is run initially
export WORKING_DIR=/tmp/cainstallscript                     # Directory where this script will be run

export SVN_URL=http://opencast.jira.com/svn/MH              # Root for the source code repository
export TRUNK_EXT=trunk                                      # Extension for the SVN_URL to reach the trunk
export BRANCHES_EXT=branches                                # Extension for the SVN_URL to reach the branches
export TAGS_EXT=tags                                        # Extension for the SVN_URL to reach the tags
export SRC_SUBDIR=matterhorn-source                         # Subdir under the selected user $HOME directory
export SRC_DEFAULT=$TRUNK_EXT                               # Default branch or tag (including trunk) from where scripts and java source will be dowloaded

export OC_DIR=/opt/matterhorn                               # Storage directory for the CA
export DEV_RULES=/etc/udev/rules.d/matterhorn.rules         # File containing the rules to be applied by udev to the configured devices -- not a pun!
export CONFIG_SCRIPT=device_config.sh                       # File name for the bash script under HOME containing the device configuration routine
export DEFAULT_INGEST_URL=http://localhost:8080             # Default value for the core url
export VGA2USB_DIR=epiphan_driver                           # Subdirectory under HOME where the epiphan driver will be downloaded to
export SRC_LIST=/etc/apt/sources.list                       # Location of the file 'sources.list'
export BKP_SUFFIX=backup                                    # Suffix to be appended to the backup file for sources.list
export STARTUP_SCRIPT=/etc/init/matterhorn.conf             # Path of the script which is set up to configure and run felix upon startup
export DEFAULT_MIRROR=http://archive.ubuntu.com/ubuntu      # URL of the default Ubuntu mirror where the packages will be downloaded from
export DEFAULT_SECURITY=http://security.ubuntu.com/ubuntu   # URL of the default Ubuntu 'security' mirror
export DEFAULT_PARTNER=http://archive.canonical.com/ubuntu  # URL of the default Ubuntu 'partner' mirror

export INSTALL_RUN=true                                     # The subsidiary scripts will check for this variable to check they are being run from here


# Third-party dependencies variables
export PKG_LIST="alsa-utils v4l-conf ivtv-utils curl maven2 sun-java6-jdk subversion wget openssh-server gcc gstreamer0.10-alsa gstreamer0.10-plugins-bad gstreamer0.10-plugins-bad-multiverse gstreamer0.10-plugins-base gstreamer0.10-plugins-good gstreamer0.10-plugins-ugly gstreamer0.10-plugins-ugly-multiverse gstreamer0.10-ffmpeg ntp acpid"

export DEFAULT_FLAVOR=0                                                  # 0-based index default option for the device flavor
export FLAVORS="presenter/source presentation/source audience/source indefinite/source"
                                                                         # Lists of flavors the user can choose from to assign to a certain device

export EPIPHAN_URL=http://www.epiphan.com/downloads/linux                # URL to download the epiphan driver
                                                                         # List of required packages
export FELIX_FILENAME=org.apache.felix.main.distribution-2.0.4.tar.gz
export FELIX_URL=http://archive.apache.org/dist/felix/$FELIX_FILENAME
export FELIX_SUBDIR=felix-framework-2.0.4                                # Subdir under the user home where FELIX_HOME is
export FELIX_GENCONF_SUFFIX=conf/config.properties                       # Path under FELIX_HOME where the general matterhorn configuration
export FELIX_PROPS_SUFFIX=conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties
                                                                         # Path under FELIX_HOME where the capture agent properties are
export JAVA_PREFIX=/usr/lib/jvm                                          # Path to where the installed jvm's are
export JAVA_PATTERN=java-6-sun                                           # A regexp to filter the right jvm directory from among all the installed ones
                                                                         # The chosen JAVA_HOME will be $JAVA_PREFIX/`ls $JAVA_PREFIX | grep $JAVA_PATTERN`

export M2_SUFFIX=.m2/repository                                          # Path to the maven2 repository, under the user home

export DEFAULT_NTP_SERVER=ntp.ubuntu.com                                 # Default ntp server
export NTP_CONF=/etc/ntp.conf                                            # Location for the ntp configuration files

export JV4LINFO_URL=http://luniks.net/luniksnet/download/java/jv4linfo   # Location of the jv4linfo jar
export JV4LINFO_JAR=jv4linfo-0.2.1-src.jar                               # Name of the jv4linfo file
export JV4LINFO_LIB=libjv4linfo.so                                       # Shared object required by the jv4linfo jar to function
export JV4LINFO_PATH=/usr/lib                                            # Location where the shared object will be copied so that jvm can find it
                                                                         # In other words, it must be in the java.library.path

# Required scripts for installation
SETUP_USER=./setup_user.sh
INSTALL_VGA2USB=./install_vga2usb_drivers.sh
SETUP_DEVICES=./setup_devices.sh
INSTALL_DEPENDENCIES=./install_dependencies.sh
SETUP_SOURCE=./setup_source.sh
SETUP_ENVIROMENT=./setup_enviroment.sh
SETUP_BOOT=./setup_boot.sh
export CLEANUP=./cleanup.sh                # This variable is exported because the script is modified by another

SCRIPTS=( "$SETUP_USER" "$INSTALL_VGA2USB" "$SETUP_DEVICES" "$INSTALL_DEPENDENCIES" "$SETUP_ENVIROMENT" "$SETUP_SOURCE" "$SETUP_BOOT" "$CLEANUP" )
SCRIPTS_EXT=docs/scripts/ubuntu_capture_agent

# End of variables section########################################################################################



# Checks if this script is being run with root privileges, exiting if it doesn't
if [[ `id -u` -ne 0 ]]; then
    echo "This script requires root privileges. Please run it with the sudo command or log in to the root user and try again"
    exit 1
fi

# Change the working directory to a temp directory under /tmp
# Deletes it first, in case it existed previously (MH-3797)
rm -rf $WORKING_DIR
mkdir -p $WORKING_DIR
cd $WORKING_DIR

# If wget isn't installed, get it from the ubuntu software repo
wget foo &> /dev/null
if [ $? -eq 127 ]; then
    apt-get -y --force-yes install wget &>/dev/null
    if [ $? -ne 0 ]; then
	echo "Couldn't install the necessary command 'wget'. Please try to install it manually and re-run this script"
	exit 1
    fi
fi

# Check for the necessary scripts and download them from the svn location
# Using C-like syntax in case file names have whitespaces
for (( i = 0; i < ${#SCRIPTS[@]}; i++ )); do
    f=${SCRIPTS[$i]}
	# Check if the script is in the directory where the install.sh script was launched
	if [[ -e $START_PATH/$f ]]; then
	    # ... and copies it to the working directory
	    cp $START_PATH/$f $WORKING_DIR
	else
	    # The script is not in the initial directory, so try to download it from the opencast source page
	    wget $SVN_URL/$SRC_DEFAULT/$SCRIPTS_EXT/$f &> /dev/null	    
	    # Check the file is downloaded
	    if [[ $? -ne 0 ]]; then
		echo "Couldn't retrieve the script $f from the repository. Try to download it manually and re-run this script."
		exit 2
	    fi
	fi  
    chmod +x $f
done

# Choose/create the matterhorn user (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
. ${SETUP_USER}

# Install the 3rd party dependencies (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
. ${INSTALL_DEPENDENCIES}
if [[ "$?" -ne 0 ]]; then
    echo "Error installing the 3rd party dependencies."
    exit 1
fi

# Install the vga2usb driver
${INSTALL_VGA2USB}
if [[ "$?" -ne 0 ]]; then
    echo "Error installing the vga2usb driver."
    exit 1
fi

# Set up the matterhorn code --doesn't build yet!
${SETUP_SOURCE}
if [[ "$?" -ne 0 ]]; then
    echo "Error setting up the matterhorn code. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi

# Setup properties of the devices
${SETUP_DEVICES}
if [[ "$?" -ne 0 ]]; then
    echo "Error setting up the capture devices. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi

# Set up user enviroment
${SETUP_ENVIROMENT}
if [[ "$?" -ne 0 ]]; then
    echo "Error setting up the enviroment for $USERNAME. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi

# Build matterhorn
echo -e "\n\nProceeding to build the capture agent source. This may take a long time. Press any key to continue...\n\n"
read -n 1 -s

cd $SOURCE
su matterhorn -c "mvn clean install -Pcapture -DdeployTo=\${FELIX_HOME}/load/bundles"
if [[ "$?" -ne 0 ]]; then
    echo -e "\nError building the matterhorn code. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi
cd $WORKING_DIR

# Set up the file to run matterhorn automatically on startup
${SETUP_BOOT}

echo -e "\n\n\nCapture Agent succesfully installed\n\n\n"

read -p "It is recommended to reboot the system after installation. Do you wish to do it now [Y/n]? " response

while [[ -z "$(echo ${response:-Y} | grep -i '^[yn]')" ]]; do
    read -p "Please enter [Y]es or [n]o: " response
done

if [[ -n "$(echo ${response:-Y} | grep -i '^y')" ]]; then
    echo "Rebooting... "
    reboot > /dev/null
else
    echo -e "\n\nThe capture agent will start automatically after rebooting the system."
    echo "However, you can start it manually by running ${FELIX_HOME}/bin/start_matterhorn.sh"
    echo "Please direct your questions / suggestions / etc. to the list: matterhorn@opencastproject.org"
    echo
    read -n 1 -s -p "Hit any key to exit..."
    clear

fi
