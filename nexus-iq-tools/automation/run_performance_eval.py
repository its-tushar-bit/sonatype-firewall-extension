#!/bin/env python3

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

import s3util
from iqutil import IqUtil
from toolsutil import IqToolsUtil


logging.basicConfig(level=logging.INFO)

logging.root.name = "PERF_AUTOMATION"
log = logging.getLogger()

sonatype_work_dir = "perf_run_work"


def main():
    log.info("Automated performance test")

    parsed_args = parse_args()

    test_profile = read_profile(parsed_args.profile)

    working_directory = create_temporary_working_directory()

    prepare_working_directory(parsed_args, test_profile, working_directory)

    iq_server_util = IqUtil(working_directory, os.path.basename(parsed_args.iq_server), sonatype_work_dir,
                            parsed_args.iq_license)
    if parsed_args.use_postgres:
        iq_tools_util = IqToolsUtil(working_directory, os.path.basename(parsed_args.iq_tools), sonatype_work_dir,
                                    use_postgres=True, database=test_profile["postgres"]["database"])
    else:
        iq_tools_util = IqToolsUtil(working_directory, os.path.basename(parsed_args.iq_tools), sonatype_work_dir)

    prepare_database(parsed_args, test_profile, iq_server_util, iq_tools_util, working_directory)

    buildUrlTemplate(test_profile, iq_tools_util, parsed_args.use_postgres)
    log.info('template build done')

    prepare_iq_server_and_execute_test(test_profile, iq_server_util, iq_tools_util)

    record_output(test_profile)

    if not parsed_args.keep:
        clean(working_directory)

    sys.exit(0)


def parse_args():
    parser = argparse.ArgumentParser(description="Execute automated performance test.")
    parser.add_argument("-p", "--profile", default=None,
                        help="automated testing profile", required=True)
    parser.add_argument("-iq", "--iq-server", default=None,
                        help="IQ Server jar", required=True)
    parser.add_argument("-tools", "--iq-tools", default=None,
                        help="IQ Tools jar", required=True)
    parser.add_argument("-lic", "--license", dest="iq_license", default=None,
                        help="IQ Server license", required=False)
    parser.add_argument("-k", "--keep", default=False,
                        help="Keep temp directory with testing work files", action='store_true',
                        required=False)
    parser.add_argument("--use-postgres", dest="use_postgres", help="Determines if Postgres should be used",
                        required=False, action="store_true")
    parsed = parser.parse_args()
    log.debug(parsed)
    return parsed


def read_profile(profile):
    profile_data = {}
    with open(profile, encoding='utf-8') as profile_file:
        profile_data = json.load(profile_file)

    profile_data['results'] = {}
    return profile_data


def create_temporary_working_directory():
    tdir = tempfile.mkdtemp(prefix="perfTemp_", dir=".")
    log.info("Performance test temporary directory: {}".format(tdir))
    return tdir


def prepare_working_directory(parsed_args, test_profile, working_directory):
    copy_database(parsed_args, test_profile, working_directory)
    copy_iq_server(parsed_args.iq_server, working_directory)
    copy_iq_tools(parsed_args.iq_tools, working_directory)
    log.info('files copied')


def copy_database(parsed_args, testProfile, workingDir):
    if parsed_args.use_postgres:
        target = download_postgres_data_from_s3(testProfile, workingDir)
        dbzip = zipfile.ZipFile(target, 'r')
        dbzip.extractall(workingDir)
        dbzip.close()
        file = glob.glob(workingDir + "/postgres_*")[0]
        my_env = os.environ.copy()
        my_env["PGPASSWORD"] = testProfile["postgres"]["password"]
        subprocess.run(["psql", "--set", "ON_ERROR_STOP=1",
                        "--host", testProfile["postgres"]["hostname"],
                        "--port", str(testProfile["postgres"]["port"]),
                        "--username", testProfile["postgres"]["username"],
                        "--dbname", testProfile["postgres"]["database"],
                        "-f", file], env=my_env)

    else:
        dbPath = testProfile['iq_data']['data_path']

        if dbPath.lower().startswith('s3://'):
            target = copyDbS3(dbPath, testProfile['iq_data']['dataset'], workingDir, testProfile['iq_data'].get('data_arn'))
        else:
            target = copyDbLocal(dbPath, testProfile['iq_data']['dataset'], workingDir)

        log.info("copy {} dataset".format(testProfile['iq_data']['dataset']))
        log.info("target: "+target)

        perf_work = os.path.join(workingDir, sonatype_work_dir)
        os.mkdir(perf_work)
        dbzip = zipfile.ZipFile(target, 'r')
        dbzip.extractall(perf_work)
        dbzip.close()


def download_postgres_data_from_s3(test_profile, working_directory):
    bucket_name = test_profile['iq_data']['data_path'].split('://')[1]
    dataset_size = test_profile['iq_data']['dataset']
    destination_file_path = os.path.join(working_directory, "postgres-data.zip")
    s3util.download_database_data(bucket_name, dataset_size, destination_file_path, is_postgres=True)
    return destination_file_path


def copyDbS3(dbPath, dataset, workingDir, data_arn=None):
    bucket_target = dbPath.split('://')[1]
    bucket, keys = s3util.list_s3(bucket_target, data_arn)
    filtered = [ds_key for ds_key in keys if dataset in ds_key.split('/') and ds_key.endswith('.zip')]
    target_db = max(filtered, key=lambda x: getDbVersionFromFilename(x))
    dl_copy = os.path.join(workingDir, target_db.split('/')[-1])
    s3util.s3download(bucket, target_db, dl_copy)
    return dl_copy


def getDbVersionFromFilename(filename):
    fname = filename.split(os.path.sep)[-1]
    fname = fname.split('-')[1]
    fname = fname.split('.')[0]
    fname = "" + fname[1:]
    return int(fname)


def copyDbLocal(dbPath, dataset, workingDir):
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


def copy_iq_server(iqBin, workingDir):
    shutil.copy(iqBin,  workingDir)


def copy_iq_tools(iqToolsBin, workingDir):
    shutil.copy(iqToolsBin,  workingDir)


def prepare_database(parsed_args, test_profile, iq_server_util, iq_tools_util, working_directory):
    migrateDb(test_profile, iq_tools_util, iq_server_util)
    log.info('migrate done')

    shiftDb(test_profile, iq_tools_util, use_postgres=parsed_args.use_postgres)
    log.info('shift done')

    if not parsed_args.use_postgres:
        compactDb(test_profile, iq_tools_util)
        log.info('compact done')


def statsdec(func):
    def stats_decorator(profile, util, iq_util=None, iq_running=False, use_postgres=False):
        res = Result()
        res.db_info_before = reportDbInfo(util, iq_running)
        res.start = round(time.time())
        if iq_util:
            res.payload = func(profile, util, iq_util)
        else:
            if use_postgres:
                res.payload = func(profile, util, use_postgres)
            else:
                res.payload = func(profile, util)
        res.end = round(time.time())
        res.elapsed = res.end - res.start
        res.db_info_after = reportDbInfo(util, iq_running)
        log.info("{}: start: {}  end: {}  elapsed: {}sec".format(func.__name__, res.start, res.end, res.elapsed))

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
def migrateDb(testProfile, tools_util, iq_util):
    "start / stop iq against dataset. report size before/after."
    "capture before/after version"
    params, opts = get_params_and_opts(testProfile, 'iq_server', 'run_server')
    iq_util.cycle_iq(params, opts)


def get_params_and_opts(testProfile, util, key):
    params = testProfile.get(util, {}).get(key, {}).get('params', [])
    opts = testProfile.get(util, {}).get(key, {}).get('java_opts', [])
    return params, opts


@statsdec
def shiftDb(testProfile, tools_util, use_postgres=False):
    "shift db to current date if specified by test profile. report size before/after"
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
def compactDb(testProfile, tools_util):
    "start iq with compact command.  report size before/after"
    tools_util.compact_db()


@statsdec
def buildUrlTemplate(testProfile, tools_util, use_postgres=False):
    "call jar against shifted db with optional config & custom params"
    params, opts = get_params_and_opts(testProfile, 'iq_tools', 'generate_urls')
    if use_postgres:
        params = params + get_postgres_parameters_for_iq_tools(testProfile)
    tools_util.generate_urls(profile_params=params, profile_opts=opts)


def prepare_iq_server_and_execute_test(test_profile, iq_server_util, iq_tools_util):
    # *** start Iq - return once iq reports as started
    params, opts = get_params_and_opts(test_profile, 'iq_server', 'run_server')
    iq_server_util.start_iq(params, opts)
    log.info('iq started... about to test')

    # *** run test
    execute_test(test_profile, iq_tools_util, iq_running=True)
    log.info('tests done')

    # *** stop Iq
    iq_server_util.stop_iq()
    log.info('iq stopped')


@statsdec
def execute_test(testProfile, tools_util, iq_running=True):
    "start iq in one thread, execute url runner in another"
    params, opts = get_params_and_opts(testProfile, 'iq_tools', 'run_test')
    return tools_util.run_test(params, opts)


def record_output(testProfile):
    log.info('record output')
    targetOut = 'perf_results-{}.out'.format(time.strftime("%Y%m%d-%H%M%S"))
    with open(targetOut, "w", newline='') as outfile:
        for method in testProfile['results']:
            write_result(outfile, method, testProfile['results'][method])

    log.info("results output to: {}".format(os.path.abspath(targetOut)))


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
