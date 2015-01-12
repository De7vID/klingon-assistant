#!/bin/sh
patch -p1 < KlingonAssistant/unpatch_tts.txt
cd KlingonAssistant
sed -i "s/\(versionName=.*\) (TTS)/\1/" AndroidManifest.xml
ant clean
