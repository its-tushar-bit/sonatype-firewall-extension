#!/usr/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import argparse
import re
import sys
import subprocess

class ResultdiffValidator:

    def __init__(self, file):
        self.file = file

    def _does_the_file_contain_differences(self):
        return subprocess.run(['grep', '-q', 'No differences found', self.file]).returncode != 0

    def _validate_differences(self):
        pattern = '(\\S+)\\s+(\\S+)\\s+(\\S+)'
        url_line = None
        min_max_line = None
        first_value_line = None
        second_value_line = None
        first_value = None
        with open(self.file) as file:
            for line in file:
                line = line.strip()
                if line.startswith('url'):
                    url_line = line
                elif line.startswith('min'):
                    min_max_line = line
                else:
                    if first_value == None:
                        first_value_line = line
                        first_value = int(re.match(pattern, line).group(2))
                    else:
                        second_value_line = line
                        second_value = int(re.match(pattern, line).group(2))
                        if first_value < second_value:
                            print('Validation failed due to the following:\n')
                            print(url_line)
                            print(min_max_line)
                            print(first_value_line)
                            print(second_value_line)
                            sys.exit(1)
                        else:
                            first_value = None
        sys.exit(0)

    def validate(self):
        if self._does_the_file_contain_differences():
            self._validate_differences()
        else:
            sys.exit(0)

if __name__ == '__main__':
    argument_parser = argparse.ArgumentParser()
    argument_parser.add_argument('-f', '--file', help='File with the result diff', required=True)
    parsed_arguments = argument_parser.parse_args()

    validator = ResultdiffValidator(parsed_arguments.file)
    validator.validate()
