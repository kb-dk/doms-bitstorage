#!/bin/bash

set -eu

. $(dirname $(readlink -f $0))/setenv.sh
DIR=${1%%/./*}
BASE=${1#*/./}
SUMFILE="$MD5SUMDIR/$(echo $DIR| tr / _).md5s"
LOCKFILE="$SUMFILE.lock"
lockfile -l 120 "$LOCKFILE"
echo "$BASE $2 $(date --iso-8601)" >> $SUMFILE
rm -f "$LOCKFILE"
