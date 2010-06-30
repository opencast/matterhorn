#! /bin/bash

##########################################################################################
# Tries to guess the most suitable vga2usb driver for the current system and installs it #
##########################################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Include the functions script
. ${FUNCTIONS}

if [[ -z "$(lsmod | grep -e "^vga2usb")" ]]; then
  
    FILE_NAME=driver_list
    
    while [[ true ]]; do
	wget -q -O $FILE_NAME $EPIPHAN_URL
	if [[ $? -eq 0 ]]; then
            # Kernel base consists of the first three numbers in the kernel version, which are the most relevant
	    kernel_base=$(uname -r | grep -o "[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]")
	    architecture=$(uname -m)
	    
            # Gets the complete list of drivers in the page compatible with the current kernel
	    # It makes no sense presenting others which are not compatible
            # First trY to filter by architecture type
	    drivers=( $(grep "vga2usb" $FILE_NAME | sed 's#^.*<a\s*href="\(.*\)".*>.*</a>.*$#\1#' | grep "$kernel_base" | grep -i $architecture ))
            # If there's no match, be less strict and not filter by architecture
	    if [[ ${#drivers[@]} -eq 0 ]]; then
		drivers=( $(grep "vga2usb" $FILE_NAME | sed 's#^.*<a\s*href="\(.*\)".*>.*</a>.*$#\1#' | grep "$kernel_base" ))
	    fi
	    break;
	fi

	echo "The list of available vga2usb drivers could not be retrieved. Please check there is a working internet connection"
	yesno -d yes "Do you wish to retry?" ok
	if [[ ! "$ok" ]]; then
	    break
	fi
    done

    # Let user choose driver
    # Main loop: if some error happens, the user will be prompted again for another option
    while [[ true ]]; do
	echo
	echo "System information: $(uname -mor)"
	echo
	
	choose -t "These are the options available for your system" -? "$VGA2USB_HELP_MSG" -o list -- ${drivers[@]} "Enter URL" "Do not need driver" opt
	if [[ $opt -le ${#drivers[@]} ]]; then
	    # The option is one of the drivers or "Enter URL"
 	    if [[ $opt -eq ${#drivers[@]} ]]; then
	        # The option is "Enter URL" -- ask the user for the driver url
		unset DRIVER_URL
		ask -? "$HELP_MSG" "Please input the URL of the driver you would like to load" DRIVER_URL
	  
		# Keeps whatever it is after the last / --the file name
  	        EPIPHAN=${DRIVER_URL##*/}
	    else
		# Download the driver from the epiphan page
  		DRIVER_URL="$EPIPHAN_URL/${drivers[$opt]}"
  		EPIPHAN="${drivers[$opt]}"
	    fi
	    
	    # Attempt to load the vga2usb driver
	    echo -n "Downloading driver $EPIPHAN... "
	    
	    mkdir -p $CA_DIR/$VGA2USB_DIR
  	    wget -q -P $CA_DIR/$VGA2USB_DIR $DRIVER_URL
	    
  	    if [[ $? -eq 0 ]]; then
		echo -n "Loading driver... "
  		cd $CA_DIR/$VGA2USB_DIR
  		tar jxf $EPIPHAN
  		sed -i '/sudo \/sbin\/insmod/s|$| num_frame_buffers=2|' Makefile
  		# First "make" is necessary according with MH-3810
		make &> /dev/null
  		make load &> /dev/null
  		if [[ $? -ne 0 ]]; then
    		    echo "Error!"
		    echo "Failed to load Epiphan driver. Maybe your machine kernel or architecture were not compatible?"
		    rm -r $(tar jtf $EPIPHAN 2> /dev/null) 2> /dev/null
		    rm $EPIPHAN
  		else
		    echo "Done."
		    ## Loop Exit ##
		    break;
                    ###############
		fi
		cd $WORKING_DIR
  	    else
		echo "Error!"
		echo "Failed to retrieve the driver from the URL. Please check it is correct and there is a working internet connection."
	    fi
	    
	else
	    # Skip driver installation
	    echo "Skipping the vga2usb driver installation. Please note that if no driver is present, the vga2usb card(s) will not be detected."
	    yesno -d no "Are you sure you want to proceed?" proceed
	    if [[ "$proceed" ]]; then
		break
	    fi
	fi
    done
else
    echo "VGA2USB driver already installed."
fi
