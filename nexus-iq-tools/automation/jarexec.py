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


log = logging.getLogger(__name__)


max_timeout = 3600 # 1 hour

def output_reader(proc, callback, outFile, callbackTrigger=None):
    for line in iter(proc.stdout.readline, b''):
        logline = line.decode('utf-8').strip()
        if outFile:
            outFile.write(logline + os.linesep)
        else:
            log.info('{0}'.format(logline))

        if callbackTrigger:
            pattern = re.compile(callbackTrigger)
            if pattern.fullmatch(logline):
                callback.append("triggered")
    return proc.wait()

def exec_jar(jar, wdir, params=[], javaopts=[], terminateOnOutput=None, outputTo=None, returnThread=False):
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
    log.info("Launching subprocess with command: {}".format(runJarS))

    proc = subprocess.Popen(runJarS,
                            cwd=wdirr,
                            shell=True,
                            env=env,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT)
    callback = []
    t = threading.Thread(target=output_reader, args=(proc, callback, outputTo, terminateOnOutput))
    t.start()

    try:
        for i in range(max_timeout):
            if len(callback) > 0:
                break
            t.join(timeout=1)
    finally:
        if returnThread:
            return t, proc
        terminate_thread(t, proc)


def terminate_thread(thread, process, timeout=5):
    if thread.is_alive():
        if sys.platform == 'win32':
            print ("try to kill: {}".format(process.pid))
            killer = subprocess.Popen("taskkill /F /T /PID %i"%process.pid , shell=True)
            killer.wait()
        else:
            process.terminate()
    try:
        process.wait(timeout=5)
        log.info('== subprocess exited with rc =%d', process.returncode)
    except subprocess.TimeoutExpired:
        log.info('subprocess did not terminate in time')
    thread.join()
