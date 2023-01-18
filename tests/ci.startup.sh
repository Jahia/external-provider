#!/usr/bin/env bash

# This script controls the startup of the container environment
# It can be used as an alternative to having docker-compose up started by the CI environment

source ./set-env.sh


echo " ci.startup.sh == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " JAHIA_CLUSTER_ENABLED: ${JAHIA_CLUSTER_ENABLED}"

echo "$(date +'%d %B %Y - %k:%M') [LICENSE] == Check if license exists in env variable (JAHIA_LICENSE) =="
if [[ -z ${JAHIA_LICENSE} ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [LICENSE] == Jahia license does not exist, checking if there is a license file in /tmp/license.xml =="
    if [[ -f /tmp/license.xml ]]; then
        echo "$(date +'%d %B %Y - %k:%M') [LICENSE] ==  License found in /tmp/license.xml, base64ing it"
        export JAHIA_LICENSE=$(base64 /tmp/license.xml)
    else
        echo "$(date +'%d %B %Y - %k:%M') [LICENSE]  == STARTUP FAILURE, unable to find license =="
        exit 1
    fi
fi

echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Value: ${JAHIA_CLUSTER_ENABLED} =="
if [[ "${JAHIA_CLUSTER_ENABLED}" == "true" ]]; then
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Starting a cluster of one processing and two browsing =="
    if [[ $1 == "notests" ]]; then
        docker-compose up -d --renew-anon-volumes mariadb jahia jahia-browsing-a jahia-browsing-b
    else
        docker-compose up --abort-on-container-exit --renew-anon-volumes cypress mariadb jahia jahia-browsing-a jahia-browsing-b
    fi
else
    echo "$(date +'%d %B %Y - %k:%M') [JAHIA_CLUSTER_ENABLED] == Starting a single processing node (no cluster) =="
    if [[ $1 == "notests" ]]; then
        docker-compose up -d --renew-anon-volumes mariadb jahia
    else
        docker-compose up --renew-anon-volumes -d mariadb jahia
        docker ps -a
        docker stats --no-stream
        docker-compose up --abort-on-container-exit cypress
    fi
fi
