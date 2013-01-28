#!/bin/sh
cd data
./generate_db.sh
cd ..
ant debug
if [ $? -eq 0 ]
then
    adb install -r bin/KlingonAssistant-debug.apk
else
    echo ERROR: Failed to compile.
fi
