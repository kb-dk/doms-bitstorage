#!/bin/bash

set -eu

. $(dirname $(readlink -f $0))/setenv.sh


main () {
    if [ -n "${SSH_ORIGINAL_COMMAND:-}" ] ; then
  	    CMD="${SSH_ORIGINAL_COMMAND%% *}"
        ARG="${SSH_ORIGINAL_COMMAND#* }"
    else
	    CMD="${1:-}"
	    if [ -n "$CMD" ] ; then
	        shift
	    fi
	    ARG="$*"
    fi
    TARG=$(echo "$ARG" | tr / \\\\)
    case "$CMD" in
	save-md5) : save a file and get md5sum back
	    if [ -n "$(get_path)"  ] ; then
	    	echo "$ARG was stored!"
		    exit 1
	    fi
	    cat > "$STAGE_DIR/$TARG"
	    MD5=$(md5sum "$STAGE_DIR/$TARG")
	    MD5=${MD5%% *}
	    MD5=${MD5#\\}
	    echo $MD5
	    ;;
	get-md5) : get md5sum of a file
	    if [ -f "$STAGE_DIR/$TARG" ] ; then
		    FILE="$STAGE_DIR/$TARG"
	    else
		    FILE="$(get_path)"
	    fi
	    if [ -z "$FILE" ] ; then
		    exit 1
	    fi
	    MD5=$(md5sum "$FILE")
	    MD5=${MD5%% *}
	    echo $MD5
	    ;;
	approve) : approve a file
	    if [ ! -f "$STAGE_DIR/$TARG" ] ; then
		    echo "$ARG not found"
		    exit 1
	    fi
	    SIZE=$(stat -c %s "$STAGE_DIR/$TARG")
	    # make space for directory entry
	    SIZE=$(($SIZE+4096))
	    for DIR in $STORAGE_DIRS ; do
		    SPACE=$(get_space $DIR)
		    if [ $SPACE -gt $SIZE ] ; then
		        break
		    fi
	    done
	    if [ $SIZE -gt $SPACE ] ; then
		    echo "No space left for file"
		    exit 1
	    fi
	    mkdir -p "$(dirname "$DIR/$ARG")"
	    mv "$STAGE_DIR/$TARG" "$DIR/$ARG"
	    MD5=$(md5sum "$DIR/$ARG")
	    MD5=${MD5%% *}
	    MD5=${MD5#\\}
	    register_sum.sh "$DIR/./$ARG" $MD5
	    echo $MD5
	    ;;
	delete) : delete a file not appoved
	    if [ -f "$STAGE_DIR/$TARG" ] ; then
		    rm -f "$STAGE_DIR/$TARG"
	    else
		    echo "$ARG not found"
		    exit 1
	    fi
	    ;;
	get) : get a file
	    FILE=$(get_path)
	    if [ -n "$FILE" ] ; then
		    cat "$FILE"
	    else
		    exit 1
	    fi
	    ;;
	getmd5s) : report md5 sums of stored files
	    for F in $MD5SUMDIR/*.md5s ; do
		if [ -f "$F" ] ; then
		    sortmd5s.py < $F
		fi
	    done
	    ;;
	space-left) : report space left
	    S_SPACE=$(get_space $STAGE_DIR)
	    SUM=0
	    MAX=0
	    for DIR in $STORAGE_DIRS ; do
		SPACE=$(get_space $DIR)
		if [ $MAX -lt $SPACE ] ; then
		    MAX=$SPACE
		fi
		SUM=$(($SUM+$SPACE))
	    done
	    if [ $MAX -lt $S_SPACE ] ; then
		    S_SPACE=$MAX
	    fi
	    echo "Max file size: $S_SPACE"
	    echo "Free space: $MAX"
	    ;;
	get-state) : reports status of file
	    if [ -f "$STAGE_DIR/$TARG" ] ; then
		    echo "File in stage"
	    elif [ -n "$(get_path)" ] ; then
		    echo "File in storage"
	    else
		    echo "File not found"
	    fi
	    ;;
	*)
	    echo Unknown command: $CMD
	    grep '[)] :' < $0 | sed 's/#//'
	    exit 1
	    ;;
    esac
}

get_path () {
    # find $ARG in $STORAGE_DIRS
    for DIR in $STORAGE_DIRS ; do
	if [ -f "$DIR/$ARG" ] ; then
	    echo "$DIR/$ARG"
	    return 0
	fi
    done
}

get_space () {
    # find space remaining in the filesystem $1
    df -PB1 "$1" | tail -1| (read I I I F I; echo $F)
}

main "$@"
