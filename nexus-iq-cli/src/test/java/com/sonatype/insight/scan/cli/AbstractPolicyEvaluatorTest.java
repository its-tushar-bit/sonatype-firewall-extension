/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.test.InjectedTest;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public abstract class AbstractPolicyEvaluatorTest
    extends InjectedTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  protected ByteArrayOutputStream log;

  @Inject
  protected PolicyEvaluator<Parameters> evaluator;

  @Inject
  protected ScanReader scanReader;

  @Override
  @Before
  public void setUp() throws Exception {
    System.out.println("--- " + testName.getMethodName() + " ------------------------");
    try {
      String outDir = tmpDir.newFolder("scan").getAbsolutePath();
      String timestamp = "20130610-171959";
      System.setProperty(AbstractPolicyEvaluatorCli.PROP_OUTPUT_DIRECTORY, outDir);
      System.setProperty(AbstractPolicyEvaluatorCli.PROP_START_TIME, timestamp);
      log = new ByteArrayOutputStream(1024 * 4);
      resetLogback();
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    super.setUp();
  }

  @After
  public void resetLogger() {
    // close file appenders to allow deletion of tmp files
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();
  }

  protected void resetLogback() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    try {
      lc.reset();
      new ContextInitializer(lc).autoConfig();
      PatternLayoutEncoder encoder = new PatternLayoutEncoder();
      encoder.setContext(lc);
      encoder.setPattern("[%level] %m%n");
      encoder.start();
      OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<ILoggingEvent>();
      appender.setContext(lc);
      appender.setEncoder(encoder);
      appender.setOutputStream(log);
      appender.setName("mem");
      appender.start();
      lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(appender);
    }
    catch (Exception je) {
      je.printStackTrace();
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
  }

  protected void assertLog(String line) throws Exception {
    List<String> logLines = Arrays.asList(log.toString("UTF-8").split("\r\n|\r|\n"));
    assertTrue("Could not locate log: " + line, logLines.contains(line));
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
