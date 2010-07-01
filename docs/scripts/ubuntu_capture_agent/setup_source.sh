#! /bin/bash

###############################
# Setup Matterhorn from trunk #
###############################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

. ${FUNCTIONS}

# Detect if the matterhorn source has been already checked out
url=$(svn info $SOURCE 2> /dev/null | grep URL: | cut -d ' ' -f 2) 
if [[ "$url" ]]; then
    echo
    yesno -d yes "The source $url has been already checked out. Do you wish to keep it?" keep
else
    keep=
fi

if [[ -z "$keep" ]]; then
    # Get the necessary matterhorn source code (the whole trunk, as specified in MH-3211)
    while [[ true ]]; do
	echo
	ask -d "${SRC_DEFAULT##*/}" "Enter the branch or tag you would like to download" response

	if [[ "$response" == "${TRUNK_URL##*/}" ]]; then
	    url=$TRUNK_URL
	else
	    # Check the branches first
	    url=$BRANCHES_URL/$response
	    svn info $url &> /dev/null
	    # If $url does not exist, try the tags
	    [[ $? -ne 0 ]] && url=$TAGS_URL/$response
	fi

	rm -rf $SOURCE
	echo -n "Attempting to download matterhorn source from $url... "
	svn co --force $url $SOURCE

	if [[ $? -eq 0 ]]; then
	    #### Exit the loop ####
	    break
	fi

	## Error. The loop repeats
	echo "Error!"
	echo "Couldn't check out the matterhorn code. Is the URL correct?"
    done
    echo "Done"
fi

# Log the URL downloaded -or already present-
echo >> $LOG_FILE
echo "# Source code URL" >> $LOG_FILE
echo "$url" >> $LOG_FILE

# Setup felix configuration
echo -n "Applying matterhorn configuration files to felix... "
cp -rf $SOURCE/$FELIX_DOCS/* ${FELIX_HOME}
# Remove the .svn folders
find ${FELIX_HOME} -name "\.svn" -exec rm -rf {} \; 2> /dev/null

echo "Done"
