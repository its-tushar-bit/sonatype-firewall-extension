/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseDetailsCache;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicenseDetailsCache;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.scan.cli.nativeimage.DefaultPolicyEvaluatorTestForNativeImageConfigGeneration;
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.test.LogOutput;

import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Rule;
import org.owasp.dependencycheck.Engine;

public abstract class AbstractPolicyEvaluatorTest
    extends AbstractBrainServiceTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(1, AbstractPolicyEvaluatorTest.class, Engine.class);

  protected DefaultPolicyEvaluator evaluator;

  protected ScanReader scanReader;

  protected String insightServerUrl;

  /**
   * The TestRunner class is responsible for executing the actual test against the subject with the given parameters,
   * and asserting the results such as exit code/exception, log output, etc...
   * Implementations:
   * <ul>
   *   <li>{@link PolicyEvaluatorTestRunner} is the main implementation for normal unit tests</li>
   *   <li>{@link DefaultPolicyEvaluatorTestForNativeImageConfigGeneration} is the implementation for generating config
   *   files for the native image tooling</li>
   *   <li>TODO {@link TBD} is the implementation for testing the native image binaries</li>
   * </ul>
   */
  protected PolicyEvaluatorTestRunner withTestRunner(final Parameters params) {
    return new PolicyEvaluatorTestRunner(params, evaluator, logOutput);
  }

  @Before
  public void setUp() throws Exception {
    System.out.println("--- " + testName.getMethodName() + " ------------------------");
    try {
      String outDir = tempDir.newFolder("scan").getAbsolutePath();
      String timestamp = "20130610-171959";
      System.setProperty(PolicyEvaluatorCli.PROP_OUTPUT_DIRECTORY, outDir);
      System.setProperty(PolicyEvaluatorCli.PROP_START_TIME, timestamp);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }

    // return a valid report zip file when asked
    getHdsServer().respondWith(new File("src/test/resources/small-report.zip"))
        .atUri("rest/application/analysis/SCAN-ID");

    insightServerUrl = getCLMServer().getClientConfiguration().getServerUrl();

    scanReader = getCLMServer().getInstance(ScanReader.class);
    evaluator = getCLMServer().getInstance(DefaultPolicyEvaluator.class);
  }

  protected List<Module> getBrainModules() {
    Module testModule = new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ProductLicense.class).to(TestProductLicense.class);
        bind(ProductLicenseDetailsCache.class).to(TestProductLicenseDetailsCache.class);
        bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
        bind(LicenseFingerprinter.class).to(TestLicenseFingerprinter.class);
        // unable to bind this class automatically during startup
        bind(Scanner.class).toInstance(new Scanner(null, null, null, null, null));
      }
    };
    return Arrays.asList(testModule);
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
}
