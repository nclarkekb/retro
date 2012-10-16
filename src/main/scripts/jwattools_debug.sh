#!/bin/sh
ProgDir=`dirname "$0"`
. "${ProgDir}/env.sh"

if [ -z "${JAVA_DEBUG_OPTS}" ]; then
  JAVA_DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044"
fi

if [ -z "${JAVA_OPTS}" ]; then
  JAVA_OPTS="-Xms256m -Xmx1024m -XX:PermSize=64M -XX:MaxPermSize=256M"
fi

"${JAVA}" ${JAVA_DEBUG_OPTS} ${JAVA_OPTS} -cp "$CP" ${assembly.main.class.name} $@
