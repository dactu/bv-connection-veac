#!/bin/bash
source ./VERSION
echo $VERSION

DEB_FOLDER=deb/$D_APP-$D_ARC-$D_VERSION
DEB_DEBIAN_FOLDER=$DEB_FOLDER/DEBIAN
DEB_BIN_FOLDER=$DEB_FOLDER/usr/local/bin/bv-connect-bv5
DEB_CFG_FOLDER=$DEB_FOLDER/etc/bv-connect-bv5
DEB_SVC_FOLDER=$DEB_FOLDER/lib/systemd/system
AUTH=$(git log -1 --pretty=format:"%ae")
current_date=$(date '+%Y-%m-%d %H:%M:%S')

echo $DEB_FOLDER
echo $DEB_BIN_FOLDER
echo $DEB_CFG_FOLDER

mkdir -p $DEB_FOLDER
mkdir -p $DEB_BIN_FOLDER
mkdir -p $DEB_BIN_FOLDER/logs
mkdir -p $DEB_CFG_FOLDER
mkdir -p $DEB_SVC_FOLDER
mkdir -p $DEB_DEBIAN_FOLDER

echo "Package: $D_APP" > $DEB_DEBIAN_FOLDER/control
echo "Version: $D_VERSION" >> $DEB_DEBIAN_FOLDER/control
echo "Architecture: $D_ARC" >> $DEB_DEBIAN_FOLDER/control
echo "Maintainer: $AUTH" >> $DEB_DEBIAN_FOLDER/control
echo "Description:  bv-connect-tsl-version $D_VERSION at $current_date">> $DEB_DEBIAN_FOLDER/control

cp -vf run.sh $DEB_BIN_FOLDER/run.sh
cp -vf target/BMSTrainingConnection.jar $DEB_BIN_FOLDER/BMSTrainingConnection.jar
cp -vf src/main/resources/application.properties $DEB_CFG_FOLDER/application.properties
cp -vf deb/template.service $DEB_SVC_FOLDER/$D_APP.service

sed -i "s|SVC_DESCRIPTION| bv-connect-bv5-version $D_VERSION|g" $DEB_SVC_FOLDER/$D_APP.service
dpkg-deb --build --root-owner-group $DEB_FOLDER
curl -u "chdk:123456aA@" -H "Content-Type: multipart/form-data" --data-binary "@./$DEB_FOLDER.deb" "http://172.31.252.188:8081/repository/chdk-apt-hosted/"
