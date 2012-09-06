#!/bin/bash

set -eu

. $(dirname $(readlink -f $0))/setenv.sh

MIN_EXTRA_SPACE=8096

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
    TARG=$(encode_arg "$ARG")
    case "$CMD" in
	save) : save a file
	    # : arguments: length md5checksum filename
	    # : exit status:
	    # :  0: file saved or md5 matched old file
	    # :  1: no space, file was not saved
	    # :  2: wrong checksum, file was not saved
	    # :  3: file locked
	    # : output: URL
	    LENGTH="${TARG%% *}"
	    TARG="${TARG#* }"
	    MD5ARG=$(printf %s "${TARG%% *}" | tr A-Z a-z)
	    TARG="${TARG#* }"
	    global_lock
	    FILE=$(get_path tmp)
	    if [ -n "$FILE"  ] ; then
		    if fuser "$FILE" > /dev/null 2>&1 ; then
		        global_unlock
		        echo "file locked" 1>&2
		        exit 3
		    fi
		    MD5=$(md5sum < "$FILE")
		    MD5=${MD5%% *}
		    if [ "$(get_path)" ] ; then
    		    global_unlock
	    	    if [ "$MD5ARG" = "$MD5" ] ; then
		        	echo "file was saved" 1>&2
			        return 0
		        else
			        echo "file was saved with an other checksum"  1>&2
			        return 2
		        fi
		    fi
	    fi
	    SLENGTH=$(echo $LENGTH + $MIN_EXTRA_SPACE | bc)
	    DIR=$(find_fs.py reserve $FS_CONF "$TARG" $SLENGTH)
	    if [ -z "$DIR" ] ; then
		    echo "no space left" 1>&2
		    global_unlock
		    return 1
	    fi
	    mkdir -p $DIR/{tmp,files}
	    touch "$DIR/tmp/$TARG"
	    global_unlock
	    cat > "$DIR/tmp/$TARG"
	    find_fs.py unreserve  $FS_CONF "$TARG"
	    MD5=$(md5sum < "$DIR/tmp/$TARG")
	    MD5=${MD5%% *}
	    if [ "$MD5" != "$MD5ARG" ] ; then
		    rm "$DIR/tmp/$TARG"
		    echo "checksum error" 1>&2
		    exit 2
	    fi
	    echo "file saved"  1>&2
	    export A=${ARG#* * }
	    URL=$(python -c "import urllib,os; print urllib.quote_plus(os.environ['A'],'/')")
	    echo "$URL_PREFIX/$URL"
	    ;;
	approve) : approve file
	    # : arguments: md5sum filename
	    # : output: URL
	    MD5ARG=$(echo "${TARG%% *}" | tr A-Z a-z)
	    TARG="${TARG#* }"
	    global_lock
	    FILE=$(get_path tmp only)
	    if [ -z "$FILE" ] ; then
		    global_unlock
		    echo "file not found" 1>&2
		    exit 1
	    fi
	    DIR=${FILE%/tmp/*}
	    mv "$DIR/tmp/$TARG" "$DIR/files/$TARG"
	    global_unlock
	    MD5=$(md5sum < "$DIR/files/$TARG")
	    MD5=${MD5%% *}
	    if [ "$MD5" != "$MD5ARG" ] ; then
		    mv "$DIR/files/$TARG" "$DIR/tmp/$TARG"
		    echo "checksum error" 1>&2
		    exit 2
	    fi
	    register_sum.sh $DIR "$TARG" $MD5
	    echo "file approved" 1>&2
	    export A=${ARG#* }
	    URL=$(python -c "import urllib,os; print urllib.quote_plus(os.environ['A'],'/')")
	    echo "$URL_PREFIX/$URL"
	    ;;
	exists) : do a file exist
	    # : arguments filename
	    FILE=$(get_path tmp)
	    if [ -z "$FILE" ] ; then
		    echo "file not found" 1>&2
		    exit 1
	    fi
	    ;;
	exists-approved) : do a file exist and is approved
	    # : arguments: filename
	    FILE=$(get_path)
	    if [ -z "$FILE" ] ; then
		    echo "file not found" 1>&2
		    exit 1
	    fi
	    ;;
	get-md5) : get md5sum of a file
	    # : arguments: filename
	    # : return md5sum of file or an error message
	    # : exit status: 1 on errors, 0 on success
	    FILE=$(get_path tmp)
	    if [ -z "$FILE" ] ; then
		    echo "file not found" 1>&2
		    exit 1
	    fi
	    MD5=$(md5sum < "$FILE")
	    MD5=${MD5%% *}
	    echo $MD5
	    ;;
	get) : get a file
	    # : arguments: filename
	    # : returns file
	    # : exit status: 0 on success, else 1
	    FILE=$(get_path tmp)
	    if [ -n "$FILE" ] ; then
		    cat "$FILE"
	    else
		    echo "file not found" 1>&2
		    exit 1
	    fi
	    ;;
	get-approved) : get an approved file only
	    # : arguments: filename
	    # : returns file
	    # : exit status: 0 on success, else 1
	    FILE=$(get_path)
	    if [ -n "$FILE" ] ; then
		    cat "$FILE"
	    else
		    echo "file not found" 1>&2
		    exit 1
	    fi
	    ;;
	delete) : delete an unapproved file
	    # : arguments filename
	    global_lock
	    FILE=$(get_path tmp only)
	    global_unlock
	    if [ -z "$FILE" ] ; then
		    echo "file not found" 1>&2
		    exit 1
	    fi
	    rm -f $FILE
	    echo "file deleted" 1>&2
	    ;;
	getmd5s) : report md5 sums of stored files
	    # : no arguments
	    # : returns list of files, with md5 and date
	    for F in $MD5SUMDIR/*.md5s ; do
		if [ -f "$F" ] ; then
		    sortmd5s.py < $F
		fi
	    done | decode_stdin
	    ;;
	space-left) : report space left
	    SUM=0
	    MAX=0
	    for DIR in $(echo $(cat $FS_CONF) ) ; do
		SPACE=$(get_space $DIR)
		SPACE=$(echo $SPACE - $MIN_EXTRA_SPACE | bc)
		if [ $MAX -lt $SPACE ] ; then
		    MAX=$SPACE
		fi
		SUM=$(echo $SUM+$SPACE | bc)
	    done
	    echo "Max file size: $MAX"
	    echo "Free space: $SUM"
	    ;;
	get-state) : reports status of file
	    # report backupstate of file
	    # output: number of backup copies
	    FILE=$(get_path)
	    if [ -n "$FILE" ] ; then
		    echo $(/usr/openv/netbackup/bin/bplist "FILE" 2> /dev/null | wc -l)
	    else
		    echo "file not found" 1>&2
    		exit 1
	    fi
	    ;;
	*)
	    echo Unknown command: $CMD
	    grep '[)#] :' < $0 | sed 's/# [:]//'
	    exit 1
	    ;;
    esac
}

get_path () {
    for DIR in $(echo $(cat $FS_CONF) ) ; do
	if [ "${2:-}" != "only" -a -f "$DIR/files/$TARG" ] ; then
	    echo "$DIR/files/$TARG"
	    return 0
	fi
	if [ "${1:-}" = tmp -a -f "$DIR/tmp/$TARG" ] ; then
	    echo "$DIR/tmp/$TARG"
	    return 0
	fi
    done
}

global_lock () {
    lockfile -l 5 $LOCKFILE 2</dev/null
}

global_unlock () {
    rm -f $LOCKFILE
}

encode_arg () {
    printf %s "$1" | sed 's/%/%1/g;s+/+%2+g'
}

decode_arg() {
    printf %s "$1" | decode_stdin
}

decode_stdin () {
    sed 's+%2+/+g;s/%1/%/g'
}

get_space () {
    # find space remaining in the filesystem $1
    df -PB1 "$1" | tail -1| (read I I I F I; echo $F)
}

main "$@"
