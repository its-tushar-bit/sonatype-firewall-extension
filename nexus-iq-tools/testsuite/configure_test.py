#!/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import sys
import shutil
import tempfile
import logging
import argparse
import os

logging.basicConfig(level=logging.INFO)

logging.root.name = "PERF_AUTOMATION"
log = logging.getLogger()

sonatype_work_dir = "perf_run_work"


def createWorkTemp():
    tdir = tempfile.mkdtemp(prefix="awsPerfRun_", dir=".")
    log.info("Performance test temporary directory: {}".format(tdir))
    return tdir


def copyIq(iqBin, workingDir):
    shutil.copy(iqBin, workingDir)


def copyIqTools(iqToolsBin, workingDir):
    shutil.copy(iqToolsBin, workingDir)


def parse_args():
    parser = argparse.ArgumentParser(description="Execute automated performance test.")
    parser.add_argument("-p", "--profile", default='./default-performance-test-profile.json',
                        help="automated testing profile", required=False)
    parser.add_argument("-iq", "--iq-server", default=None,
                        help="Path to IQ Server jar", required=True)
    parser.add_argument("-tools", "--iq-tools", default=None,
                        help="Path to IQ Tools jar", required=True)
    parser.add_argument("-lic", "--license", dest="iq_license", default=None,
                        help="Path to IQ Server license", required=True)
    # parser.add_argument("-a", "--all-logs", default=False,
    #                     help="Retrieve all logs for the test run in addition to the results.",
    #                     action='store_true', required=False)
    parser.add_argument("-auto", "--automation", default=None,
                        help="Path to IQ automation zip.", required=True)

    parsed = parser.parse_args()
    log.debug(parsed)
    return parsed


def main():
    log.info("Automated performance test")

    parsed = parse_args()

    workingDir = createWorkTemp()

    shutil.copy(parsed.profile, os.path.join(workingDir))
    shutil.copy(parsed.iq_server, os.path.join(workingDir))
    shutil.copy(parsed.iq_tools, os.path.join(workingDir))
    shutil.copy(parsed.iq_license, os.path.join(workingDir))
    shutil.copy(parsed.automation, os.path.join(workingDir))

    # shutil.copy(parsed.profile,  os.path.join(workingDir, 'profile.json'))
    # shutil.copy(parsed.iq_server, os.path.join(workingDir, 'iq_server.jar'))
    # shutil.copy(parsed.iq_tools, os.path.join(workingDir, 'iq_tools.jar'))
    # shutil.copy(parsed.iq_license, os.path.join(workingDir, 'iq_license.lic'))

    sys.exit(0)


if __name__ == "__main__":
    main()
