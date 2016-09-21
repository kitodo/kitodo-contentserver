#!/bin/bash
## intranda compressImages script
##

for dir in $(find $1 -type d); do
	#echo $dir
	for file in $dir/*.jpg; do
		echo "Reading file $file"
 		if [ "$(identify -verbose $file | grep exif: >> /dev/null; echo $?)" == "0" ]; then 
			echo "Removing header of file $file"
			mogrify $file -strip -quiet
 		fi
	done
done
