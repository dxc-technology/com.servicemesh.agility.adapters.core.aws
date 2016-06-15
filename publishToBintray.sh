#!/bin/bash

echo "Uploading artifacts to bintray at https://bintray.com/csc/opensource/com.servicemesh.agility.adapters.core.aws"

cd dist

shopt -s nullglob
for file in *;
do
   curl -T $file -u$2:$3 "https://api.bintray.com/content/csc/opensource/com.servicemesh.agility.adapters.core.aws/$1/$file;publish=1;override=1;bt_package=com.servicemesh.agility.adapters.core.aws;bt_version=$1"
done


