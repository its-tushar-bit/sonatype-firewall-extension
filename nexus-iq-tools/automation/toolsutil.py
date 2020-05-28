#!/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import datetime
import logging
import os

from jarexec import exec_jar

log = logging.getLogger(__name__)


class IqToolsUtil(object):
    """Utility to call iq tools jar
    """

    def __init__(self, working_dir, iq_tools_jar, sonatype_work_dir, use_postgres=False, database=None):
        self.working_dir = working_dir
        self.iq_tools_jar = iq_tools_jar
        self.sonatype_work_dir = sonatype_work_dir
        if use_postgres:
            self.database = database
        else:
            self.database = os.sep.join([self.sonatype_work_dir, 'data', 'ods'])

    def run_tools_jar(self, params, outputFile=None, javaopts=[]):
        if outputFile:
            targetOut = os.path.join(self.working_dir, outputFile)
            with open(targetOut, "w") as outfile:
                exec_jar(self.iq_tools_jar, self.working_dir, params, javaopts=javaopts,
                         output_file=outfile)
        else:
            exec_jar(self.iq_tools_jar, self.working_dir, params, javaopts=javaopts)

    def shift_db(self, datestring=None, profile_params=[], profile_opts=[]):
        params = ['dbmod', '-db', self.database, '-info'] + profile_params

        database_info_path = "tools-shift-info.out"
        log.info("Query database information and store it in %s", database_info_path)
        self.run_tools_jar(params, database_info_path, profile_opts)

        params = ['dbmod', '-db', self.database] + profile_params
        if not datestring:
            datestring = datetime.datetime.today().strftime('%Y-%m-%d')
        log.info('shifting to: {}'.format(datestring))
        default_max = ['-maxDate', datestring, '-v']
        params = params + default_max

        self.run_tools_jar(params, "tools-shift.out", profile_opts)

    def compact_db(self, profile_params=[], profile_opts=[]):
        params = ['dbmod', '-db', self.database, '-c']
        self.run_tools_jar(params, "tools-compact.out")

    def generate_urls(self, profile_params=[], profile_opts=[]):
        params = ['dbutil', '-db', self.database] + profile_params
        self.test_url_file = "target_test_urls.json"
        self.run_tools_jar(params, self.test_url_file)

    def run_test(self, profile_params=[], profile_opts=['-Xms1024m', '-Xmx2048m']):
        params = ['urlrunner', '-s', '"http://localhost:8070"', '-f', self.test_url_file]
        self.run_tools_jar(params, "url-runner-out.txt", profile_opts)
        return os.path.join(self.working_dir, "url-runner-out.txt")

    def db_version(self, profile_params=[], profile_opts=[]):
        params = ['dbmod', '-db', self.database, '-dbv']
        self.run_tools_jar(params, "tools-version.out")
