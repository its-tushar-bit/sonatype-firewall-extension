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

import s3util
from iqutil import IqUtil
from toolsutil import IqToolsUtil


logging.basicConfig(level=logging.INFO)

logging.root.name = "PERF_AUTOMATION"
log = logging.getLogger()

sonatype_work_dir = "perf_run_work"


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


def createWorkTemp():
    tdir = tempfile.mkdtemp(prefix="perfTemp_", dir=".")
    log.info("Performance test temporary directory: {}".format(tdir))
    return tdir


def readProfile(profile):
    profile_data = {}
    with open(profile, encoding='utf-8') as profile_file:
        profile_data = json.load(profile_file)

    profile_data['results'] = {}
    return profile_data


def getDbVersionFromFilename(filename):
    fname = filename.split(os.path.sep)[-1]
    fname = fname.split('-')[1]
    fname = fname.split('.')[0]
    fname = "" + fname[1:]
    return int(fname)


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


def copyDb(testProfile, workingDir):
    dbPath = testProfile['iq_data']['data_path']
    target = None
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


def copyDbS3(dbPath, dataset, workingDir, data_arn=None):
    bucket_target = dbPath.split('://')[1]
    bucket, keys = s3util.list_s3(bucket_target, data_arn)
    filtered = [ds_key for ds_key in keys if dataset in ds_key.split('/') and ds_key.endswith('.zip')]
    target_db = max(filtered, key=lambda x: getDbVersionFromFilename(x))
    dl_copy = os.path.join(workingDir, target_db.split('/')[-1])
    s3util.s3download(bucket, target_db, dl_copy)
    return dl_copy


def copyDbLocal(dbPath, dataset, workingDir):
    target = None
    for root, dirs, files in os.walk(dbPath):
        if os.path.basename(root) != dataset:
            continue

        for file in files:
            if not target:
                target = os.path.join(root, file)
            else:
                if getDbVersionFromFilename(file) > getDbVersionFromFilename(target):
                    target = os.path.join(root, file)

    return target


def copyIq(iqBin, workingDir):
    shutil.copy(iqBin,  workingDir)


def copyIqTools(iqToolsBin, workingDir):
    shutil.copy(iqToolsBin,  workingDir)


def statsdec(func):
    def stats_decorator(profile, util, iq_util=None, iq_running=False):
        res = Result()
        res.db_info_before = reportDbInfo(util, iq_running)
        res.start = round(time.time())
        if iq_util:
            res.payload = func(profile, util, iq_util)
        else:
            res.payload = func(profile, util)
        res.end = round(time.time())
        res.elapsed = res.end - res.start
        res.db_info_after = reportDbInfo(util, iq_running)
        log.info("{}: start: {}  end: {}  elapsed: {}sec".format(func.__name__, res.start, res.end, res.elapsed))

        profile['results'][func.__name__] = res
        return res.payload

    return stats_decorator


def get_params_and_opts(testProfile, util, key):
    params = testProfile.get(util, {}).get(key, {}).get('params', [])
    opts = testProfile.get(util, {}).get(key, {}).get('java_opts', [])
    return params, opts


@statsdec
def migrateDb(testProfile, tools_util, iq_util):
    "start / stop iq against dataset. report size before/after."
    "capture before/after version"
    params, opts = get_params_and_opts(testProfile, 'iq_server', 'run_server')
    iq_util.cycle_iq(params, opts)


@statsdec
def shiftDb(testProfile, tools_util):
    "shift db to current date if specified by test profile. report size before/after"
    params, opts = get_params_and_opts(testProfile, 'iq_tools', 'shift_db')
    tools_util.shift_db(profile_params=params, profile_opts=opts)


@statsdec
def compactDb(testProfile, tools_util):
    "start iq with compact command.  report size before/after"
    tools_util.compact_db()


@statsdec
def buildUrlTemplate(testProfile, tools_util):
    "call jar against shifted db with optional config & custom params"
    params, opts = get_params_and_opts(testProfile, 'iq_tools', 'generate_urls')
    tools_util.generate_urls(profile_params=params, profile_opts=opts)


@statsdec
def executeTest(testProfile, tools_util, iq_running=True):
    "start iq in one thread, execute url runner in another"
    return tools_util.run_test()


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


def recordOutput(testProfile, workingDir):
    log.info('record output')
    targetOut = 'perf_results-{}.out'.format(time.strftime("%Y%m%d-%H%M%S"))
    with open(targetOut, "w", newline='') as outfile:
        for method in testProfile['results']:
            write_result(outfile, method, testProfile['results'][method])

    log.info("results output to: {}".format(os.path.abspath(targetOut)))


def clean(testProfile, workingDir):
    "remove temp files and directories"
    shutil.rmtree(workingDir, True)


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
                        help="Keep temp directory with testing work files", action='store_true', required=False)
    parsed = parser.parse_args()
    log.debug(parsed)
    return parsed


def main():
    log.info("Automated performance test")

    parsed = parse_args()

    workingDir = createWorkTemp()

    testProfile = readProfile(parsed.profile)

    copyDb(testProfile, workingDir)
    copyIq(parsed.iq_server, workingDir)
    copyIqTools(parsed.iq_tools, workingDir)
    log.info('files copied')

    iq_util = IqUtil(workingDir, os.path.basename(parsed.iq_server), sonatype_work_dir, parsed.iq_license)
    tools_util = IqToolsUtil(workingDir, os.path.basename(parsed.iq_tools), sonatype_work_dir)

    # **** migrate db
    migrateDb(testProfile, tools_util, iq_util)
    log.info('migrate done')

    # *** shift db
    shiftDb(testProfile, tools_util)
    log.info('shift done')

    # *** compact db
    compactDb(testProfile, tools_util)
    log.info('compact done')

    # *** populate url template
    buildUrlTemplate(testProfile, tools_util)
    log.info('template build done')

    # *** start Iq - return once iq reports as started
    params, opts = get_params_and_opts(testProfile, 'iq_server', 'run_server')
    iq_util.start_iq(params, opts)
    log.info('iq started... about to test')

    # *** run test
    executeTest(testProfile, tools_util, iq_running=True)
    log.info('tests done')

    # *** stop Iq
    iq_util.stop_iq()
    log.info('iq stopped')

    # *** capture output, copy to destination
    recordOutput(testProfile, workingDir)

    # *** optional cleanup
    if not parsed.keep:
        clean(testProfile, workingDir)

    sys.exit(0)


if __name__ == "__main__":
    main()
