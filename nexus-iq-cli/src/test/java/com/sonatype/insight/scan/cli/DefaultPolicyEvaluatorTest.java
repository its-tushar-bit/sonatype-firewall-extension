/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpResponseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultPolicyEvaluatorTest
    extends AbstractPolicyEvaluatorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

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
  public void testRun_ServerDown() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenThrow(new HttpResponseException(503, "Maintenance"));
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
  public void testRun_InvalidAppId() throws Exception {
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
  public void testRun_InvalidAuthc() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenThrow(new HttpResponseException(401, "Bad Authc"));
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
  public void testRun_InvalidAuthz() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenThrow(new HttpResponseException(403, "Bad Authz"));
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
  public void testRun_MultiAuthenticationModesEnabled() throws Exception {
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
  public void testRun_PkiAuthenticationMode() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenThrow(new HttpResponseException(401, "Bad Authc"));
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
  public void testRun_NoViolations() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("Summary of policy violations: 0 critical, 0 severe, 0 moderate");
  }

  @Test
  public void testRun_SomeViolations() throws Exception {
    PolicyAlert alert = new PolicyAlert(new PolicyFact("policyId", "Policy Name", 10), Arrays.asList(new Action(
        Action.ID_WARN)));
    PolicyEvaluationResult eval = new PolicyEvaluationResult();
    eval.setAffectedComponentCount(6);
    eval.setCriticalComponentCount(1);
    eval.setSevereComponentCount(2);
    eval.setModerateComponentCount(3);
    eval.setAlerts(Arrays.asList(alert));
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_EffectiveActionIsMostSevere() throws Exception {
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
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_FailOnWarn() throws Exception {
    PolicyAlert alert = new PolicyAlert(new PolicyFact("policy1", "Policy 1", 10), Arrays.asList(new Action(
        Action.ID_WARN)));

    PolicyEvaluationResult eval = new PolicyEvaluationResult();
    eval.setAffectedComponentCount(6);
    eval.setCriticalComponentCount(1);
    eval.setAlerts(Arrays.asList(alert));
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_PassWhenIgnoreSystemExceptions() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenThrow(new HttpResponseException(503, ""));

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
  public void testRun_ReportUrl() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("The detailed report can be viewed online at http://localhost:8070/the-report-url");
  }

  @Test
  public void testRun_Scan() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.sonatype"));
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_GlobalProprietaryConfigOverriddenByClient() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.overridden"));
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_GlobalProprietaryConfigRegexOverriddenByClient() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setRegexes(Arrays.asList("com.overridden.*"));
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_GlobalProprietaryConfigFailure() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_SetScanStage() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
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
  public void testRun_DefaultScanStage() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), anyString())).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    verify(restClient).evaluatePolicy("the-app-id", "the-scan-id", Stage.ID_BUILD);
  }

  @Test
  public void testRun_JsonExport() throws Exception {
    ScanReceipt receipt = newReceipt();
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE))).thenReturn(receipt);
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    File jsonFile = new File(tmpDir.getRoot(), "not-yet-existent/results.json");
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-r", jsonFile.getAbsolutePath());
    evaluator.run(params);
    verify(restClient).saveResults(eq("the-app-id"), eq(jsonFile), eq(receipt));
  }

  @Test
  public void testRun_ParametersFromFile() throws Exception {
    // Verifies that (from the CLM-7494 user story):
    // - The argument file must use the JVM's default character encoding.
    // - The argument file can be mixed with explicit input file specifications on the CLI.
    // - There can be any number of argument files on the CLI.
    // - Arguments and their values must be on separate lines.
    // - Both short and long argument names are supported.
    // - File paths within the argument file are relative to the process' current directory, not the argument file.

    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_RELEASE)))
        .thenReturn(new PolicyEvaluationResult());

    List<String> paramFileLines1 = new ArrayList<>();
    paramFileLines1.add("-i");
    paramFileLines1.add("the-app-id");
    File paramFile1 = tempDir.newFile();
    List<String> paramFileLines2 = new ArrayList<>();
    paramFileLines2.add("--stage");
    paramFileLines2.add(Stage.ID_RELEASE);
    paramFileLines2.add("src/test/data/artifact.jar");
    File paramFile2 = tempDir.newFile();
    // We use the default character encoding to write the parameter files because JCommander uses the default character
    // encoding to read the file.
    FileUtils.writeLines(paramFile1, paramFileLines1, "\n");
    FileUtils.writeLines(paramFile2, paramFileLines2, "\n");
    Parameters params = new Parameters("-s", "http://localhost:8070/", "@" + paramFile1.getAbsolutePath(),
        "@" + paramFile2.getAbsolutePath());
    evaluator.run(params);
    logOutput.assertInfo("Summary of policy violations: 0 critical, 0 severe, 0 moderate");
  }

  @Test
  public void testRun_AutoAppCreationEnabled() throws Exception {
    when(restClient.verifyOrCreateApplication("non-existent-app-public-id")).thenReturn(true);
    when(restClient.uploadScan(eq("non-existent-app-public-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("non-existent-app-public-id"), eq("the-scan-id"), eq(Stage.ID_BUILD)))
        .thenReturn(new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "non-existent-app-public-id",
        "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("Summary of policy violations: 0 critical, 0 severe, 0 moderate");
  }

  @Test
  public void testRun_AutoAppCreationDisabled() throws Exception {
    when(restClient.verifyOrCreateApplication("non-existent-app-public-id")).thenReturn(false);
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "non-existent-app-public-id",
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The application ID non-existent-app-public-id is invalid.");
    }
  }
}
