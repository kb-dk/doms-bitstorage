#!/usr/bin/python

import sys

def main():
    m = {}
    for line in sys.stdin.read().split('\n')[:-1]:
        line = line.split(' ')
        md5 = line[-2]
        time = line[-1]
        fn = ' '.join(line[:-2])
        m[fn] = (md5, time)
    c = []
    for fn in m.keys():
        md5, time = m[fn]
        c.append((time, fn, md5))
    c.sort()
    for time, fn, md5 in c:
        if md5 != 'DELETED':
            print fn, md5, time

if __name__ == '__main__':
    main()
