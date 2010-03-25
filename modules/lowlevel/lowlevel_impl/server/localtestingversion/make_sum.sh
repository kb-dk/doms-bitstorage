#!/bin/bash

set -eu

. $(dirname $0)/setenv.sh

if [ -f "$1" ] ; then
    MD5=$(md5sum "$1")
    MD5=${MD5%% *}
else
    MD5=DELETED
fi
register_sum.sh "$1" $MD5
