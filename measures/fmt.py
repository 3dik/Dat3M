#!/usr/bin/env python3

import argparse
import sys

import reader

import bigrels
import runtime

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--rlimit', type=int, default=5,
            help='maximal number of listed relations')
    parser.add_argument('--alimit', type=int, default=5,
            help='maximal number of listed axioms')
    parser.add_argument('--noout', action='store_true')
    return parser.parse_args()

def show_output(text, args):
    if not args.noout:
        print(reader.get_output(text), end='')

def main():
    args = get_args()

    manager = reader.Reader(sys.stdin)
    for name, lists in manager.extract_lists():
        if 'enc' == name:
            bigrels.do(lists, args.rlimit, args.alimit)
        elif 'time' == name:
            runtime.do(lists)
        print('', end='', flush=True)

    if not args.noout:
        print(manager.get_output(), end='')

if '__main__' == __name__:
    main()
