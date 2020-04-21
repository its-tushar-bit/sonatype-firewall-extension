#!/usr/bin/env python3

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
    parser.add_argument("-p", "--profile", default='./performance-test-profile-medium.json',
                        help="automated testing profile", required=False)
    parser.add_argument("-iq", "--iq-server", default=None,
                        help="Path to IQ Server jar", required=True)
    parser.add_argument("-tools", "--iq-tools", default=None,
                        help="Path to IQ Tools jar", required=True)
    parser.add_argument("-lic", "--license", dest="iq_license", default=None,
                        help="Path to IQ Server license", required=True)
    parser.add_argument("-auto", "--automation", default=None,
                        help="Path to IQ automation zip.", required=True)
    parser.add_argument("-u", "--urls", default=None,
                        help="Path to target URLs JSON.", required=True)
    parser.add_argument("-l", "--logback", default=None,
                        help="Path to custom Logback file.", required=False)
    parser.add_argument("-cy", "--config-yaml", default=None, dest="config_yaml",
                        help="Path to custom config.yml for IQ Server.", required=False)
    parser.add_argument("--use-postgres", default=False, dest="use_postgres",
                        help="Determines if Postgres should be used.", action='store_true', required=False)
    parser.add_argument("--migrate-h2-to-postgres", dest="migrate_h2_to_postgres",
                        help="Determines if the dataset in the testing profile is H2 and "
                             "if it should be migrated to Postgres",
                        required=False, action="store_true")

    parsed = parser.parse_args()
    log.debug(parsed)
    return parsed


def main():
    log.info("Automated performance test")

    parsed_args = parse_args()

    workingDir = createWorkTemp()

    shutil.copy(parsed_args.profile, os.path.join(workingDir))
    shutil.copy(parsed_args.iq_server, os.path.join(workingDir))
    shutil.copy(parsed_args.iq_tools, os.path.join(workingDir))
    shutil.copy(parsed_args.iq_license, os.path.join(workingDir))
    shutil.copy(parsed_args.automation, os.path.join(workingDir))
    shutil.copy(parsed_args.urls, os.path.join(workingDir))
    if parsed_args.logback:
        shutil.copy(parsed_args.logback, os.path.join(workingDir))
    if parsed_args.config_yaml:
        shutil.copy(parsed_args.config_yaml, os.path.join(workingDir))

    with open("iqperf.auto.tfvars", "w") as file:
        file.write(("use_postgres = " + str(parsed_args.use_postgres).lower()))
    with open("run_aws_test.config", "w") as file:
        file.write(("USE_POSTGRES=" + str(parsed_args.use_postgres).lower() + "\n"))
        file.write(("MIGRATE_H2_TO_POSTGRES=" + str(parsed_args.migrate_h2_to_postgres).lower() + "\n"))

    sys.exit(0)


if __name__ == "__main__":
    main()
