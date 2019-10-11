#!/bin/env python3
# rotate_reviewers.py

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import fileinput
import os

current_dir = os.path.dirname(os.path.abspath(__file__))
reviewers_file = os.path.join(current_dir, 'reviewers.txt')
readme_file =  os.path.join(current_dir, os.pardir, 'readme.md')

reviewers = [line.strip() for line in open(reviewers_file).read().strip().split('\n')]

review_begin = "<!-- rotating-reviewers-begin -->"
review_end = "<!-- rotating-reviewers-end -->"

current = []
replacements = []
in_replace = False

with fileinput.FileInput(readme_file, inplace=True) as file:
    for line in file:
        if not in_replace:
            print(line, end='')
        elif line.strip() == review_end:
            in_replace = False
            print(line, end='')
        else:
            rev = line.strip()
            next_rev = reviewers[(reviewers.index(rev) + 2) % len(reviewers)]
            current.append(rev)
            replacements.append(next_rev)
            print(line.replace(rev, next_rev), end='')

        if line.strip() == review_begin:
            in_replace = True

print(f"replacing:\n{current[0]}\n{current[1]}\nwith:\n{replacements[0]}\n{replacements[1]}")

