#!/bin/bash

# Version de l'image de base. Décorellé de la version applicative, n'évolue pas souvent.
CHOUETTE_BASE_VERSION=1.6

docker build --no-cache -t registry.okina.fr/mobiiti/chouette-base:${CHOUETTE_BASE_VERSION} -f docker/Dockerfile-base .
docker push registry.okina.fr/mobiiti/chouette-base:${CHOUETTE_BASE_VERSION}
#docker buildx build --platform linux/amd64,linux/arm64 -t registry.okina.fr/mobiiti/chouette-base:${CHOUETTE_BASE_VERSION} -f docker/Dockerfile-base --push .
