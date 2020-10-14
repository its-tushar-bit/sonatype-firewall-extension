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
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
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

  protected ScanReader scanReader;

  protected String insightServerUrl;

  /**
   * The TestRunner class is responsible for executing the actual test against the subject with the given parameters,
   * and asserting the results such as exit code/exception, log output, etc...
   * Implementations:
   * <ul>
   *   <li>{@link JUnitPolicyEvaluatorTestRunner} is the main implementation for normal unit tests</li>
   *   <li><a href="http://github.com/sonatype/native-image-nexus-iq-cli>sonatype/native-image-nexus-iq-cli/</a>
   *   contains the native-image implementations
   * </ul>
   */
  protected abstract AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params);

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
    File smallReportZip = new File(getClass().getClassLoader().getResource("small-report.zip").getFile());
    getHdsServer().respondWith(smallReportZip)
        .atUri("rest/application/analysis/SCAN-ID");

    insightServerUrl = getCLMServer().getClientConfiguration().getServerUrl();

    scanReader = getCLMServer().getInstance(ScanReader.class);
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
