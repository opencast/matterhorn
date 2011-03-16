#!/bin/bash
function usage {
    echo \
"upload.sh -d directory [-u username][-p password][-s server url][-t time][-h]

    -d directory
        The directory that you want to upload your files from
        generated by 'convert.sh'
    -s server url
        the URL of the server you want to upload to
        defaults to http://localhost:8080/
    -u username
        the username required to log into the server
        defaults to 'admin'
    -p password
        the password required to log into the server
        defaults to 'opencast'
    -t time 
        the time between posting media to the server
        defaults to '3m' or 3 minutes
    -h
        displays this help message
";
    
}

server="http://localhost:8080/"
directory="";
username="admin"
password="opencast"
delay="3m"
while getopts “hu:p:s:d:” OPTION
do
	case $OPTION in
	h)
	  usage
	  exit 1
	  ;;
	s)
      server=$OPTARG
	  ;;
	d)
	  directory=$OPTARG
	  ;;
	u)
	  username=$OPTARG
	  ;;
	u)
	  password=$OPTARG
	  ;;
    t)
      delay=$OPTARG
      ;;
	?)
	  usage
	  exit
	  ;;
	esac
done

if [[ $server != */ ]] 
then
    server=$server"/"  
fi 

curl -c cookies.txt $server"ingest/addMediaPackage" \
    -H "X-Requested-Auth: Digest" \
    --digest --user $username:$password \

if [ -d $directory ]
then
    cd $directory 
    date=(date +"%B %e %Y")
    for i in * 
    do
        echo "ingesting $i into $server"
        if [[ $i == s-* ]] 
        then
            curl -v -o /dev/null -b ../cookies.txt $server"ingest/addMediaPackage" \
             	--form-string "title=$i" \
	            --form-string "creator=TestScript" \
	            --form-string "flavor=presentation/source" \
	            --form track=@$i;
        else
            curl -v -o /dev/null -b ../cookies.txt $server"ingest/addMediaPackage" \
             	--form-string "title=$i" \
	            --form-string "creator=TestScript" \
	            --form-string "flavor=presenter/source" \
	            --form track=@$i;
        fi
        sleep $delay
    done
    cd ..
fi
rm cookies.txt


    




