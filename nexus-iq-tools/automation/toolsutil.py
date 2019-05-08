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

    def __init__(self, working_dir, iq_tools_jar, sonatype_work_dir):
        self.working_dir = working_dir
        self.iq_tools_jar = iq_tools_jar
        self.sonatype_work_dir = sonatype_work_dir
        self.ods = os.sep.join([self.sonatype_work_dir, 'data', 'ods'])

    def run_tools_jar(self, params, outputFile=None, javaopts=[]):
        if outputFile:
            targetOut = os.path.join(self.working_dir, outputFile)
            with open(targetOut, "w") as outfile:
                exec_jar(self.iq_tools_jar, self.working_dir, params, javaopts=javaopts, outputTo=outfile)
        else:
            exec_jar(self.iq_tools_jar, self.working_dir, params, javaopts=javaopts)

    def shift_db(self, datestring=None, profile_params=[], profile_opts=[]):
        params = ['dbmod', '-db', self.ods, '-info']
        self.run_tools_jar(params, "tools-shift-info.out", profile_opts)

        params = []
        db_params = ['dbmod', '-db', self.ods]
        if len(profile_params) > 0:
            params = db_params + profile_params
        else:
            if not datestring:
                datestring = datetime.datetime.today().strftime('%Y-%m-%d')
            log.info('shifting to: {}'.format(datestring))
            default_max = ['-maxDate', datestring, '-v']
            params = db_params + default_max

        self.run_tools_jar(params, "tools-shift.out", profile_opts)

    def compact_db(self, profile_params=[], profile_opts=[]):
        params = ['dbmod', '-db', self.ods, '-c']
        self.run_tools_jar(params, "tools-compact.out")

    def generate_urls(self, profile_params=[], profile_opts=[]):
        params = ['dbutil', '-db', self.ods] + profile_params
        self.test_url_file = "target_test_urls.json"
        self.run_tools_jar(params, self.test_url_file)

    def run_test(self, profile_params=[], profile_opts=['-Xms1024m', '-Xmx2048m']):
        params = ['urlrunner', '-s', '"http://localhost:8070"', '-f', self.test_url_file]
        self.run_tools_jar(params, "url-runner-out.txt", profile_opts)
        return os.path.join(self.working_dir, "url-runner-out.txt")

    def db_version(self, profile_params=[], profile_opts=[]):
        params = ['dbmod', '-db', self.ods, '-dbv']
        self.run_tools_jar(params, "tools-version.out")
