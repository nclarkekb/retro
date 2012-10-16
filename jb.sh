#!/bin/sh
mvn -U clean install
cd target
unzip retro-*.zip
cd ..
