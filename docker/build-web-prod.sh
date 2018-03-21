#!/usr/bin/env bash

# Builds the Web image, for production: includes the latest versions of
# all generated scripts and styles directly in the image.
# (So won't need to be mounted via a Docker-mount-stuff directive.)

# Run from the docker/ parent dir.


set -e # exit on any error.
set -x

rm -fr target/docker-web-prod

# We'll build the prod Web image, in this dir:
cp -a  docker/web  target/docker-web-prod

# Copy all static assets, incl minified scripts and styles, to that dir.
# Also, -L: copy the files symlinks point to, rather than just copying the symlinks.
cp -aL assets      target/docker-web-prod/assets

cd target/docker-web-prod

docker build --tag=debiki/talkyard-web:latest .

echo "Image tag: debiki/talkyard-web:latest"

