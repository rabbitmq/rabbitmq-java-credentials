#!/usr/bin/env bash

set -euo pipefail

source ./release-versions.txt
git checkout "$RELEASE_BRANCH"

./mvnw release:clean release:prepare -DdryRun=true -Darguments="-DskipTests" --no-transfer-progress \
  --batch-mode -Dtag="v$RELEASE_VERSION" \
  -DreleaseVersion="$RELEASE_VERSION" \
  -DdevelopmentVersion="$DEVELOPMENT_VERSION"

./mvnw release:clean release:prepare -Darguments="-DskipTests" --no-transfer-progress \
  --batch-mode -Dtag="v$RELEASE_VERSION" \
  -DreleaseVersion="$RELEASE_VERSION" \
  -DdevelopmentVersion="$DEVELOPMENT_VERSION"

git checkout "v$RELEASE_VERSION"

./mvnw clean deploy -Ppublish -DskipTests --no-transfer-progress
