#!/bin/bash

MVN_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
MVN_VERSION=$(echo ${MVN_VERSION} | sed -r "s/\x1B\[([0-9]{1,3}(;[0-9]{1,2};?)?)?[mGK]//g")

#mvn  install -DskipTests -DskipWildfly=true -DskipInitDb=true
docker build -t registry.okina.fr/mobiiti/chouette:${MVN_VERSION} -f docker/Dockerfile-build .
docker push registry.okina.fr/mobiiti/chouette:${MVN_VERSION}
