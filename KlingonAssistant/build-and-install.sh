#!/bin/sh
cd data
./generate_db.sh
cd ..
ant debug
adb install -r bin/KlingonAssistant-debug.apk
