#!/bin/bash

# Version de l'image de base. Décorellé de la version applicative, n'évolue pas souvent.
CHOUETTE_BASE_VERSION=1.6

MVN_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
MVN_VERSION=`echo ${MVN_VERSION} | sed -r "s/\x1B\[([0-9]{1,3}(;[0-9]{1,2};?)?)?[mGK]//g"`

#mvn  install -DskipTests -DskipWildfly=true -DskipInitDb=true
docker build -t registry.okina.fr/mobiiti/chouette:${MVN_VERSION} -f docker/Dockerfile-build --build-arg CHOUETTE_BASE_VERSION=registry.okina.fr/mobiiti/chouette-base:${CHOUETTE_BASE_VERSION} .
docker push registry.okina.fr/mobiiti/chouette:${MVN_VERSION}
