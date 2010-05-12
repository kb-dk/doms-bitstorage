#!/bin/bash

set -eu

. $(dirname $0)/setenv.sh

sum_one_dir() {
    S=$MD5SUMDIR/$(echo $1| tr / _).md5s
    (
	cd $1
	ls
	if [ -f $S ] ; then
	    sortmd5s.py < $S | sed 's/ [^ ]* [^ ]*$//'
	fi
    ) | sort -u |
    while read F ; do
	make_sum.sh "$1/./$F"
    done
    if [ -f $S ] ; then
	L=$S.lock
	lockfile -l 120 $L
	sortmd5s.py < $S > $S.new
	mv $S.new $S
	rm -f $L
    fi
}
LOCKFILE=$MD5SUMDIR/resum_files.lock
# 604800 = 1 week
if lockfile -! -l 604800 $LOCKFILE ; then
    exit
fi
for DIR in $STORAGE_DIRS ; do
    sum_one_dir $DIR
    touch $LOCKFILE
done
rm -f $LOCKFILE
