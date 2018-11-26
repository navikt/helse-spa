#!/usr/bin/env bash
set -ev

if [ "${TRAVIS_PULL_REQUEST}" = "false" ] && [ "${TRAVIS_BRANCH}" = "master" ]; then
    make release
    git push --tags https://x-access-token:$INSTALLATION_TOKEN@github.com/$TRAVIS_REPO_SLUG.git HEAD:master
fi
