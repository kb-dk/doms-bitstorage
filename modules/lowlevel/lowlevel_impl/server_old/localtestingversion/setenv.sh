
BIN=$(dirname $(readlink -f $0) )
PATH=/usr/bin:/bin:$BIN
ROOTDIR=~/Servers/testbitstorage
STAGE_DIR=$ROOTDIR/stage
STORAGE_DIRS=$ROOTDIR/storage
MD5SUMDIR=$ROOTDIR/md5sums
WEBDIR=$ROOTDIR/web
mkdir -p $MD5SUMDIR $STORAGE_DIRS $STAGE_DIR $WEBDIR

