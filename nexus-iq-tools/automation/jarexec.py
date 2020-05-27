#!/bin/env python3

# Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
# Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
# "Sonatype" is a trademark of Sonatype, Inc.

import logging
import os
import subprocess
import sys
import threading
import re
import time
import typing

log = logging.getLogger(__name__)


class _ProcessOutputReader(threading.Thread):

    def __init__(self, proc: subprocess.Popen, output_file, wait_line_pattern=None):
        super().__init__(name="ProcessOutputReader")
        self.proc = proc
        self.output_file = output_file
        self.wait_line_pattern_found = False
        self.wait_line_pattern = wait_line_pattern
        self.exception = None
        self.check_process_exit_status = True

    def run(self):
        try:
            log.info("Read output of process %d until it finishes", self.proc.pid)
            if self.wait_line_pattern:
                log.info("Find wait line pattern \"%s\"", self.wait_line_pattern)

            for line in iter(self.proc.stdout.readline, ""):
                logline = line.strip()
                if self.output_file is not None:
                    self.output_file.write(logline + os.linesep)
                else:
                    log.info('Output: {0}'.format(logline))

                if self.wait_line_pattern is not None and not self.wait_line_pattern_found:
                    pattern = re.compile(self.wait_line_pattern)
                    if pattern.fullmatch(logline):
                        log.info("Wait line pattern \"%s\" found: %s", self.wait_line_pattern,
                                 logline)
                        self.wait_line_pattern_found = True

            if self.wait_line_pattern is not None and not self.wait_line_pattern_found:
                log.warning(f"The process {self.proc.pid} finished but the line pattern "
                               f"\"{self.wait_line_pattern}\" was not found")

            log.info("All output of process %d was read", self.proc.pid)
        except Exception as e:
            log.exception(e)
            self.exception = e


def exec_jar(jar, wdir, params=[], javaopts=[], wait_line_pattern=None, output_file=None,
             timeout_seconds=3600) -> typing.Tuple[threading.Thread, subprocess.Popen]:
    runJar = []

    # build call
    runJar.append('java')
    for opt in javaopts:
        if opt.startswith('-D') or opt.startswith('-X'): runJar.append(opt)
        else: runJar.append('-D'+opt)

    runJar.append('-jar')
    runJar.append(jar)
    for param in params:
        runJar.append(param)

    env = dict(os.environ)

    wdirr = os.path.realpath(wdir) + os.sep

    runJarS = ' '.join(runJar)  # need for shell=True

    log.info("Launch subprocess with the command: {}".format(runJarS))
    proc = subprocess.Popen(runJarS,
                            cwd=wdirr,
                            shell=True,
                            env=env,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT,
                            universal_newlines=True)
    log.info("Subprocess launched with PID: %s", proc.pid)

    thread = _ProcessOutputReader(proc, output_file, wait_line_pattern=wait_line_pattern)
    log.info("Start thread \"%s\" to read output of process %d", thread.name, proc.pid)
    thread.start()

    try:
        for i in range(timeout_seconds):

            if thread.exception is not None:
                raise Exception(f"The {thread.name} thread has an exception") from thread.exception
            elif _read_all_output_and_exited_with_success(wait_line_pattern, proc):
                terminate_process_and_thread(proc, thread)
                return thread, proc
            elif _read_all_output_and_exited_with_error(wait_line_pattern, proc):
                raise Exception(f"The process {proc.pid} finished "
                                f"with an error exit status: {proc.returncode}")
            elif _wait_line_pattern_found(wait_line_pattern, proc, thread):
                return thread, proc
            elif _looking_for_wait_line_pattern_but_process_finished(wait_line_pattern, proc):
                raise Exception(f"The process {proc.pid} finished but this is not allowed when "
                                f"a wait line pattern is set")

            time.sleep(1)

            if i == (timeout_seconds-1):
                raise Exception("Timeout while waiting for thread \"" + thread.name + "\"")

            _maintain_output_active(i+1, wait_line_pattern)
    except Exception as e:
        terminate_process_and_thread(proc, thread, ignore_thread_exception=True)
        raise e


def terminate_process_and_thread(process: subprocess.Popen, thread: threading.Thread,
                                 ignore_thread_exception=False):
    log.info("Terminate process with PID %d and thread \"%s\" ", process.pid, thread.name)

    t = typing.cast(_ProcessOutputReader, thread)
    if not ignore_thread_exception and t.exception is not None:
        raise Exception(f"Exception found in the {t.name} thread when terminating")\
            from t.exception

    if process.poll() is None:
        log.info("Terminate process with PID %d", process.pid)
        thread.check_process_exit_status = False
        if sys.platform == 'win32':
            print ("Try to kill: {}".format(process.pid))
            killer = subprocess.Popen("taskkill /F /T /PID %i"%process.pid , shell=True)
            killer.wait()
        else:
            process.terminate()

        if thread.is_alive():
            log.info("Wait thread \"%s\" to finish", thread.name)
            thread.join(timeout=5)

        process.communicate(timeout=5)
        log.info('Subprocess finished with exit status %d', process.returncode)
    elif thread.is_alive():
        log.info("Wait thread \"%s\" to finish", thread.name)
        thread.join(timeout=5)


def _read_all_output_and_exited_with_success(wait_line_pattern, proc):
    return wait_line_pattern is None and proc.poll() is not None and proc.returncode == 0


def _read_all_output_and_exited_with_error(wait_line_pattern, proc):
    return wait_line_pattern is None and proc.poll() is not None and proc.returncode != 0


def _wait_line_pattern_found(wait_line_pattern, proc, thread):
    return wait_line_pattern is not None and proc.poll() is None and thread.wait_line_pattern_found


def _looking_for_wait_line_pattern_but_process_finished(wait_line_pattern, proc):
    return wait_line_pattern is not None and proc.poll() is not None


def _maintain_output_active(i, wait_line_pattern):
    if i % 30 == 0:
        if wait_line_pattern is not None:
            log.info("Waiting for thread to find line pattern \"%s\"...",
                     wait_line_pattern)
        else:
            log.info("Waiting for process to finish...")