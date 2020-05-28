#!/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import unittest
import logging
import sys
import os
import io
import subprocess
from jarexec import exec_jar, terminate_process_and_thread

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s [%(threadName)s] "
                                               "%(name)s - %(message)s",
                    datefmt="%Y-%m-%dT%H:%M:%S%z", stream=sys.stdout)
log = logging.getLogger(__name__)


class JarexecTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        log.info("Creating ProgramForTestingJarExec.jar...")
        subprocess.run(["javac", "ProgramForTestingJarExec.java"], cwd="java", timeout=5,
                       check=True, stdout=subprocess.DEVNULL)
        subprocess.run(["jar", "cvfe", "ProgramForTestingJarExec.jar",
                        "ProgramForTestingJarExec", "ProgramForTestingJarExec.class"],
                       cwd="java", timeout=5, check=True, stdout=subprocess.DEVNULL)
        dir_path = os.path.dirname(os.path.realpath(__file__))
        log.info("Jar created in: %s", os.path.join(dir_path, "java",
                                                       "ProgramForTestingJarExec.jar"))

    def setUp(self):
        self.path_to_jar = "ProgramForTestingJarExec.jar"
        self.working_directory = "java"
        self.process = None
        self.thread = None
        self.output_stream = io.StringIO()

    def test_execJar_waitLinePatternPresent_processAndThreadRunning(self):
        self.thread, self.process = exec_jar(self.path_to_jar, self.working_directory,
                                             params=["--continuous"],
                                             output_file=self.output_stream,
                                             wait_line_pattern=".*line 10$")

        self.assertIsNone(self.process.poll())
        self.assertTrue(self.thread.is_alive())
        self.assertGreaterEqual(len(self.output_stream.getvalue().splitlines()), 10)

    def test_execJar_waitLinePatternNotPresent_throwsException(self):
        self.assertRaises(Exception, exec_jar, self.path_to_jar, self.working_directory,
                          params=["--small"], output_file=self.output_stream,
                          wait_line_pattern=".*line 100$")

    def test_execJar_searchingWaitLinePatternButProcessFinished_throwsException(self):
        self.assertRaisesRegex(Exception, "^The process \\d+ finished but this is not allowed when "
                                          "a wait line pattern is set$",
                               exec_jar, self.path_to_jar, self.working_directory,
                               params=["--delayed-exception"], output_file=self.output_stream,
                               wait_line_pattern=".*line 50$")

    def test_execJar_successExitStatus_processAndThreadFinished(self):
        self.thread, self.process = exec_jar(self.path_to_jar, self.working_directory,
                                             params=["--small"], output_file=self.output_stream)

        self.assertEqual(0, self.process.returncode)
        self.assertFalse(self.thread.is_alive())
        self.assertEqual(50, len(self.output_stream.getvalue().splitlines()))

    def test_execJar_errorExitStatus_throwsException(self):
        self.assertRaisesRegex(Exception,
                               "^The process \\d+ finished with an error exit status: -?\\d+$",
                               exec_jar, self.path_to_jar, self.working_directory,
                               params=["--exception"], output_file=self.output_stream)

    def test_execJar_timeoutReached_throwsException(self):
        self.assertRaisesRegex(Exception, "^Timeout.+", exec_jar, self.path_to_jar,
                               self.working_directory, params=["--continuous"],
                               output_file=self.output_stream, timeout_seconds=1)

    def test_execJar_threadHasException_throwsException(self):
        self.assertRaisesRegex(Exception, "^The ProcessOutputReader thread has an exception$",
                               exec_jar, self.path_to_jar, self.working_directory,
                               params=["--continuous"], output_file="this-is-not-a-file")

    def test_terminateProcessAndThread(self):
        self.thread, self.process = exec_jar(self.path_to_jar, self.working_directory,
                                             params=["--continuous"],
                                             output_file=self.output_stream,
                                             wait_line_pattern=".*line 20$")

        terminate_process_and_thread(self.process, self.thread)

        self.assertIsNotNone(self.process.returncode)
        self.assertFalse(self.thread.is_alive())

    def test_terminateProcessAndThread_threadHasException_throwsException(self):
        self.thread, self.process = exec_jar(self.path_to_jar, self.working_directory,
                                             params=["--continuous"],
                                             output_file=self.output_stream,
                                             wait_line_pattern=".*line 10$")
        self.output_stream.close()
        self.thread.join(timeout=5)

        self.assertRaisesRegex(Exception, "^Exception found in the ProcessOutputReader thread "
                                          "when terminating$",
                               terminate_process_and_thread, self.process, self.thread)

    def test_terminateProcessAndThread_threadExceptionIgnored_processAndThreadTerminated(self):
        self.thread, self.process = exec_jar(self.path_to_jar, self.working_directory,
                                             params=["--continuous"],
                                             output_file=self.output_stream,
                                             wait_line_pattern=".*line 10$")
        self.output_stream.close()
        self.thread.join(timeout=5)

        terminate_process_and_thread(self.process, self.thread, ignore_thread_exception=True)

        self.assertIsNotNone(self.process.returncode)
        self.assertFalse(self.thread.is_alive())

    def tearDown(self):
        if self.process is not None:
            if self.process.poll() is None:
                log.info("Terminate process with PID %d", self.process.pid)
                self.process.terminate()
                if self.thread.is_alive():
                    log.info("Wait for thread \"%s\" to finish", self.thread.name)
                    self.thread.join(timeout=5)
                self.process.communicate(timeout=5)
            elif self.thread.is_alive():
                log.info("Wait for thread \"%s\" to finish", self.thread.name)
                self.thread.join(timeout=5)

        if not self.output_stream.closed:
            log.info("Close output stream")
            self.output_stream.close()


if __name__ == '__main__':
    unittest.main()
