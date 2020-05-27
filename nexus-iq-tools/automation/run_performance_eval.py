#!/usr/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import sys
import os
import shutil
import tempfile
import zipfile
import logging
import argparse
import json
import time
import subprocess
import glob
import fileinput
import re
from typing import Tuple

import s3util
from iqutil import IqUtil
from toolsutil import IqToolsUtil


logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s [%(threadName)s] %(name)s - %(message)s",
                    datefmt="%Y-%m-%dT%H:%M:%S%z", stream=sys.stdout)
log = logging.getLogger(__name__)

sonatype_work_dir = "perf_run_work"


def main():
    log.info("Run Performance Evaluation started")

    parsed_args = parse_args()

    test_profile = read_profile(parsed_args.profile)

    working_directory = create_temporary_working_directory()

    iq_server_util = IqUtil(working_directory, os.path.basename(parsed_args.iq_server), sonatype_work_dir,
                            parsed_args.iq_license)

    prepare_working_directory(parsed_args, test_profile, working_directory, iq_server_util)

    if parsed_args.use_postgres:
        iq_tools_util = IqToolsUtil(working_directory, os.path.basename(parsed_args.iq_tools), sonatype_work_dir,
                                    use_postgres=True, database=test_profile["postgres"]["database"])
    else:
        iq_tools_util = IqToolsUtil(working_directory, os.path.basename(parsed_args.iq_tools), sonatype_work_dir)

    prepare_database(parsed_args, test_profile, iq_server_util, iq_tools_util)

    buildUrlTemplate(test_profile, iq_tools_util, iq_server_util, False, parsed_args.use_postgres)

    run_iq_server_and_execute_test(test_profile, iq_server_util, iq_tools_util)

    record_output(test_profile)

    if not parsed_args.keep:
        clean(working_directory)

    sys.exit(0)


def parse_args():
    parser = argparse.ArgumentParser(description="Execute automated performance test.")
    parser.add_argument("-p", "--profile", default=None,
                        help="JSON test profile", required=True)
    parser.add_argument("-iq", "--iq-server", default=None,
                        help="IQ Server jar", required=True)
    parser.add_argument("-tools", "--iq-tools", default=None,
                        help="IQ Tools jar", required=True)
    parser.add_argument("-lic", "--license", dest="iq_license", default=None,
                        help="IQ Server license", required=False)
    parser.add_argument("--use-postgres", dest="use_postgres",
                        help="Determines if Postgres should be used."
                             " The connection details should be in the testing profile",
                        required=False, action="store_true")
    parser.add_argument("--migrate-h2-to-postgres", dest="migrate_h2_to_postgres",
                        help="Determines if the dataset in the testing profile is H2 and "
                             "if it should be migrated to Postgres",
                        required=False, action="store_true")
    parser.add_argument("-k", "--keep", default=False,
                        help="Keep temp directory with testing work files", action='store_true',
                        required=False)
    parsed = parser.parse_args()
    log.debug(parsed)
    return parsed


def read_profile(profile):
    profile_data = {}
    log.info("Reading profile: %s", profile)
    with open(profile, encoding='utf-8') as profile_file:
        profile_data = json.load(profile_file)

    profile_data['results'] = {}
    return profile_data


def create_temporary_working_directory():
    tdir = tempfile.mkdtemp(prefix="perfTemp_", dir=".")
    log.info("Performance test temporary directory: {}".format(tdir))
    return tdir


def prepare_working_directory(parsed_args, test_profile, working_directory, iq_server_util: IqUtil):
    log.info("Prepare working environment")
    copy_iq_server_and_iq_tools_jars(parsed_args.iq_server, parsed_args.iq_tools, working_directory)
    copy_and_restore_database(parsed_args, test_profile, working_directory, iq_server_util)


def copy_iq_server_and_iq_tools_jars(iqBin, iqToolsBin, workingDir):
    log.info("Copy Jars (%s, %s) to %s", iqBin, iqToolsBin, workingDir)
    shutil.copy(iqBin,  workingDir)
    shutil.copy(iqToolsBin,  workingDir)


def copy_and_restore_database(parsed_args, test_profile, workingDir, iq_server_util: IqUtil):
    log.info("Copy and restore database")
    if parsed_args.use_postgres:
        log.info("Use PostgreSQL database")

        if parsed_args.migrate_h2_to_postgres:
            copy_h2_zip_and_extract(test_profile, workingDir)

            log.info("Generate and update H2 schemas")
            params, opts = get_params_and_opts(test_profile, 'iq_server', 'run_server')
            iq_server_util.cycle_iq(["server"], opts)

            log.info("Migrate H2 database to PostgreSQL")
            params, opts = get_params_and_opts(test_profile, "iq_server", "export-embedded-db")
            iq_server_util.export_embedded_db("postgres-data.sql", java_opts=opts, params=params)
            file = workingDir + "/postgres-data.sql"

            filter_invalid_postgres_statements(file)  # TODO: remove once the issue has been solved
        else:
            target = download_postgres_data_from_s3(test_profile, workingDir)
            dbzip = zipfile.ZipFile(target, 'r')
            dbzip.extractall(workingDir)
            dbzip.close()
            file = glob.glob(workingDir + "/postgres_*")[0]

        log.info("Restore database with file: %s", file)
        my_env = os.environ.copy()
        my_env["PGCLIENTENCODING"] = "UTF8"
        my_env["PGPASSWORD"] = test_profile["postgres"]["password"]
        completed_process = subprocess.run(["psql", "--set", "ON_ERROR_STOP=1",
                        "--host", test_profile["postgres"]["hostname"],
                        "--port", str(test_profile["postgres"]["port"]),
                        "--username", test_profile["postgres"]["username"],
                        "--dbname", test_profile["postgres"]["database"],
                        "-f", file], env=my_env, check=True)

    else:
        log.info("Use H2 database")
        copy_h2_zip_and_extract(test_profile, workingDir)


def copy_h2_zip_and_extract(test_profile, workingDir):
    dbPath = test_profile['iq_data']['data_path']

    if dbPath.lower().startswith('s3://'):
        bucket_name = test_profile['iq_data']['data_path'].split('://')[1]
        dataset_size = test_profile['iq_data']['dataset']
        target = os.path.join(workingDir, "h2-data.zip")
        log.info("Download %s database from %s S3 bucket to %s", dataset_size,
                 bucket_name, target)
        s3util.download_database_data(bucket_name, dataset_size, target)
    else:
        target = copyDbLocal(dbPath, test_profile['iq_data']['dataset'], workingDir)

    log.info("database file: " + target)
    perf_work = os.path.join(workingDir, sonatype_work_dir)
    os.mkdir(perf_work)
    dbzip = zipfile.ZipFile(target, 'r')
    dbzip.extractall(perf_work)
    dbzip.close()


def filter_invalid_postgres_statements(file):
    pattern = re.compile('(CREATE UNIQUE INDEX )".+?"\\.(.+)', re.DOTALL)
    for line in fileinput.input(file, inplace=True):
        matcher = pattern.fullmatch(line)
        if matcher:
            print('{}{}'.format(matcher.group(1), matcher.group(2)), end='')
        else:
            print(line, end='')


def download_postgres_data_from_s3(test_profile, working_directory):
    bucket_name = test_profile['iq_data']['data_path'].split('://')[1]
    dataset_size = test_profile['iq_data']['dataset']
    destination_file_path = os.path.join(working_directory, "postgres-data.sql.zip")
    log.info("Download %s database from %s S3 bucket to %s", dataset_size,
             bucket_name, destination_file_path)
    s3util.download_database_data(bucket_name, dataset_size, destination_file_path, is_postgres=True)
    return destination_file_path


def getDbVersionFromFilename(filename):
    fname = filename.split(os.path.sep)[-1]
    fname = fname.split('-')[1]
    fname = fname.split('.')[0]
    fname = "" + fname[1:]
    return int(fname)


def copyDbLocal(dbPath, dataset, workingDir):
    log.info("Copy local %s database from %s to %s", dataset, dbPath, workingDir)
    target = None
    for root, dirs, files in os.walk(dbPath):
        if os.path.basename(root) != dataset:
            continue

        for file in files:
            if not file.endswith('zip'):
                continue
            if not target:
                target = os.path.join(root, file)
            else:
                if getDbVersionFromFilename(file) > getDbVersionFromFilename(target):
                    target = os.path.join(root, file)

    return target


def prepare_database(parsed_args, test_profile, iq_server_util, iq_tools_util):
    log.info("Prepare database")
    if not parsed_args.migrate_h2_to_postgres:
        migrateDb(test_profile, iq_tools_util, iq_server_util, False, parsed_args.use_postgres)

    shiftDb(test_profile, iq_tools_util, iq_server_util, False, parsed_args.use_postgres)

    if not parsed_args.use_postgres:
        compactDb(test_profile, iq_tools_util, iq_server_util, False, parsed_args.use_postgres)


def statsdec(func):
    def stats_decorator(profile, util, iq_util, iq_running, use_postgres):
        res = Result()
        if not use_postgres:
            res.db_info_before = reportDbInfo(util, iq_running)
        res.start = round(time.time())
        res.payload = func(profile, util, iq_util, iq_running, use_postgres)
        res.end = round(time.time())
        res.elapsed = res.end - res.start
        if not use_postgres:
            res.db_info_after = reportDbInfo(util, iq_running)
        log.info("Stats for {}: start: {}  end: {}  elapsed: {}sec".format(func.__name__, res.start, res.end, res.elapsed))

        profile['results'][func.__name__] = res
        return res.payload

    return stats_decorator


def reportDbInfo(tools_util, iq_running=False):
    dbinfo = []
    for root, dirs, files in os.walk(tools_util.working_dir):
        for filename in files:
            if filename.lower().endswith('.h2.db'):
                dbfile = os.path.join(root, filename)
                dbsize = os.stat(dbfile).st_size
                dbver = -1
                if not iq_running:
                    tools_util.db_version()
                    dbver = int(open(tools_util.working_dir + '/tools-version.out', 'r').read().strip())
                dbinfo.append((dbfile, dbver, dbsize))
    return dbinfo


@statsdec
def migrateDb(testProfile, tools_util, iq_util, iq_running, use_postgres):
    "start / stop iq against dataset. report size before/after."
    "capture before/after version"
    log.info("Generate and update schemas")
    params, opts = get_params_and_opts(testProfile, 'iq_server', 'run_server')
    iq_util.cycle_iq(params, opts)


def get_params_and_opts(testProfile: dict, util: str, key: str) -> Tuple[list, list]:
    params = testProfile.get(util, {}).get(key, {}).get('params', [])
    opts = testProfile.get(util, {}).get(key, {}).get('java_opts', [])
    return params, opts


@statsdec
def shiftDb(testProfile, tools_util, iq_util, iq_running, use_postgres):
    "shift db to current date if specified by test profile. report size before/after"
    log.info('Shift database time columns to the current date')
    params, opts = get_params_and_opts(testProfile, 'iq_tools', 'shift_db')
    if use_postgres:
        params = params + get_postgres_parameters_for_iq_tools(testProfile)
    tools_util.shift_db(profile_params=params, profile_opts=opts)


def get_postgres_parameters_for_iq_tools(test_profile):
    return ["--postgres",
            "-h", test_profile["postgres"]["hostname"],
            "-p", str(test_profile["postgres"]["port"]),
            "-user", test_profile["postgres"]["username"],
            "-pass", test_profile["postgres"]["password"]]


@statsdec
def compactDb(testProfile, tools_util, iq_util, iq_running, use_postgres):
    "start iq with compact command.  report size before/after"
    log.info("Compact H2 database")
    tools_util.compact_db()


@statsdec
def buildUrlTemplate(testProfile, tools_util, iq_util, iq_running, use_postgres):
    "call jar against shifted db with optional config & custom params"
    log.info("Build URL template")
    params, opts = get_params_and_opts(testProfile, 'iq_tools', 'generate_urls')
    if use_postgres:
        params = params + get_postgres_parameters_for_iq_tools(testProfile)
    tools_util.generate_urls(profile_params=params, profile_opts=opts)


def run_iq_server_and_execute_test(test_profile, iq_server_util, iq_tools_util):
    log.info('Run IQ Server and execute test')

    log.info("Run IQ Server")
    params, opts = get_params_and_opts(test_profile, 'iq_server', 'run_server')
    iq_server_util.start_iq(params, opts)

    execute_test(test_profile, iq_tools_util, iq_server_util, True, False)

    log.info("Stop IQ Server")
    iq_server_util.stop_iq()


@statsdec
def execute_test(testProfile, tools_util, iq_util, iq_running, use_postgres):
    "start iq in one thread, execute url runner in another"
    log.info("Execute test")
    params, opts = get_params_and_opts(testProfile, 'iq_tools', 'run_test')
    return tools_util.run_test(params, opts)


def record_output(testProfile):
    log.info('Record output')
    targetOut = 'perf_results-{}.out'.format(time.strftime("%Y%m%d-%H%M%S"))
    with open(targetOut, "w", newline='') as outfile:
        for method in testProfile['results']:
            write_result(outfile, method, testProfile['results'][method])

    log.info("Results output to: {}".format(os.path.abspath(targetOut)))


def write_result(outfile, method, res):
    outfile.write('-'*10 + os.linesep)
    outfile.write("method: {}".format(method) + os.linesep)
    try:
        outfile.write(str(res) + os.linesep)
        if res.payload and os.path.isfile(res.payload):
            outfile.write('-'*40 + os.linesep)
            with open(res.payload, 'r', newline='') as source:
                for line in source:
                    outfile.write(line)
    except Exception:
        pass


def clean(workingDir):
    "remove temp files and directories"
    log.info("Clean directory: %s", workingDir)
    shutil.rmtree(workingDir, True)


class Result(object):
    def __init__(self, start=None,  end=None,  elapsed=None, db_info_before=None,  db_info_after=None, payload=None):
        self.start=start
        self.end=end
        self.elapsed=elapsed
        self.db_info_before=db_info_before
        self.db_info_after=db_info_after
        self.payload=payload

    def __str__(self):
        return "Result(start={}, end={}, elapsed={}, db_info_before={}, db_info_after={}, payload={})".format(
            self.start,
            self.end,
            self.elapsed,
            self.db_info_before,
            self.db_info_after,
            self.payload)


if __name__ == "__main__":
    main()
