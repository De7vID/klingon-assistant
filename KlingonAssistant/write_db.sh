#!/bin/bash

cd data
./renumber.py
./generate_db.sh --noninteractive
cd ..
