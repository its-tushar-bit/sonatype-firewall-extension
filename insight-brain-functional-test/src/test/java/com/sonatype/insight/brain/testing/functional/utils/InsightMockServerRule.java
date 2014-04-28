/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.utils;

import java.io.File;

import com.sonatype.insight.mock.InsightMockServer;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class InsightMockServerRule
    implements TestRule
{
  private InsightMockServer saas;
  private int saasPort = 8090;
  private File saasWork = new File("target/mock-saas-work/");

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement()
    {
      @Override
      public void evaluate() throws Throwable {
        startIfRequired();
        try {
          base.evaluate();
        }
        finally {
          saas.stop();
          saas = null;
        }
      }
    };
  }

  private void startIfRequired() {
    if (saas != null) {
      return;
    }

    saas = new InsightMockServer();
    saas.setHttpPort(saasPort);
    saas.setJsonResponseDirectory(new File(saasWork, "json"));
    saas.setZipResponseDirectory(new File(saasWork, "zip"));

    try {
      saas.setResponseForURI("rest/ci/scan", "{\"scanId\": \"blah\",\"timeToReport\": 0}", 200);
      saas.setResponseForURI("rest/ci/report?scanId=blah",
          FileUtils.readFileToByteArray(new File(getClass().getResource("/report.zip").toURI())), 200);
      saas.start();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void setResponseForURI(String uri, Object body, int status) {
    saas.setResponseForURI(uri, body, status);
  }

  void setResponseForURI(String uri, String body, int status) {
    saas.setResponseForURI(uri, body, status);
  }
}
