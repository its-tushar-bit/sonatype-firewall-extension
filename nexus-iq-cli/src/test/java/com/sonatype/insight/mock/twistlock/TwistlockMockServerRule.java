/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.twistlock;

import org.junit.rules.ExternalResource;

public class TwistlockMockServerRule
    extends ExternalResource
{
  protected TwistlockMockServer twistlockMockServer;

  @Override
  protected void before() throws Throwable {
    start();
  }

  @Override
  protected void after() {
    stop();
  }

  public void start() throws Exception {
    long start = System.currentTimeMillis();

    System.out.println("Starting TwistlockMockServer");
    twistlockMockServer = new TwistlockMockServer();
    twistlockMockServer.start();
    System.out.println("Started TwistlockMockServer on port " + getHttpPort() + " in "
        + (System.currentTimeMillis() - start) + " ms.");
  }

  public void stop() {
    long start = System.currentTimeMillis();
    if (twistlockMockServer != null) {
      twistlockMockServer.stop();
      twistlockMockServer = null;
    }

    System.out.println("Stopped twistlockMockServer in " + (System.currentTimeMillis() - start) + " ms.");
  }

  public void setResponseForURI(String uri, Object body, int status) {
    twistlockMockServer.setResponseForURI(uri, body, status);
  }

  public void reset() {
    twistlockMockServer.reset();
  }

  public int getHttpPort() {
    return twistlockMockServer.getHttpPort();
  }
}
