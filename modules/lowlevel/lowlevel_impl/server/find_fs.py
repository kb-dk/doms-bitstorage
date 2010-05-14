#!/usr/bin/python
# encoding: latin1

"""find a filesystem from a list with space for a file and reserve the space
for 1 week.
"""
reserve_time = 7 * 24 * 60 * 60
import sys, time, fcntl, os, glob

def usage():
    print 'usage:\n\t', sys.argv[0],'reserve FS_TABLE NAME SIZE'
    print '\t', sys.argv[0], 'unreserve FS_TABLE NAME'
    sys.exit(1)

def main():
    global fs_table, f_size, f_name, now
    now = time.time()
    if len(sys.argv) == 1:
        usage()
        return
    if sys.argv[1] == 'reserve' and len(sys.argv) == 5:
        fs_table = sys.argv[2]
        f_name = sys.argv[3]
        f_size = long(sys.argv[4])
        lock(fs_table)
        reserve()
    elif sys.argv[1] == 'unreserve' and len(sys.argv) == 4:
        fs_table = sys.argv[2]
        f_name = sys.argv[3]
        lock(fs_table)
        unreserver()
    else:
        usage()

def lock(fs_table):
    lock_name = fs_table + '.lock'
    global lock_fh
    lock_fh = open(lock_name, 'w+')
    fcntl.lockf(lock_fh, fcntl.LOCK_EX)

def unreserver():
    reserved = {}
    r_lines = []
    read_res_file(fs_table, reserved, r_lines, f_name)
    write_res_file(fs_table, r_lines)
    #print 'reservation on', f_name, 'removed.'

def get_fs_free_space(fs):
    st = os.statvfs(fs)
    free_bytes = st.f_frsize * (st.f_bavail - 2)
    return free_bytes

def reserve():
    reserved = {}
    r_lines = []
    reserved_fs = read_res_file(fs_table, reserved, r_lines, f_name)
    fss = []
    for fs in open(fs_table).read().split():
        fss += glob.glob(fs)
    fss.sort()
    if reserved_fs in fss:
        # move reserved_fs to top
        fss.remove(reserved_fs)
        fss.insert(0, reserved_fs)
    found_fs = None
    for fs in fss:
        if reserved.has_key(fs):
            continue
        if get_fs_free_space(fs) > f_size:
            found_fs = fs
            break
    if not found_fs:
        for fs in fss:
            st = os.statvfs(fs)
            if get_fs_free_space(fs) - reserved.get(fs,0)> f_size:
                found_fs = fs
                break
    if not found_fs:
        return
    r_lines.append('%s %f %d %s\n' %
                   (found_fs, now + reserve_time,
                    f_size + 8096,
                    f_name))
    write_res_file(fs_table, r_lines)
    print found_fs

def write_res_file(fs_table, r_lines):
    res_name = fs_table + '.reserved'
    f = open(res_name + '.new', 'w')
    f.write(''.join(r_lines))
    f.close()
    os.rename(res_name + '.new', res_name)

def read_res_file(fs_table, reserved, r_lines, f_name):
    res_name = fs_table + '.reserved'
    reserved_fs = None
    if os.path.isfile(res_name):
        for line in open(res_name):
            fs, r_time, r_size, name = line[:-1].split(' ',3)
            if f_name == name:
                reserved_fs = fs
            elif float(r_time) > now:
                reserved[fs] = reserved.get(fs, 0) + long(r_size)
                r_lines.append(line)
    return reserved_fs


if __name__ == '__main__':
    main()
