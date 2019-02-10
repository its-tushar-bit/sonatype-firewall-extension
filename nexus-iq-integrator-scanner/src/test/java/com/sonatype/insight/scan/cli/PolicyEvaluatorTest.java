/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.inject.Inject;

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
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.apache.http.client.HttpResponseException;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyEvaluatorTest
    extends InjectedTest
{
  @Rule
  public TestName testName = new TestName();

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput(1, PolicyEvaluator.class);

  @Inject
  private PolicyEvaluator evaluator;

  @Inject
  private ScanReader scanReader;

  private RestClient restClient;

  private ArgumentCaptor<Configuration> httpConfig;

  @Override
  @Before
  public void setUp() throws Exception {
    System.out.println("--- " + testName.getMethodName() + " ------------------------");
    try {
      String outDir = tmpDir.newFolder("scan").getAbsolutePath();
      String timestamp = "20130610-171959";
      System.setProperty(PolicyEvaluatorCli.PROP_OUTPUT_DIRECTORY, outDir);
      System.setProperty(PolicyEvaluatorCli.PROP_START_TIME, timestamp);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    super.setUp();
  }

  private ScanReceipt newReceipt() {
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("the-scan-id");
    receipt.setReportUrl("the-report-url");
    receipt.setPdfUrl("the-pdf-url");
    receipt.setTimeToReport(0L);
    return receipt;
  }

  @Override
  public void configure(Binder binder) {
    RestClientFactory restClientFactory = mock(RestClientFactory.class);
    binder.bind(RestClientFactory.class).toInstance(restClientFactory);
    httpConfig = ArgumentCaptor.forClass(Configuration.class);
    restClient = mock(RestClient.class);
    when(restClientFactory.newRestCIClient(httpConfig.capture())).thenReturn(restClient);
  }

  @Test
  public void testServerDown() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenThrow(new HttpResponseException(503, "Maintenance"));
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-p", "localhost:8888", "-U",
        "proxyuser:proxypass", "-i", "the-app-id", "src/test/data/artifact.jar");
    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params);
    });
    assertThat(logOutput).atErrorLevel().contains("The IQ Server is down for maintenance, please try again later.");
    assertThat(httpConfig.getValue().getServerUrl()).isEqualTo("http://localhost:8070/");
    assertThat(httpConfig.getValue().getProxyHost()).isEqualTo("localhost");
    assertThat(httpConfig.getValue().getProxyPort()).isEqualTo(8888);
    assertThat(httpConfig.getValue().getProxyAuth().getUsername()).isEqualTo("proxyuser");
    assertThat(new String(httpConfig.getValue().getProxyAuth().getPassword())).isEqualTo("proxypass");
  }

  @Test
  public void testInvalidAppId() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");
    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params);
    });
    assertThat(logOutput).atErrorLevel().contains("The application ID the-app-id is invalid.");
  }

  @Test
  public void testNoViolations() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    assertThat(logOutput).atInfoLevel().contains("Summary of policy violations: 0 critical, 0 severe, 0 moderate");
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
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(eval);
    Parameters params = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    assertThat(logOutput).atInfoLevel().contains("Policy Action: Warning")
        .contains("Summary of policy violations: 1 critical, 2 severe, 3 moderate").atWarnLevel()
        .contains("The IQ Server reports policy warning due to \nPolicy(Policy Name) null");
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
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(eval);
    Parameters params = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");

    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params);
    }).satisfies(e -> assertThat(e.getExitCode()).isOne());
    assertThat(logOutput).atInfoLevel().contains("Policy Action: Failure")
        .contains("Summary of policy violations: 1 critical, 2 severe, 3 moderate").atWarnLevel()
        .contains("The IQ Server reports policy warning due to \nPolicy(Policy 1) null").atErrorLevel()
        .contains("The IQ Server reports policy failing due to \nPolicy(Policy 2) null").atWarnLevel()
        .contains("The IQ Server reports policy warning due to \nPolicy(Policy 3) null");
  }

  @Test
  public void testPassWhenIgnoreSystemExceptions() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenThrow(new HttpResponseException(503, ""));

    Parameters params1 = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");

    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params1);
    }).satisfies(e -> assertThat(e.getExitCode()).isOne());

    assertThat(logOutput).atErrorLevel().contains("The IQ Server is down for maintenance, please try again later.");

    Parameters params2 = new Parameters(
        Stream.concat(Stream.of("-e", "true"), Stream.of(params1.getArgs())).toArray(String[]::new));

    // The evaluator will still throw an exit exception in the case where the -e flag is passed in as true
    // The exception will have exit status code 0 such that it will "pass" in a CI
    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params2);
    }).satisfies(e -> assertThat(e.getExitCode()).isZero());

    assertThat(logOutput).atErrorLevel().contains("The IQ Server is down for maintenance, please try again later.");
  }

  @Test
  public void testScan() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.sonatype"));
    ArgumentCaptor<File> scanFile = ArgumentCaptor.forClass(File.class);
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.getProprietaryConfigForApplicationEvaluation("the-app-id")).thenReturn(proprietaryConfig);
    when(restClient.uploadScan(eq("the-app-id"), scanFile.capture(), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    assertThat(scanFile.getValue()).isNotNull();
    Scan scan = scanReader.read(scanFile.getValue());
    assertThat(scan).isNotNull();
    ScanSummary summary = scan.getSummary();
    assertThat(summary).isNotNull();
    assertThat(summary.getStartTime()).isNotNull();
    assertThat(summary.getEndTime()).isNotNull();
    assertThat(summary.getClientInfo()).containsKey("java.version");
    ScanConfiguration config = scan.getConfiguration();
    assertThat(config).isNotNull();
    assertThat(config.getString("", "proprietaryPackages")).isEqualTo(ScanWriter.PROPERTY_MASKED);
    assertThat(scan.getItems()).hasSize(1);
    ScanItem jar = scan.getItems().get(0);
    assertThat(jar.getPath()).isEqualTo("artifact.jar");
    assertThat(jar.getSha1()).isEqualTo("87cf012929052d02c3f1");
    assertThat(jar.getItems()).hasSize(1);
    for (ScanItem item : jar.getItems()) {
      assertThat(item.getPath()).isNull();
      assertThat(item.getSha1()).isNotNull();
      assertThat(item.getSha1JA001()).isNotNull();
      assertThat(item.getNoPathReason()).isEqualTo("proprietaryPackages");
    }
  }

  @Test
  public void testGlobalProprietaryConfigOverriddenByClient() throws Exception {
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
    assertThat(scanFile.getValue()).isNotNull();
    Scan scan = scanReader.read(scanFile.getValue());
    assertThat(scan).isNotNull();
    ScanConfiguration config = scan.getConfiguration();
    assertThat(config).isNotNull();
    assertThat(config.getString("", "proprietaryPackages")).isEqualTo(ScanWriter.PROPERTY_MASKED);
    assertThat(scan.getItems()).hasSize(1);
    ScanItem jar = scan.getItems().get(0);
    assertThat(jar.getPath()).isEqualTo("artifact.jar");
    assertThat(jar.getSha1()).isEqualTo("87cf012929052d02c3f1");
    for (ScanItem item : jar.getItems()) {
      assertThat(item.getPath()).isNull();
      assertThat(item.getSha1()).isNotNull();
      assertThat(item.getSha1JA001()).isNotNull();
      assertThat(item.getNoPathReason()).isEqualTo("proprietaryPackages");
    }
  }

  @Test
  public void testGlobalProprietaryConfigRegexOverriddenByClient() throws Exception {
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
    assertThat(scanFile.getValue()).isNotNull();
    Scan scan = scanReader.read(scanFile.getValue());
    assertThat(scan).isNotNull();
    ScanConfiguration config = scan.getConfiguration();
    assertThat(config).isNotNull();
    assertThat(config.getString("", "proprietaryRegexes")).isEqualTo(ScanWriter.PROPERTY_MASKED);
    assertThat(scan.getItems()).hasSize(1);
    ScanItem jar = scan.getItems().get(0);
    assertThat(jar.getPath()).isEqualTo("artifact.jar");
    assertThat(jar.getSha1()).isEqualTo("87cf012929052d02c3f1");
    for (ScanItem item : jar.getItems()) {
      assertThat(item.getPath()).isNull();
      assertThat(item.getSha1()).isNotNull();
      assertThat(item.getSha1JA001()).isNotNull();
      assertThat(item.getNoPathReason()).isEqualTo("proprietaryPackages");
    }
  }

  @Test
  public void testGlobalProprietaryConfigFailure() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    HttpResponseException expectedException = new HttpResponseException(500, "error");
    when(restClient.getProprietaryConfigForApplicationEvaluation("the-app-id")).thenThrow(expectedException);
    Parameters params = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");
    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> {
      evaluator.run(params);
    });
    assertThat(logOutput).atErrorLevel()
        .contains("Could not retrieve configuration for proprietary components from the IQ Server", expectedException);
  }

  @Test
  public void testSetScanStage() throws Exception {
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
  public void testDefaultScanStage() throws Exception {
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE)))
        .thenReturn(newReceipt());
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), anyString())).thenReturn(
        new PolicyEvaluationResult());
    Parameters params = new Parameters("-s", "http://localhost:87/", "-i", "the-app-id", "src/test/data/artifact.jar");
    evaluator.run(params);
    verify(restClient).evaluatePolicy("the-app-id", "the-scan-id", Stage.ID_BUILD);
  }

  @Test
  public void testInvalidStage() throws Exception {
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-t", "invalid-stage-id");
    assertThat(params.getError()).as("Invalid stage id was not detected").isNotNull();
  }

  @Test
  public void testSaveReportBundle() throws Exception {
    ScanReceipt receipt = newReceipt();
    when(restClient.verifyOrCreateApplication("the-app-id")).thenReturn(true);
    when(restClient.uploadScan(eq("the-app-id"), any(File.class), eq(ClientScanType.SONATYPE))).thenReturn(receipt);
    when(restClient.evaluatePolicy(eq("the-app-id"), eq("the-scan-id"), eq(Stage.ID_BUILD))).thenReturn(
        new PolicyEvaluationResult());
    File reportBundleFile = new File(tmpDir.getRoot(), "not-yet-existent/reportBundle.zip");
    Parameters params = new Parameters("-s", "http://localhost:8070/", "-i", "the-app-id",
        "src/test/data/artifact.jar", "-b", reportBundleFile.getAbsolutePath());
    evaluator.run(params);
    verify(restClient).saveReportBundle(eq("the-app-id"), eq(receipt.getScanId()), eq(reportBundleFile));
  }
}
