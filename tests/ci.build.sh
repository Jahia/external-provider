#!/bin/bash
# This script can be used to manually build the docker images necessary to run the tests
# It should be executed from the tests folder

echo " ci.build.sh == Build test image"

source ./set-env.sh

# It assumes that you previously built the module you're going to be testing
#   and that the modules artifacts are located one level up

if [ ! -d ./artifacts ]; then
  mkdir -p ./artifacts
fi

if [[ -e ../core/target ]]; then
  cp ../core/target/*-SNAPSHOT.jar ./artifacts/
fi

if [[ -e ../vfs/target ]]; then
  cp ../vfs/target/*-SNAPSHOT.jar ./artifacts/
fi

if [[ -e ../modules-provider/target ]]; then
  cp ../modules-provider/target/*-SNAPSHOT.jar ./artifacts/
fi

docker build -t ${TESTS_IMAGE} .
