#!/usr/bin/env sh
# Gradle wrapper shell script
if [ "$DEBUG" = "true" ]; then
  set -x
fi
DIR=$(dirname "$0")
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$DIR/.gradle}"
export GRADLE_USER_HOME
exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
