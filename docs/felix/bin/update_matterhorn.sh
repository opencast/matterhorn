#!/bin/bash

cd /opt/matterhorn/matterhorn_trunk

# update from svn
if [ -z $1 ]
then
  svn update
else
  svn update -r $1
fi

#stop felix
sudo service matterhorn stop

# Clean old jars
rm -rf /opt/matterhorn/felix/load
rm -rf /opt/matterhorn/felix/matterhorn

export MAVEN_OPTS='-Xms256m -Xmx960m -XX:PermSize=64m -XX:MaxPermSize=150m'

# build matterhorn
mvn clean install -DskipTests -DdeployTo=/opt/matterhorn/felix/matterhorn

# creating backup of configuration
tar -czf /home/opencast/felix-config-backup.tar.gz /opt/matterhorn/felix/conf/ 

# update felix configuration
rm -rf /opt/matterhorn/felix/conf
cp -rf docs/felix/* /opt/matterhorn/felix/
cd /home/opencast

# update felix config (url)
MY_IP=`ifconfig | grep "inet addr:" | grep -v 127.0.0.1 | awk '{print $2}' | cut -d':' -f2`
sed -i "s/http:\/\/localhost:8080/http:\/\/$MY_IP:8080/" /opt/matterhorn/felix/conf/config.properties
sed -i "s/rtmp:\/\/localhost\/matterhorn-engage/rtmp:\/\/$MY_IP\/matterhorn-engage/" /opt/matterhorn/felix/conf/config.properties
sed -i 's/\${org.opencastproject.storage.dir}\/streams/\/opt\/matterhorn\/red5\/webapps\/matterhorn\/streams/' /opt/matterhorn/felix/conf/config.properties
sed -i "s/conf\/security.xml/\/opt\/matterhorn\/felix\/conf\/security.xml/" /opt/matterhorn/felix/conf/config.properties
# update capture properties
sed -i "s/http:\/\/localhost:8080/http:\/\/$MY_IP:8080/" /opencast/config/capture.properties

for i in /opt/matterhorn/felix/load/*.cfg; do sed -i 's/\.\//\/opt\/matterhorn\/felix\//' $i; done

# restart felix
sudo service matterhorn start