#!/bin/bash

# Generate the database.
cd data
./generate_db.sh
cd ..

# Build the debug build.
ant debug
if [ $? -eq 0 ]
then
    # Pause before pushing.
    read -n1 -r -p "Press any key to push to device..."
    echo

    # Push to device.
    adb install -r bin/KlingonAssistant-debug.apk
    adb push bin/KlingonAssistant-debug.apk /sdcard/
else
    echo ERROR: Failed to compile.
fi
