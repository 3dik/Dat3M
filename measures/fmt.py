#!/usr/bin/env python3

import argparse
import sys

import reader

import bigrels
import runtime

def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--enclimit', type=int, default=5)
    parser.add_argument('--noout', action='store_true')
    return parser.parse_args()

def show_data(text, args):
    for name, data in reader.get_lists(text).items():
        if 'enc' == name:
            bigrels.do(data, args.enclimit)
        elif 'time' == name:
            runtime.do(data)

def show_output(text, args):
    if not args.noout:
        print(reader.get_output(text), end='')

def main():
    args = get_args()
    text = sys.stdin.read()
    show_data(text, args)
    show_output(text, args)

if '__main__' == __name__:
    main()
