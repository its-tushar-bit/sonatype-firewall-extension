/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.Arrays;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.ScanWriter;

import com.google.inject.Binder;
import org.apache.http.client.HttpResponseException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyEvaluatorTest
    extends AbstractPolicyEvaluatorTest
{

  private RestClient restClient;

  private ArgumentCaptor<Configuration> httpConfig;

  @Override
  public void configure(Binder binder) {
    RestClientFactory restClientFactory = mock(RestClientFactory.class);
    binder.bind(RestClientFactory.class).toInstance(restClientFactory);
    httpConfig = ArgumentCaptor.forClass(Configuration.class);
    restClient = mock(RestClient.class);
    when(restClientFactory.newRestCLIClient(httpConfig.capture())).thenReturn(restClient);
  }

  @Test
  public void testServerDown() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenThrow(new HttpResponseException(503, "Maintenance"));
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-p", "localhost:8888", "-U",
        "proxyuser:proxypass", "-i", "the-app-id", "-a", "user:pass", "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The IQ Server is down for maintenance, please try again later.");
      assertEquals("http://localhost:8070/", httpConfig.getValue().getServerUrl());
      assertEquals("localhost", httpConfig.getValue().getProxyHost());
      assertEquals(8888, httpConfig.getValue().getProxyPort());
      assertEquals("proxyuser", httpConfig.getValue().getProxyAuth().getUsername());
      assertEquals("proxypass", new String(httpConfig.getValue().getProxyAuth().getPassword()));
      assertEquals("user", httpConfig.getValue().getServerAuth().getUsername());
      assertEquals("pass", new String(httpConfig.getValue().getServerAuth().getPassword()));
    }
  }

  @Test
  public void testInvalidAppId() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The application ID the-app-id is invalid.");
    }
  }

  @Test
  public void testInvalidAuthc() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenThrow(new HttpResponseException(401, "Bad Authc"));
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "-a", "user:pass",
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The IQ Server http://localhost:8070/ rejected the supplied credentials.");
    }
  }

  @Test
  public void testInvalidAuthz() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenThrow(new HttpResponseException(403, "Bad Authz"));
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "-a", "user:pass",
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The IQ Server http://localhost:8070/ rejected the supplied credentials.");
    }
  }

  @Test
  public void testMultiAuthenticationModesEnabled() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "--pki-authentication", "-a", "user:pass",
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError(
          "Only one mode of authentication can be enabled at a time, --authentication and --pki-authentication are mutually exclusive.");
    }
  }

  @Test
  public void testPkiAuthenticationMode() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenThrow(new HttpResponseException(401, "Bad Authc"));
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "--pki-authentication",
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The IQ Server http://localhost:8070/ rejected the supplied credentials.");
      // verify that basic auth credentials are not set
      assertNull(httpConfig.getValue().getServerAuth());
    }
  }

  @Test
  public void testNoViolations() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("Summary of policy violations: 0 critical, 0 severe, 0 moderate");
  }

  @Test
  public void testSomeViolations() throws Exception {
    PolicyAlert alert = new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10), Arrays.asList(new Action(
        Action.ID_WARN)));
    PolicyEvaluationResult eval = new PolicyEvaluationResult();
    eval.setAffectedComponentCount(6);
    eval.setCriticalComponentCount(1);
    eval.setSevereComponentCount(2);
    eval.setModerateComponentCount(3);
    eval.setAlerts(Arrays.asList(alert));
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(eval);
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("Policy Action: Warning");
    logOutput.assertInfo("Summary of policy violations: 1 critical, 2 severe, 3 moderate");
    logOutput.assertWarn("The IQ Server reports policy warning due to \nPolicy(Policy Name) null");
  }

  @Test
  public void testEffectiveActionIsMostSevere() throws Exception {
    PolicyAlert alert1 = new PolicyAlert(new PolicyFact("policy1", "Policy 1", 10), Arrays.asList(new Action(
        Action.ID_WARN)));
    PolicyAlert alert2 = new PolicyAlert(new PolicyFact("policy2", "Policy 2", 10), Arrays.asList(new Action(
        Action.ID_FAIL)));
    PolicyAlert alert3 = new PolicyAlert(new PolicyFact("policy3", "Policy 3", 10), Arrays.asList(new Action(
        Action.ID_WARN)));
    PolicyEvaluationResult eval = new PolicyEvaluationResult();
    eval.setAffectedComponentCount(6);
    eval.setCriticalComponentCount(1);
    eval.setSevereComponentCount(2);
    eval.setModerateComponentCount(3);
    eval.setAlerts(Arrays.asList(alert1, alert2, alert3));
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(eval);
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");

    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(1, ex.getExitCode());
    }
    logOutput.assertInfo("Policy Action: Failure");
    logOutput.assertInfo("Summary of policy violations: 1 critical, 2 severe, 3 moderate");
    logOutput.assertWarn("The IQ Server reports policy warning due to \nPolicy(Policy 1) null");
    logOutput.assertError("The IQ Server reports policy failing due to \nPolicy(Policy 2) null");
    logOutput.assertWarn("The IQ Server reports policy warning due to \nPolicy(Policy 3) null");
  }

  @Test
  public void testFailOnWarn() throws Exception {
    PolicyAlert alert = new PolicyAlert(new PolicyFact("policy1", "Policy 1", 10), Arrays.asList(new Action(
        Action.ID_WARN)));

    PolicyEvaluationResult eval = new PolicyEvaluationResult();
    eval.setAffectedComponentCount(6);
    eval.setCriticalComponentCount(1);
    eval.setAlerts(Arrays.asList(alert));
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(eval);
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");

    evaluator.run(params);
    logOutput.assertInfo("Policy Action: Warning");
    logOutput.assertInfo("Summary of policy violations: 1 critical, 0 severe, 0 moderate");
    logOutput.assertWarn("The IQ Server reports policy warning due to \nPolicy(Policy 1) null");

    params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar", "-w",
        "true");

    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(1, ex.getExitCode());
    }
    logOutput.assertInfo("Policy Action: Warning");
    logOutput.assertInfo("Summary of policy violations: 1 critical, 0 severe, 0 moderate");
    logOutput.assertWarn("The IQ Server reports policy warning due to \nPolicy(Policy 1) null");
  }

  @Test
  public void testPassWhenIgnoreSystemExceptions() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenThrow(new HttpResponseException(503, ""));

    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");

    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(1, ex.getExitCode());
    }

    logOutput.assertError("The IQ Server is down for maintenance, please try again later.");

    params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar", "-e",
        "true");

    // The evaluator will still throw an exit exception in the case where the -g flag is passed in as true
    // The exception will have exit status code 0 such that it will "pass" in a CI
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(0, ex.getExitCode());
    }

    logOutput.assertError("The IQ Server is down for maintenance, please try again later.");
  }

  @Test
  public void testReportUrl() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("The detailed report can be viewed online at http://localhost:8070/the-report-url");
  }

  @Test
  public void testScan() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.sonatype"));
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.getProprietaryConfigForApplicationEvaluation("the-app-id")).thenReturn(proprietaryConfig);
    when(restClient.uploadScan(eq("the-app-id"), scanFile.capture(), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    assertNotNull(scanFile.getValue());
    Scan scan = scanReader.read(scanFile.getValue());
    assertNotNull(scan);
    ScanSummary summary = scan.getSummary();
    assertNotNull(summary);
    assertNotNull(summary.getStartTime());
    assertNotNull(summary.getEndTime());
    assertNotNull(summary.getClientInfo());
    assertNotNull(summary.getClientInfo().getProperty("java.version"));
    ScanConfiguration config = scan.getConfiguration();
    assertNotNull(config);
    assertEquals(ScanWriter.PROPERTY_MASKED, config.getString("", "proprietaryPackages"));
    assertEquals(1, scan.getItems().size());
    ScanItem jar = scan.getItems().get(0);
    assertEquals("artifact.jar", jar.getPath());
    assertEquals("87cf012929052d02c3f1", jar.getSha1());
    assertEquals(1, jar.getItems().size());
    for (ScanItem item : jar.getItems()) {
      assertNull(item.getPath());
      assertNotNull(item.getSha1());
      assertNotNull(item.getSha1JA001());
      assertEquals("proprietaryPackages", item.getNoPathReason());
    }
  }

  @Test
  public void testGlobalProprietaryConfigOverriddenByClient() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.overridden"));
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.getProprietaryConfigForApplicationEvaluation("the-app-id")).thenReturn(proprietaryConfig);
    when(restClient.uploadScan(eq("the-app-id"), scanFile.capture(), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-D", "proprietaryPackages=com.sonatype");
    evaluator.run(params);
    assertNotNull(scanFile.getValue());
    Scan scan = scanReader.read(scanFile.getValue());
    assertNotNull(scan);
    ScanConfiguration config = scan.getConfiguration();
    assertNotNull(config);
    assertEquals(ScanWriter.PROPERTY_MASKED, config.getString("", "proprietaryPackages"));
    assertEquals(1, scan.getItems().size());
    ScanItem jar = scan.getItems().get(0);
    assertEquals("artifact.jar", jar.getPath());
    assertEquals("87cf012929052d02c3f1", jar.getSha1());
    for (ScanItem item : jar.getItems()) {
      assertNull(item.getPath());
      assertNotNull(item.getSha1());
      assertNotNull(item.getSha1JA001());
      assertEquals("proprietaryPackages", item.getNoPathReason());
    }
  }

  @Test
  public void testGlobalProprietaryConfigRegexOverriddenByClient() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setRegexes(Arrays.asList("com.overridden.*"));
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.getProprietaryConfigForApplicationEvaluation("the-app-id")).thenReturn(proprietaryConfig);
    when(restClient.uploadScan(eq("the-app-id"), scanFile.capture(), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-D", "proprietaryRegexes=com.sonatype.*");
    evaluator.run(params);
    assertNotNull(scanFile.getValue());
    Scan scan = scanReader.read(scanFile.getValue());
    assertNotNull(scan);
    ScanConfiguration config = scan.getConfiguration();
    assertNotNull(config);
    assertEquals(ScanWriter.PROPERTY_MASKED, config.getString("", "proprietaryRegexes"));
    assertEquals(1, scan.getItems().size());
    ScanItem jar = scan.getItems().get(0);
    assertEquals("artifact.jar", jar.getPath());
    assertEquals("87cf012929052d02c3f1", jar.getSha1());
    for (ScanItem item : jar.getItems()) {
      assertNull(item.getPath());
      assertNotNull(item.getSha1());
      assertNotNull(item.getSha1JA001());
      assertEquals("proprietaryPackages", item.getNoPathReason());
    }
  }

  @Test
  public void testNoGlobalProprietaryConfig() throws Exception {
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.getProprietaryConfigForApplicationEvaluation("the-app-id"))
        .thenThrow(new HttpResponseException(404, "outdated"));
    when(restClient.uploadScan(eq("the-app-id"), scanFile.capture(), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertWarn("The IQ Server is outdated and does not provide configuration for proprietary components");
    assertNotNull(scanFile.getValue());
    Scan scan = scanReader.read(scanFile.getValue());
    assertNotNull(scan);
  }

  @Test
  public void testGlobalProprietaryConfigFailure() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    HttpResponseException expectedException = new HttpResponseException(500, "error");
    when(restClient.getProprietaryConfigForApplicationEvaluation("the-app-id")).thenThrow(expectedException);
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("Could not retrieve configuration for proprietary components from the IQ Server",
          expectedException);
    }
  }

  @Test
  public void testSetScanStage() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), anyString())).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-t", Stage.ID_RELEASE);
    evaluator.run(params);
    verify(restClient).evaluatePolicy("the-app-id", "the-scan-id", Stage.ID_RELEASE);
  }

  @Test
  public void testDefaultScanStage() throws Exception {
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), anyString())).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    verify(restClient).evaluatePolicy("the-app-id", "the-scan-id", Stage.ID_BUILD);
  }

  @Test
  public void testInvalidStage() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-t", "invalid-stage-id");
    assertNotNull("Invalid stage id was not detected", params.getError());
  }

  @Test
  public void testJsonExport() throws Exception {
    ScanReceipt receipt = newReceipt();
    when(restClient.getApplicationsForApplicationEvaluation()).thenReturn(
        newApplicationSummaryList("the-app-id", "My App"));
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE))).thenReturn(receipt);
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    File jsonFile = new File(tmpDir.getRoot(), "not-yet-existent/results.json");
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-r", jsonFile.getAbsolutePath());
    evaluator.run(params);
    verify(restClient).saveResults(eq("the-app-id"), eq(jsonFile), eq(receipt));
  }
}
