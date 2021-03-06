#!/bin/bash
# Copy/pasted from https://github.com/scala-native/scala-native 

set -e

AUTHOR=$GITHUB_ACTOR
echo "Pull request submitted by $AUTHOR";
signed=$(curl -s https://www.lightbend.com/contribute/cla/scala/check/$AUTHOR | jq -r ".signed");
if [ "$signed" = "true" ] ; then
  echo "CLA check for $AUTHOR successful";
else
  echo "CLA check for $AUTHOR failed";
  echo "Please sign the Scala CLA to contribute to Scala Native";
  echo "Go to https://www.lightbend.com/contribute/cla/scala and then";
  echo "comment on the pull request to ask for a new check.";
  exit 1;
fi;
