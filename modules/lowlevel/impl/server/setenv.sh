
BIN=$(dirname $(readlink -f $0) )
PATH=/usr/bin:/bin:$BIN
STAGE_DIR=~/stage
STORAGE_DIRS=~/storage
MD5SUMDIR=~/md5sums

if [ $HOSTNAME = jhlj-linux ] ; then
    STAGE_DIR=/tmp/stage
    STORAGE_DIRS=$(echo /tmp/storage{1,2})
    MD5SUMDIR=/tmp/md5
    mkdir -p $MD5SUMDIR $STORAGE_DIRS $STAGE_DIR
fi
