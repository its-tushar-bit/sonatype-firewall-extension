/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.signature.ComponentWithSignatures;
import com.sonatype.clm.dto.model.signature.ComponentWithSignaturesList;
import com.sonatype.clm.dto.model.signature.FunctionSignature;
import com.sonatype.clm.dto.model.signature.Signature;
import com.sonatype.insight.brain.client.ErrorData;
import com.sonatype.insight.brain.client.ResultData;
import com.sonatype.insight.brain.client.UnsupportedServerVersionException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;
import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.ArtifactId;
import com.sonatype.insight.scan.model.Dependency;
import com.sonatype.insight.scan.model.DirectoryScanItem;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.nexus.git.utils.Environment.GitLabCI;
import com.sonatype.nexus.git.utils.commit.CommitHashFinderBuilder;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The primary set of tests for the {@link DefaultPolicyEvaluator}.
 *
 * This set of test cases powers not only the regular unit tests (see @{@link JUnitDefaultPolicyEvaluatorTest}, but also
 * the native image configuration and testing. This allows us to have one set of tests which covers all three cases.
 */
public abstract class DefaultPolicyEvaluatorTest
    extends AbstractPolicyEvaluatorTest
{
  @Rule
  public final AccessibleEnvironmentVariables environmentVariables = new AccessibleEnvironmentVariables();

  private PolicyEvaluationDAO policyEvaluationDAO;

  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  private ApplicationDAO applicationDAO;

  private LabelDAO labelDAO;

  private ComponentLabelDAO componentLabelDAO;

  @Before
  public void before() {
    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);
    automaticApplicationsConfigurationDAO = lookup(AutomaticApplicationsConfigurationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    labelDAO = lookup(LabelDAO.class);
    componentLabelDAO = lookup(ComponentLabelDAO.class);
  }

  @Test
  public void testRun_ServerDown() throws Exception {
    getTestCLMServer().stop();

    try {
      tempEntity.newApplicationWithParent("the-app-id");

      List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
          "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
          "src/test/data/artifact.jar");
      withTestRunner(params)
          .expectFailExit()
          .expectErrorLog("The IQ Server " + insightServerUrl + " could not be contacted")
          .doPolicyEvaluationRun();
    }
    finally {
      getTestCLMServer().start();
    }
  }

  @Test
  public void testRun_InvalidAppId() throws Exception {
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("The application ID the-app-id is invalid.")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_InvalidAuthentication() throws Exception {
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "user:pass", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("The IQ Server " + insightServerUrl + " rejected the supplied credentials.")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_InvalidAuthorization() throws Exception {
    tempEntity.newUser("user");
    List<String> params = ImmutableList.of("-s", insightServerUrl, //
        "-a", "user:" + TemporaryEntity.USER_PASSWORD_CLEAR, //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("The application ID the-app-id is invalid.")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_MultiAuthenticationModesEnabled() throws Exception {
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "user:pass", "--pki-authentication", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("Only one mode of authentication can be enabled at a time"
            + ", --authentication and --pki-authentication are mutually exclusive.")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_PkiAuthenticationMode() throws Exception {
    List<String> params = ImmutableList.of("-s", insightServerUrl, "--pki-authentication", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("The IQ Server " + insightServerUrl + " rejected the supplied credentials.")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_NoViolations() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectPolicyEvaluationResult(newPolicyEvaluationResultForOneComponent())
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_ContainerTargetIsNotCheckedForFileExists() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "container://registry/image:tag");
    withTestRunner(params)
        .expectFailExit(2) // due to a scanning error
        .expectPolicyEvaluationResult(newPolicyEvaluationResultForOneComponent())
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_ContainerTargetIsNotCheckedForFileExists_IgnoreScanningFailure() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), "-E", //
        "container:registry/image:tag");
    withTestRunner(params)
        .expectPolicyEvaluationResult(newPolicyEvaluationResultForOneComponent())
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_ContainerTargetIsNotCheckedForFileExists_ScanningFailure() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");
    File jsonFile = new File(tempDir.getRoot(), "not-yet-existent/results.json");

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(),
        "-r", jsonFile.getAbsolutePath(), "container:registry/image:tag");
    withTestRunner(params)
        .expectFailExit(2) // due to a scanning error
        .doPolicyEvaluationRun();

    ErrorData resultData = JsonUtils.parse(Files.readAllBytes(jsonFile.toPath()), ErrorData.class);
    assertThat(resultData.isScanningError).isTrue();
    assertThat(resultData.isSystemError).isFalse();
    assertThat(resultData.errorMessage).isEqualTo("Scanning errors encountered");
  }

  @Test
  public void testRun_SomeViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicy(app.getId(), "Policy Name", Action.ID_WARN, 10);

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");

    PolicyEvaluationResult expectedPolicyEvaluationResult = newPolicyEvaluationResultForOneComponent();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);

    withTestRunner(params)
        .expectInfoLog("Policy Action: Warning")
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(Policy Name)")
        .expectPolicyEvaluationResult(expectedPolicyEvaluationResult)
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_EffectiveActionIsMostSevere() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicy(app.getId(), "Policy 1", Action.ID_WARN, 9);
    createPolicy(app.getId(), "Policy 2", Action.ID_FAIL, 5);
    createPolicy(app.getId(), "Policy 3", Action.ID_WARN, 2);

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");

    PolicyEvaluationResult expectedPolicyEvaluationResult = newPolicyEvaluationResultForOneComponent();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);
    expectedPolicyEvaluationResult.setSeverePolicyViolationCount(4);
    expectedPolicyEvaluationResult.setModeratePolicyViolationCount(4);

    withTestRunner(params)
        .expectFailExit()
        .expectInfoLog("Policy Action: Failure")
        .expectPolicyEvaluationResult(expectedPolicyEvaluationResult)
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(Policy 1)") //
        .expectErrorLog("The IQ Server reports policy failing due to \nPolicy(Policy 2)") //
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(Policy 3)")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_FailOnWarn() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicy(app.getId(), "TestPolicy", Action.ID_WARN, 9);

    List<String> params1 = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");

    PolicyEvaluationResult expectedPolicyEvaluationResult = newPolicyEvaluationResultForOneComponent();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);

    withTestRunner(params1)
        .expectInfoLog("Policy Action: Warning")
        .expectPolicyEvaluationResult(expectedPolicyEvaluationResult)
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(TestPolicy)");

    logOutput.clear();

    List<String> params2 = ImmutableList.<String>builder().addAll(params1).add("-w").build();
    withTestRunner(params2)
        .expectFailExit()
        .expectInfoLog("Policy Action: Warning")
        .expectPolicyEvaluationResult(expectedPolicyEvaluationResult)
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(TestPolicy)")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_PassWhenIgnoreSystemExceptions() throws Exception {
    getTestCLMServer().stop();

    try {
      tempEntity.newApplicationWithParent("the-app-id");

      List<String> params1 = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
          "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
          "src/test/data/artifact.jar");
      withTestRunner(params1).expectFailExit()
          .expectErrorLog("The IQ Server " + insightServerUrl + " could not be contacted").doPolicyEvaluationRun();

      logOutput.clear();

      List<String> params2 = ImmutableList.<String>builder().addAll(params1).add("-e").build();
      // The evaluator will still throw an exit exception in the case where the -e flag is passed in as true
      // The exception will have exit status code 0 such that it will "pass" in a CI
      withTestRunner(params2).expectErrorLog("The IQ Server " + insightServerUrl + " could not be contacted")
          .expectExitExceptionButSuccessExit().doPolicyEvaluationRun();
    }
    finally {
      getTestCLMServer().start();
    }
  }

  @Test
  public void testRun_ReportUrl() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectInfoLog("The detailed report can be viewed online at " + insightServerUrl
            + "ui/links/application/the-app-id/report/SCAN-ID")
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_Scan() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.singletonList("com.sonatype"), Collections.emptyList());

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);
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
  public void testRun_Scan_Manifest() throws Exception {
    tempEntity.newApplicationWithParent("the-app-id");
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-D", "poetryManifestScanningEnabled=true", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/manifest/");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);
    assertThat(scan).isNotNull();

    assertThat(scan.getItems()).hasSize(22).allSatisfy(item -> {
          assertThat(item.getPath()).isNotNull();
          assertThat(item.getSha1()).isNotNull();
          assertThat(item.getContentType()).isNotNull();
          assertThat(item.getContent()).isNotNull();
        }
    );
  }

  @Test
  public void testRun_ScanWithBaseDirAdjustsComponentPath() throws Exception {
    // given an application
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.singletonList("com.sonatype"), Collections.emptyList());

    // when we run the CLI with required parameters and a base directory
    List<String> params = ImmutableList.of(
        "-s", insightServerUrl,
        "-a", "admin:admin123",
        "-i", "the-app-id",
        "--output-directory", tempDir.getRoot().getAbsolutePath(),
        "-b", "src/test",
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();
    // and we read into the resulting scan file
    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);

    // then we find one artifact is reported
    assertThat(scan.getItems()).hasSize(1);
    ScanItem jar = scan.getItems().get(0);
    // and its path is relative to the base dir
    assertThat(jar.getPath()).isEqualTo("data/artifact.jar");
    // and its sha1 remains the same
    assertThat(jar.getSha1()).isEqualTo("87cf012929052d02c3f1");
  }

  @Test
  public void testRun_GlobalProprietaryConfigOverriddenByClient() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.singletonList("com.overridden"), Collections.emptyList());

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-D", "proprietaryPackages=com.sonatype", //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);
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
  public void testRun_GlobalProprietaryConfigRegexOverriddenByClient() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.emptyList(),
        Collections.singletonList("com.overridden.*"));

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-D", "proprietaryRegexes=com.sonatype.*", //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);
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
  public void testRun_SetScanStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-t", Stage.ID_RELEASE, //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    assertThat(policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), Stage.ID_RELEASE)).isNotNull();
  }

  @Test
  public void testRun_DefaultScanStage() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    assertThat(policyEvaluationDAO.getLastByApplicationIdAndStageId(app.getId(), Stage.ID_BUILD)).isNotNull();
  }

  @Test
  public void testRun_JsonExport() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    File jsonFile = new File(tempDir.getRoot(), "not-yet-existent/results.json");
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-r", jsonFile.getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    ResultData resultData = JsonUtils.parse(Files.readAllBytes(jsonFile.toPath()), ResultData.class);
    assertThat(resultData.scanId).isEqualTo("SCAN-ID");
    assertThat(resultData.applicationId).isEqualTo(app.getPublicId());
    assertThat(resultData.reportDataUrl).isNotNull();
    assertThat(resultData.reportHtmlUrl).isNotNull();
    assertThat(resultData.reportPdfUrl).isNotNull();
    assertThat(resultData.policyEvaluationResult.getTotalComponentCount()).isEqualTo(1);
  }

  @Test
  public void testRun_JsonExportWithPolicyViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicy(app.getId(), "Policy 1", Action.ID_WARN, 9);
    createPolicy(app.getId(), "Policy 2", Action.ID_FAIL, 5);
    createPolicy(app.getId(), "Policy 3", Action.ID_WARN, 2);

    File jsonFile = new File(tempDir.getRoot(), "not-yet-existent/results.json");
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-r", jsonFile.getAbsolutePath(), //
        "src/test/data/artifact.jar");

    PolicyEvaluationResult expectedPolicyEvaluationResult = newPolicyEvaluationResultForOneComponent();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);
    expectedPolicyEvaluationResult.setSeverePolicyViolationCount(4);
    expectedPolicyEvaluationResult.setModeratePolicyViolationCount(4);

    // Check result
    withTestRunner(params)
        .expectFailExit()
        .expectInfoLog("Policy Action: Failure")
        .expectPolicyEvaluationResult(expectedPolicyEvaluationResult)
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(Policy 1)") //
        .expectErrorLog("The IQ Server reports policy failing due to \nPolicy(Policy 2)") //
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(Policy 3)")
        .doPolicyEvaluationRun();

    // Check exported JSON
    ResultData resultData = JsonUtils.parse(Files.readAllBytes(jsonFile.toPath()), ResultData.class);
    assertThat(resultData.scanId).isEqualTo("SCAN-ID");
    assertThat(resultData.applicationId).isEqualTo(app.getPublicId());
    assertThat(resultData.reportDataUrl).isNotNull();
    assertThat(resultData.reportHtmlUrl).isNotNull();
    assertThat(resultData.reportPdfUrl).isNotNull();
    assertThat(resultData.policyEvaluationResult.getTotalComponentCount()).isEqualTo(1);
    assertThat(resultData.policyEvaluationResult.getCriticalComponentCount()).isEqualTo(4);
    assertThat(resultData.policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(4);
    assertThat(resultData.policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(4);
    assertThat(resultData.policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(4);
  }

  @Test
  public void testRun_JsonExportWithPolicyViolationsAndNotifications() throws Exception {
    // Creating some notifications for the policies
    Role role = tempEntity.newRole(true, Permission.WRITE, Permission.EVALUATE_COMPONENT);
    Notification roleNotification = new RoleNotification(role.getId(), role.getName(), Stage.ID_BUILD);
    Webhook webhook = tempEntity.newWebhook(Stream.of(WebhookEventType.POLICY_ALERT)
        .collect(Collectors.toCollection(HashSet::new)));
    Notification webhookNotification = new WebhookNotification(webhook.getId(), Stage.ID_BUILD);
    Notification jiraNotification = new JiraNotification("PROJECT_KEY", 1000, Stage.ID_BUILD);

    // Creating apps and policies
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    createPolicyWithNotifications(app.getId(), "Policy 1", Action.ID_WARN, 9, roleNotification);
    createPolicyWithNotifications(app.getId(), "Policy 2", Action.ID_FAIL, 5, webhookNotification);
    createPolicyWithNotifications(app.getId(), "Policy 3", Action.ID_WARN, 2, jiraNotification);

    // Executing CLI
    File jsonFile = new File(tempDir.getRoot(), "not-yet-existent/results.json");
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-r", jsonFile.getAbsolutePath(), //
        "src/test/data/artifact.jar");

    PolicyEvaluationResult expectedPolicyEvaluationResult = newPolicyEvaluationResultForOneComponent();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);
    expectedPolicyEvaluationResult.setSeverePolicyViolationCount(4);
    expectedPolicyEvaluationResult.setModeratePolicyViolationCount(4);

    // Check result
    withTestRunner(params)
        .expectFailExit()
        .expectInfoLog("Policy Action: Failure")
        .expectPolicyEvaluationResult(expectedPolicyEvaluationResult)
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(Policy 1)") //
        .expectErrorLog("The IQ Server reports policy failing due to \nPolicy(Policy 2)") //
        .expectWarnLog("The IQ Server reports policy warning due to \nPolicy(Policy 3)")
        .doPolicyEvaluationRun();

    // Check exported JSON
    ResultData resultData = JsonUtils.parse(Files.readAllBytes(jsonFile.toPath()), ResultData.class);
    assertThat(resultData.policyEvaluationResult.getTotalComponentCount()).isEqualTo(1);
    assertThat(resultData.policyEvaluationResult.getCriticalComponentCount()).isEqualTo(4);
    assertThat(resultData.policyEvaluationResult.getCriticalPolicyViolationCount()).isEqualTo(4);
    assertThat(resultData.policyEvaluationResult.getSeverePolicyViolationCount()).isEqualTo(4);
    assertThat(resultData.policyEvaluationResult.getModeratePolicyViolationCount()).isEqualTo(4);

    // Check notifications are part of evaluation result
    checkNotificationActionTypeExists(resultData, NotifyActionType.TARGET_TYPE_ROLE);
    checkNotificationActionTypeExists(resultData, NotifyActionType.TARGET_TYPE_WEBHOOK);
    checkNotificationActionTypeExists(resultData, NotifyActionType.TARGET_TYPE_JIRA);
  }

  private void checkNotificationActionTypeExists(ResultData resultData, String notificationType) {
    assertThat(resultData.policyEvaluationResult.getAlerts().stream()
        .flatMap(policyAlert -> policyAlert.getActions().stream())
        .anyMatch(action -> notificationType.equals(action.getTargetType())))
        .isTrue();
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

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "@" + paramFile1.getAbsolutePath(), "@" + paramFile2.getAbsolutePath());
    withTestRunner(params)
        .expectPolicyEvaluationResult(newPolicyEvaluationResultForOneComponent())
        .doPolicyEvaluationRun();
  }

  @Test
  public void testRun_AutoAppCreationEnabled() throws Exception {
    Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setOrganizationId(org.getId());
    automaticApplicationsConfigurationDAO.setEnabled(true);

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "non-existent-app-public-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectPolicyEvaluationResult(newPolicyEvaluationResultForOneComponent())
        .doPolicyEvaluationRun();

    Application app = applicationDAO.getByPublicId("non-existent-app-public-id");
    assertThat(app).isNotNull();
    applicationDAO.delete(app);
  }

  @Test
  public void testRun_AutoAppCreationEnabled_orgIdProvided() throws Exception {
    Organization org = tempEntity.newOrganization();
    automaticApplicationsConfigurationDAO.setOrganizationId("non-existent-org-id");
    automaticApplicationsConfigurationDAO.setEnabled(true);

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "non-existent-app-public-id", "-O", org.getId(),
        "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectPolicyEvaluationResult(newPolicyEvaluationResultForOneComponent())
        .doPolicyEvaluationRun();

    Application app = applicationDAO.getByPublicId("non-existent-app-public-id");
    assertThat(app).isNotNull();
    assertThat(app.getOrganizationId()).isEqualTo(org.getId());
    applicationDAO.delete(app);
  }

  @Test
  public void testRun_AutoAppCreationEnabled_orgIdProvided_appExistsInDifferentOrg() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("app-in-org", org.getId());
    automaticApplicationsConfigurationDAO.setOrganizationId("non-existent-org-id");
    automaticApplicationsConfigurationDAO.setEnabled(true);
    Organization org2 = tempEntity.newOrganization();

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", app.getPublicId(), "-O", org2.getId(),
        "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("The application ID app-in-org is invalid for organization ID " + org2.getId() + ".")
        .doPolicyEvaluationRun();

    applicationDAO.delete(app);
  }

  @Test
  public void testRun_AutoAppCreationDisabled() throws Exception {
    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "non-existent-app-public-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");
    withTestRunner(params)
        .expectFailExit()
        .expectErrorLog("The application ID non-existent-app-public-id is invalid.")
        .doPolicyEvaluationRun();
  }

  private File findScanFile() {
    File scanOutputDir = new File(tempDir.getRoot().getAbsolutePath());

    File[] scanFiles = scanOutputDir.listFiles(file -> file.getName().startsWith("scan-"));
    assertThat(scanFiles).hasSize(1);

    return scanFiles[0];
  }

  @Test
  public void testRun_ServerVersionRequired() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", app.getPublicId(), "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "src/test/data/artifact.jar");

    VersionService versionService = getCLMServer().getInstance(VersionService.class);
    String savedServerVersion = versionService.getVersion();

    // Verify older server version. There should be an exception because the client requires a minimal server version
    // that is newer than the server version.
    String olderServerVersion = decrementVersion(AbstractPolicyEvaluator.MINIMAL_SERVER_VERSION_REQUIRED);
    versionService.setVersion(olderServerVersion);
    try {
      String expectedMessage = "The IQ Server version " + olderServerVersion
          + " is not compatible. Supported IQ server versions are "
          + AbstractPolicyEvaluator.MINIMAL_SERVER_VERSION_REQUIRED + " or newer.";
      withTestRunner(params)
          .expectFailExit()
          .expectErrorLog(expectedMessage)
          .expectException(UnsupportedServerVersionException.class, expectedMessage)
          .expectErrorLog(expectedMessage)
          .doPolicyEvaluationRun();
    }
    finally {
      versionService.setVersion(savedServerVersion);
    }

    // Verify newer server version. There should be no exceptions because the client requires a minimal server version
    // that is older than the server version.
    String newerServerVersion = incrementVersion(AbstractPolicyEvaluator.MINIMAL_SERVER_VERSION_REQUIRED);
    versionService.setVersion(newerServerVersion);
    try {
      withTestRunner(params)
          .doPolicyEvaluationRun();
    }
    finally {
      versionService.setVersion(savedServerVersion);
    }
  }

  @Test
  public void testRun_ScanWithCommitHashFromEnvironmentVariable() throws Exception {
    final String commitHash = "COMMIT_HASH_FROM_ENV_VAR";
    environmentVariables.set("GIT_COMMIT", commitHash);

    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.singletonList("com.sonatype"), Collections.emptyList());

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-m", "src/test/data/metadata.json", "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);

    environmentVariables.clear("GIT_COMMIT");
    assertThat(scan).isNotNull();
    assertThat(scan.getMetadata().getCommitHash()).isEqualTo(commitHash);
  }

  @Test
  public void testRun_ScanWithCommitHashFromLocalGitRepository() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    tempEntity.newProprietaryConfig(app.getId(), Collections.singletonList("com.sonatype"), Collections.emptyList());

    List<String> params = ImmutableList.of("-s", insightServerUrl, "-a", "admin:admin123", //
        "-i", "the-app-id", "--output-directory", tempDir.getRoot().getAbsolutePath(), //
        "-m", "src/test/data/metadata.json", "src/test/data/artifact.jar");
    withTestRunner(params)
        .doPolicyEvaluationRun();

    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);

    assertThat(scan).isNotNull();
    // by default this should always be running in the context of a checked out git repo, so we will manually look
    // up the commit hash and make sure it matches what is found in the scanner
    Optional<String> commitHash = new CommitHashFinderBuilder()
        .withEnvironmentVariableDefault()
        .withEnvironmentVariableNamed(GitLabCI.COMMIT_HASH_ENV_VARIABLE)
        .withGitRepo()
        .build()
        .tryGetCommitHash();
    assertThat(scan.getMetadata().getCommitHash()).isEqualTo(commitHash.get());
  }

  @Test
  public void testRun_ScanWithModule() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");
    List<String> params = ImmutableList.of(
        "-s", insightServerUrl,
        "-a", "admin:admin123",
        "-i", app.getPublicId(),
        "--output-directory", tempDir.getRoot().getAbsolutePath(),
        "src/test/data/module"
    );

    withTestRunner(params).doPolicyEvaluationRun();

    File scanFile = findScanFile();
    Scan scan = scanReader.read(scanFile);
    assertThat(scan).isNotNull();
    assertThat(scan.getItems()).hasSize(3);
    ScanItem moduleXmlFile = findScanItemByPath(scan.getItems(), "module/sonatype-clm/module.xml");
    assertThat(moduleXmlFile).isNotNull();
    DirectoryScanItem artifact =
        (DirectoryScanItem) findScanItemByPath(scan.getItems(), "module/test2-1.0-SNAPSHOT.jar");
    assertThat(artifact.getItems()).extracting(ScanItem::getPath).containsExactly(
        "META-INF/MANIFEST.MF",
        "META-INF/maven/org.example/test2/pom.xml",
        "META-INF/maven/org.example/test2/pom.properties",
        "Main.class"
    );
    assertThat(artifact.getIds()).hasSize(1);
    ArtifactId artifactId = artifact.getIds().get(0);
    assertThat(artifactId.getKind()).isEqualTo("maven");
    assertThat(artifactId.getId()).isEqualTo("org.example:test2:1.0-SNAPSHOT");
    ProjectScanItem project = (ProjectScanItem) findScanItemById(scan.getItems(), "org.example:test2:jar:1.0-SNAPSHOT");
    List<Dependency> dependencies = project.getDependencies();
    assertThat(dependencies).hasSize(4);
    assertDependency(dependencies, "org.apache.httpcomponents:httpclient:jar:4.5.13",
        "org.apache.httpcomponents:httpcore:jar:4.4.13",
        "commons-logging:commons-logging:jar:1.2",
        "commons-codec:commons-codec:jar:1.11"
    );
    assertDependency(dependencies, "org.apache.httpcomponents:httpcore:jar:4.4.13");
    assertDependency(dependencies, "commons-logging:commons-logging:jar:1.2");
    assertDependency(dependencies, "commons-codec:commons-codec:jar:1.11");
  }

  @Test
  public void testRun_WithCallFlowAnalysisAndNoVulnerableSignatures() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    hdsRespondWith(new ComponentWithSignaturesList()).atUri("rest/component/signatures/vulnerability");

    List<String> params = ImmutableList.of(
        "-s", insightServerUrl,
        "-a", "admin:admin123",
        "-i", app.getPublicId(),
        "--output-directory", tempDir.getRoot().getAbsolutePath(),
        "-c",
        "src/test/data/artifact.jar"
    );

    withTestRunner(params).doPolicyEvaluationRun();

    Label label = labelDAO.getByLabelWithHierarchy("Security-Reachable", app.getId());
    if (label != null) {
      try (TransactionContext tx = componentLabelDAO.createTransactionContext()) {
        assertThat(componentLabelDAO.getByLabelIdAndOwnerIds(tx, label.getId(), Collections.singleton(app.getId())))
            .isEmpty();
      }
    }
  }

  @Test
  public void testRun_WithCallFlowAnalysisAndVulnerableSignatures() throws Exception {
    Application app = tempEntity.newApplicationWithParent("the-app-id");

    Signature signature = new Signature();
    signature.setAnchor("test-anchor");
    signature
        .setFunctionSignature(new FunctionSignature("com/sonatype/insight/scan/cli/Main.main([Ljava/lang/String;)V"));

    ComponentWithSignatures component =
        new ComponentWithSignatures("pkg:maven/ch.qos.logback/logback-access@0.6", signature);

    ComponentWithSignaturesList componentWithSignaturesList =
        new ComponentWithSignaturesList(Collections.singletonList(component));

    hdsRespondWith(componentWithSignaturesList).atUri("rest/component/signatures/vulnerability");

    List<String> params = ImmutableList.of(
        "-s", insightServerUrl,
        "-a", "admin:admin123",
        "-i", app.getPublicId(),
        "--output-directory", tempDir.getRoot().getAbsolutePath(),
        "-c",
        "src/test/data/artifact.jar"
    );

    // This creates labels at root org level. We need to remove them after the test.
    Set<String> oldLabelIds = labelDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID).stream().map(Label::getId)
        .collect(Collectors.toSet());
    try {
      withTestRunner(params).doPolicyEvaluationRun();

      Label label = labelDAO.getByLabelWithHierarchy("Security-Reachable", app.getId());
      assertThat(label).isNotNull();

      if (label != null) {
        try (TransactionContext tx = componentLabelDAO.createTransactionContext()) {
          assertThat(componentLabelDAO.getByLabelIdAndOwnerIds(tx, label.getId(), Collections.singleton(app.getId())))
              .isNotEmpty();
        }
      }
    }
    finally {
      List<Label> labels = labelDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
      labels.stream().filter(label -> !oldLabelIds.contains(label.getId())).forEach(labelDAO::delete);
    }
  }

  private ScanItem findScanItemByPath(Collection<ScanItem> scanItems, String path) {
    return findScanItemByPredicate(scanItems, scanItem -> path.equals(scanItem.getPath()));
  }

  private ScanItem findScanItemById(Collection<ScanItem> scanItems, String id) {
    return findScanItemByPredicate(scanItems, scanItem -> id.equals(scanItem.getId()));
  }

  private ScanItem findScanItemByPredicate(Collection<ScanItem> scanItems, Predicate<ScanItem> predicate) {
    if (scanItems == null) {
      return null;
    }
    return scanItems.stream()
        .filter(predicate)
        .findFirst()
        .orElse(null);
  }

  private void assertDependency(Collection<Dependency> dependencies, String id, String... childIds) {
    Dependency dependency = dependencies.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null);
    assertThat(dependency).isNotNull();
    assertThat(dependency.getDependencies()).extracting(Dependency::getId).containsExactlyInAnyOrder(childIds);
  }

  private String decrementVersion(String versionAsString) {
    int dotAt = versionAsString.indexOf(".");
    if (dotAt > 0) {
      return (Integer.parseInt(versionAsString.substring(0, dotAt)) - 1) + "." + versionAsString.substring(dotAt + 1);
    }
    return String.valueOf(Integer.parseInt(versionAsString) - 1);
  }

  private String incrementVersion(String versionAsString) {
    int dotAt = versionAsString.indexOf(".");
    if (dotAt > 0) {
      return (Integer.parseInt(versionAsString.substring(0, dotAt)) + 1) + "." + versionAsString.substring(dotAt + 1);
    }
    return String.valueOf(Integer.parseInt(versionAsString) + 1);
  }
}
