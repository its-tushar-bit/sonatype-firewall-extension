/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.client.ResultData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.ScanWriter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DefaultPolicyEvaluatorTest
    extends AbstractPolicyEvaluatorTest
{
  @Test
  public void testRun_ServerDown() throws Exception {
    stopInsightServer();

    tempEntity.newApplicationWithParent("the-app-id");

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError(startsWith("The IQ Server " + insightServerUrl + " could not be contacted"));
    }
  }

  @Test
  public void testRun_InvalidAppId() throws Exception {
    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The application ID the-app-id is invalid.");
    }
  }

  @Test
  public void testRun_InvalidAuthentication() throws Exception {
    Parameters params = new Parameters("-s", insightServerUrl, "-a", "user:pass", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The IQ Server " + insightServerUrl + " rejected the supplied credentials.");
    }
  }

  @Test
  public void testRun_InvalidAuthorization() throws Exception {
    tempEntity.newUser("user");
    Parameters params = new Parameters("-s", insightServerUrl, //
        "-a", "user:" + TemporaryEntity.USER_PASSWORD_CLEAR, //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The application ID the-app-id is invalid.");
    }
  }

  @Test
  public void testRun_MultiAuthenticationModesEnabled() throws Exception {
    Parameters params = new Parameters("-s", insightServerUrl, "-a", "user:pass", "--pki-authentication", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
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
    Parameters params = new Parameters("-s", insightServerUrl, "--pki-authentication", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");

    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The IQ Server " + insightServerUrl + " rejected the supplied credentials.");
    }
  }

  @Test
  public void testRun_NoViolations() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);
    assertLogSummary(new PolicyEvaluationResult());
  }

  @Test
  public void testRun_SomeViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicy(app.getId(), "Policy Name", Action.ID_WARN, 10);

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);

    logOutput.assertInfo("Policy Action: Warning");
    PolicyEvaluationResult expectedPolicyEvaluationResult = new PolicyEvaluationResult();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);
    assertLogSummary(expectedPolicyEvaluationResult);
    logOutput.assertWarn(startsWith("The IQ Server reports policy warning due to \nPolicy(Policy Name)"));
  }

  @Test
  public void testRun_EffectiveActionIsMostSevere() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicy(app.getId(), "Policy 1", Action.ID_WARN, 9);
    createPolicy(app.getId(), "Policy 2", Action.ID_FAIL, 5);
    createPolicy(app.getId(), "Policy 3", Action.ID_WARN, 2);

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");

    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(1, ex.getExitCode());
    }
    logOutput.assertInfo("Policy Action: Failure");
    PolicyEvaluationResult expectedPolicyEvaluationResult = new PolicyEvaluationResult();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);
    expectedPolicyEvaluationResult.setSeverePolicyViolationCount(4);
    expectedPolicyEvaluationResult.setModeratePolicyViolationCount(4);
    assertLogSummary(expectedPolicyEvaluationResult);
    logOutput.assertWarn(startsWith("The IQ Server reports policy warning due to \nPolicy(Policy 1)"));
    logOutput.assertError(startsWith("The IQ Server reports policy failing due to \nPolicy(Policy 2)"));
    logOutput.assertWarn(startsWith("The IQ Server reports policy warning due to \nPolicy(Policy 3)"));
  }

  @Test
  public void testRun_FailOnWarn() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicy(app.getId(), "TestPolicy", Action.ID_WARN, 9);

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("Policy Action: Warning");
    PolicyEvaluationResult expectedPolicyEvaluationResult = new PolicyEvaluationResult();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);
    assertLogSummary(expectedPolicyEvaluationResult);
    logOutput.assertWarn(startsWith("The IQ Server reports policy warning due to \nPolicy(TestPolicy)"));

    logOutput.clear();

    params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "-w", //
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(1, ex.getExitCode());
    }
    logOutput.assertInfo("Policy Action: Warning");
    assertLogSummary(expectedPolicyEvaluationResult);
    logOutput.assertWarn(startsWith("The IQ Server reports policy warning due to \nPolicy(TestPolicy)"));
  }

  @Test
  public void testRun_PassWhenIgnoreSystemExceptions() throws Exception {
    stopInsightServer();

    tempEntity.newApplicationWithParent("the-app-id");

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(1, ex.getExitCode());
    }

    logOutput.assertError(startsWith("The IQ Server " + insightServerUrl + " could not be contacted"));

    logOutput.clear();

    params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "-e", //
        "src/test/data/artifact.jar");
    // The evaluator will still throw an exit exception in the case where the -e flag is passed in as true
    // The exception will have exit status code 0 such that it will "pass" in a CI
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException ex) {
      assertEquals(0, ex.getExitCode());
    }

    logOutput.assertError(startsWith("The IQ Server " + insightServerUrl + " could not be contacted"));
  }

  @Test
  public void testRun_ReportUrl() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);
    logOutput.assertInfo("The detailed report can be viewed online at " + insightServerUrl
        + "ui/links/application/the-app-id/report/SCAN-ID");
  }

  @Test
  public void testRun_Scan() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.singletonList("com.sonatype"), Collections.emptyList());

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);

    File scanFile = findScanFile(params);
    Scan scan = scanReader.read(scanFile);
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
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.singletonList("com.overridden"), Collections.emptyList());

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "-D", "proprietaryPackages=com.sonatype", //
        "src/test/data/artifact.jar");
    evaluator.run(params);

    File scanFile = findScanFile(params);
    Scan scan = scanReader.read(scanFile);
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
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.emptyList(),
        Collections.singletonList("com.overridden.*"));

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "-D", "proprietaryRegexes=com.sonatype.*", //
        "src/test/data/artifact.jar");
    evaluator.run(params);

    File scanFile = findScanFile(params);
    Scan scan = scanReader.read(scanFile);
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
  public void testRun_SetScanStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "-t", Stage.ID_RELEASE, //
        "src/test/data/artifact.jar");
    evaluator.run(params);

    assertThat(new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(), Stage.ID_RELEASE),
        is(notNullValue()));
  }

  @Test
  public void testRun_DefaultScanStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);

    assertThat(new PolicyEvaluationDAO().getLastByApplicationIdAndStageId(app.getId(), Stage.ID_BUILD),
        is(notNullValue()));
  }

  @Test
  public void testRun_JsonExport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    File jsonFile = new File(tmpDir.getRoot(), "not-yet-existent/results.json");
    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "-r", jsonFile.getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);
    
    ResultData resultData = JsonUtils.parse(Files.readAllBytes(jsonFile.toPath()), ResultData.class);
    assertThat(resultData.scanId, is("SCAN-ID"));
    assertThat(resultData.applicationId, is(app.getPublicId()));
    assertThat(resultData.reportDataUrl, is(notNullValue()));
    assertThat(resultData.reportHtmlUrl, is(notNullValue()));
    assertThat(resultData.reportPdfUrl, is(notNullValue()));
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

    tempEntity.newApplicationWithParent("the-app-id");

    List<String> paramFileLines1 = new ArrayList<>();
    paramFileLines1.add("-i");
    paramFileLines1.add("the-app-id");
    File paramFile1 = tmpDir.newFile();
    List<String> paramFileLines2 = new ArrayList<>();
    paramFileLines2.add("--stage");
    paramFileLines2.add(Stage.ID_RELEASE);
    paramFileLines2.add("src/test/data/artifact.jar");
    File paramFile2 = tmpDir.newFile();
    // We use the default character encoding to write the parameter files because JCommander uses the default character
    // encoding to read the file.
    FileUtils.writeLines(paramFile1, paramFileLines1, "\n");
    FileUtils.writeLines(paramFile2, paramFileLines2, "\n");

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "@" + paramFile1.getAbsolutePath(), "@" + paramFile2.getAbsolutePath());
    evaluator.run(params);
    assertLogSummary(new PolicyEvaluationResult());
  }

  @Test
  public void testRun_AutoAppCreationEnabled() throws Exception {
    Organization org = tempEntity.newOrganization();
    AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO = new AutomaticApplicationsConfigurationDAO();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "non-existent-app-public-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    evaluator.run(params);

    ApplicationDAO appDAO = new ApplicationDAO();
    Application app = appDAO.getByPublicId("non-existent-app-public-id");
    assertThat(app, is(notNullValue()));
    appDAO.delete(app);

    assertLogSummary(new PolicyEvaluationResult());
  }

  @Test
  public void testRun_AutoAppCreationDisabled() throws Exception {
    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "non-existent-app-public-id", "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    try {
      evaluator.run(params);
      fail("Expected error");
    }
    catch (ExitException e) {
      logOutput.assertError("The application ID non-existent-app-public-id is invalid.");
    }
  }

  private File findScanFile(Parameters params) {
    File scanOutputDir = params.getOutputDirectory();

    File[] scanFiles = scanOutputDir.listFiles(file -> file.getName().startsWith("scan-"));
    assertThat(scanFiles.length, is(1));

    return scanFiles[0];
  }

  private void createPolicy(String ownerId, String policyName, String actionId, int threatLevel) {
    Policy policy = new Policy();
    policy.setName(policyName);
    policy.setOwnerId(ownerId);
    Condition condition = new Condition(MatchStateConditionType.ID, "is");
    condition.setValue(MatchState.EXACT.getId());
    Constraint constraint = new Constraint();
    constraint.setName("test constraint");
    constraint.addCondition(condition);
    policy.addConstraint(constraint);
    policy.setAction(Stage.ID_BUILD, actionId);
    policy.setThreatLevel(threatLevel);
    tempEntity.newPolicy(policy);
  }

  @Test
  public void testRun_ServerVersionRequired() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    Parameters params = new Parameters("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", app.getPublicId(), "--output-directory", tmpDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");

    VersionService versionService = testInsightServer.getCLMServer().getInjector().getInstance(VersionService.class);
    String currentServerVersion = versionService.getVersion();
    currentServerVersion = currentServerVersion.replace("-SNAPSHOT", "");

    // Verify older server version. There should be no exceptions because the client requires a minimal server version
    // that is older than the current server version.
    String olderServerVersion = decrementVersion(currentServerVersion);
    String savedMinimalServerVersionRequired = evaluator.getMinimalServerVersionRequired();
    try {
      evaluator.setMinimalServerVersionRequired(olderServerVersion);
      evaluator.run(params);
    }
    finally {
      evaluator.setMinimalServerVersionRequired(savedMinimalServerVersionRequired);
    }

    // Verify newer server version. There should be an exception because the client requires a minimal server version
    // that is newer than the current server version.
    String newerServerVersion = incrementVersion(currentServerVersion);
    evaluator.setMinimalServerVersionRequired(newerServerVersion);
    try {
      evaluator.run(params);
      fail("Expected exception");
    }
    catch (ExitException expected) {
      String expectedMessage = String.format(
          "The IQ Server version %s is not compatible. Supported IQ server versions are %s or newer.",
          currentServerVersion, newerServerVersion);
      assertThat(expected.getMessage(), endsWith(expectedMessage));
      logOutput.assertError(expectedMessage);
    }
    finally {
      evaluator.setMinimalServerVersionRequired(savedMinimalServerVersionRequired);
    }
  }

  private String decrementVersion(String versionAsString) {
    int dotAt = versionAsString.indexOf(".");
    if (dotAt > 0) {
      return (Integer.valueOf(versionAsString.substring(0, dotAt)) - 1) + "." + versionAsString.substring(dotAt + 1);
    }
    return String.valueOf(Integer.valueOf(versionAsString) - 1);
  }

  private String incrementVersion(String versionAsString) {
    int dotAt = versionAsString.indexOf(".");
    if (dotAt > 0) {
      return (Integer.valueOf(versionAsString.substring(0, dotAt)) + 1) + "." + versionAsString.substring(dotAt + 1);
    }
    return String.valueOf(Integer.valueOf(versionAsString) + 1);
  }
}
