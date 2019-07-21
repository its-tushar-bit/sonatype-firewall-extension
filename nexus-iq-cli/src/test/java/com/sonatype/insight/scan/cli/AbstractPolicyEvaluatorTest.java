/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.test.InjectedTest;
import com.sonatype.insight.test.LogOutput;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.owasp.dependencycheck.Engine;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractPolicyEvaluatorTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  protected static TestCLMServer testInsightServer;

  @Rule
  public LogOutput logOutput = new LogOutput(1, AbstractPolicyEvaluatorTest.class, Engine.class);

  @Inject
  protected DefaultPolicyEvaluator evaluator;

  @Inject
  protected ScanReader scanReader;

  protected String insightServerUrl;

  @Override
  @Before
  public void setUp() throws Exception {
    System.out.println("--- " + testName.getMethodName() + " ------------------------");
    try {
      String outDir = tmpDir.newFolder("scan").getAbsolutePath();
      String timestamp = "20130610-171959";
      System.setProperty(AbstractPolicyEvaluatorCli.PROP_OUTPUT_DIRECTORY, outDir);
      System.setProperty(AbstractPolicyEvaluatorCli.PROP_START_TIME, timestamp);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    super.setUp();

    startInsightServer();

    // return a valid report zip file when asked
    testInsightServer.getHdsServer().respondWith(new File("src/test/resources/small-report.zip"))
        .atUri("rest/application/analysis/SCAN-ID");

    insightServerUrl = testInsightServer.getCLMServer().getClientConfiguration().getServerUrl();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    stopInsightServer();
  }

  protected void startInsightServer() throws Exception {
    if (testInsightServer != null) {
      return;
    }

    testInsightServer = new TestCLMServer(false, null, new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setImportRefrencePoliciesFromHDS(false);
      }
    });
    testInsightServer.start();
  }

  protected static void stopInsightServer() throws Exception {
    if (testInsightServer == null) {
      return;
    }

    testInsightServer.stop();
    testInsightServer = null;
  }

  protected ScanReceipt newReceipt() {
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("the-scan-id");
    receipt.setReportUrl("the-report-url");
    receipt.setPdfUrl("the-pdf-url");
    receipt.setTimeToReport(0L);
    return receipt;
  }

  protected ApplicationSummaryList newApplicationSummaryList(String publicId, String name) {
    ApplicationSummary appSummary = new ApplicationSummary();
    appSummary.setPublicId(publicId);
    appSummary.setName(name);
    ApplicationSummaryList appSummaryList = new ApplicationSummaryList();
    appSummaryList.getApplicationSummaries().add(appSummary);
    return appSummaryList;
  }

  protected void assertLogSummary(PolicyEvaluationResult expectedPolicyEvalutionResult) {
    assertThat(logOutput).atInfoLevel()
        .contains(String.format("Number of components affected: %s critical, %s severe, %s moderate",
            expectedPolicyEvalutionResult.getCriticalComponentCount(),
            expectedPolicyEvalutionResult.getSevereComponentCount(),
            expectedPolicyEvalutionResult.getModerateComponentCount()))
        .contains(String.format("Number of open policy violations: %s critical, %s severe, %s moderate",
            expectedPolicyEvalutionResult.getCriticalPolicyViolationCount(),
            expectedPolicyEvalutionResult.getSeverePolicyViolationCount(),
            expectedPolicyEvalutionResult.getModeratePolicyViolationCount()))
        .contains(String.format("Number of grandfathered policy violations: %s",
            expectedPolicyEvalutionResult.getGrandfatheredPolicyViolationCount()));
  }
}
