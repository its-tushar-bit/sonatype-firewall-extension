#!/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

from jarexec import exec_jar, terminate_thread
import logging
import os


log = logging.getLogger(__name__)


class IqUtil(object):
    """Utility to interact with IQ Server jar"""

    def __init__(self, working_dir, iq_jar, sonatype_work_dir, iq_license=None):
        self.working_dir = working_dir
        self.iq_jar = iq_jar
        self.sonatype_work_dir = sonatype_work_dir
        self.iq_license = iq_license
        self._iq_thread = None
        self.iq_proc = None
        self.out_count = 0

    def get_console_out_filename(self):
        outfile = os.path.join(self.working_dir, 'iq_console_output-{}.out'.format(self.out_count))
        self.out_count += 1
        return outfile

    def cycle_iq(self, profile_params=[], profile_opts=[], outputToFile=True):
        """start IQ server and then exit once started"""

        self.start_iq(profile_params, profile_opts, outputToFile)
        self.stop_iq()

        log.info("finished run_iq_jar")

    def start_iq(self, profile_params=[], profile_opts=[], outputToFile=True):
        startupString = ".+org.eclipse.jetty.server.Server.+Started.+"
        work_dir_opt = "-Ddw.sonatypeWork=" + self.sonatype_work_dir
        profile_opts.append(work_dir_opt)

        if self.iq_license:
            profile_opts.append("-Ddw.licenseFile={}".format(self.iq_license))

        if outputToFile:
            consoleOut = self.get_console_out_filename()
            self.outfile = open(consoleOut, 'w')
            log.info("IQ started with console output directed to file: {}.".format(os.path.realpath(consoleOut)))
            self._iq_thread, self.iq_proc = exec_jar(self.iq_jar, self.working_dir, profile_params, profile_opts,
                                                     startupString, outputTo=self.outfile, returnThread=True)
        else:
            self._iq_thread, self.iq_proc = exec_jar(self.iq_jar, self.working_dir, profile_params, profile_opts,
                                                     startupString, returnThread=True)
        log.info("iq started, running")

    def stop_iq(self):
        log.info("stopping iq...")
        terminate_thread(self._iq_thread, self.iq_proc)
        log.info("iq stopped")
        self.outfile.close()

    def export_embedded_db(self, dump_file_path: str, java_opts: list = None, params: list = None):
        if java_opts is None:
            java_opts = []
        if params is None:
            params = []
        console_output_path = self.get_console_out_filename()
        log.info("Exporting H2 database for Postgres. The IQ console output is directed to the file: {}."
                 .format(os.path.realpath(console_output_path)))
        java_opts.extend(["-Ddw.sonatypeWork=" + self.sonatype_work_dir])
        params = ["export-embedded-db", "--dump-file", dump_file_path] + params
        with open(console_output_path, "w") as output_file:
            iq_thread, iq_proc = exec_jar(self.iq_jar, self.working_dir, params=params, javaopts=java_opts,
                     terminateOnOutput="Completed export to", outputTo=output_file, returnThread=True)
            if iq_proc.returncode != 0:
                raise Exception("Return code is different from 0")
