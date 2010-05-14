#!/bin/bash

#DEBUG=1
if [ "$DEBUG" ] ; then
    echo Content-type: text/plain
    echo
    env | sort
    exec 2>&1
    set -x
fi
DIR=$(dirname $(readlink -f $0))
FN=${REQUEST_URI:${#SCRIPT_NAME}}
export FN=${FN:1}
FN=$(python -c "import urllib, os; print urllib.unquote_plus(os.environ['FN'])")
CT=application/octet-stream
case "$FN" in
    *.txt)  CT=text/plain ;;
esac
case $REMOTE_ADDR in
    127.0.0.1        | \
    130.225.2[456]*  | \
    172.18.* )
	APPROVED=
	;;
    *)
	APPROVED=-approved
	;;
esac

if $DIR/server.sh exists$APPROVED "$FN" ; then
    echo Content-type: $CT
    echo
    $DIR/server.sh get$APPROVED "$FN"
else
    echo Content-type: text/plain
    echo Status: 404 File not found
    echo
    echo File not found
fi


