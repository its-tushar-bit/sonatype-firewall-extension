/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.pullrequestcreationservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.dto.GithubUser;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class ManualPullRequestCreationServiceTest
    extends AbstractComponentTest
{
  private static final String DEFAULT_SCAN_ID = "scan-id";

  private static final String DEFAULT_VERSION = "1.0.0";

  private static final String DEFAULT_REMEDIATION_VERSION = "2.0.0";

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Inject
  private PullRequestBranchNameGenerator branchNameGenerator;

  @Inject
  private ManualPullRequestCreationService manualPrService;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Rule
  public LogOutput logOutput = new LogOutput(ManualPullRequestCreationService.class);

  private Application application;

  private ComponentIdentifier mavenComponent;

  private Stage stage;

  @Override
  public void configure(Binder binder) {
    binder.bind(ComponentInfoService.class).toInstance(mockComponentInfoService);
    super.configure(binder);
  }

  @Before
  public void setup() throws PlexusCipherException {
    application = tempEntity.newApplicationWithParent("abc123");
    mavenComponent = ComponentIdentifier.createMavenCoordinates("group", "artifact", DEFAULT_VERSION);
    stage = new Stage("build");
    setBaseUrl("http://localhost:1122");

    GithubUser githubUser = new GithubUser();
    githubUser.setGlobalId("userId");
    gitService.stubFor(get("/api/v3/user").withHeader("Authorization", matching("token token"))
        .willReturn(aResponse().withStatus(200).withBody(JsonUtils.format(githubUser))));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/[^/]+/[^/]+"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": true }")));

    //set up source control configuration
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl(gitService.baseUrl() + "/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }

  @Test
  public void testCreateManualRemediationPullRequest_success() throws IOException {
    ComponentIdentifier mavenComponentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "group", "artifact", DEFAULT_VERSION);
    String branchName = branchNameGenerator.getBranchName(application, mavenComponent, DEFAULT_REMEDIATION_VERSION);

    //setup policy evaluation and mock component info data
    setupPolicyEvaluationAndViolation();
    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        mavenComponentIdentifier, "build", "Sonatype", DEFAULT_SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(setupComponentVersionInfoDTO());

    PullRequestSubmissionResultDTO result = manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        mavenComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true
    );
    assertThat(result.id()).isNotEmpty();
    //verify that the event is created
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.getById(result.id());
    assertThat(sourceControlEvent.getEventType()).isEqualTo(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(application.getId());
    assertThat(sourceControlEvent.getScanId()).isEqualTo(DEFAULT_SCAN_ID);
    assertThat(sourceControlEvent.getComponentIdentifier()).isEqualTo(mavenComponent);
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(branchName);
    assertThat(sourceControlEvent.getRemediationVersion()).isEqualTo(DEFAULT_REMEDIATION_VERSION);
    assertThat(sourceControlEvent.getStageTypeId()).isEqualTo(stage.getStageTypeId());
    assertThat(sourceControlEvent.getInitiator()).isEqualTo("manual request");
  }

  @Test
  public void testCreateManualRemediationPullRequest_branchExistComplete() {
    setupPolicyEvaluationAndViolation();
    //branch exist
    SourceControlEvent sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setBranchName(
        branchNameGenerator.getBranchName(application, mavenComponent, DEFAULT_REMEDIATION_VERSION));
    sourceControlEvent.setApplicationId(application.getId());
    sourceControlEvent.setEventType(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEvent.setScanId(DEFAULT_SCAN_ID);
    sourceControlEventDAO.insert(sourceControlEvent);

    assertThatThrownBy(() -> manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        mavenComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true
    )).isInstanceOf(BadRequestException.class)
        .hasMessageContaining(
            "A remediation event for branch name '" +
                branchNameGenerator.getBranchName(application, mavenComponent, DEFAULT_REMEDIATION_VERSION) +
                "' already exists for application '" + application.getPublicId() +
                "'. Please choose a different branch name.");
  }

  @Test
  public void testCreateManualRemediationPullRequest_branchExistError() throws Exception {
    setupPolicyEvaluationAndViolation();
    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        mavenComponent, "build", "Sonatype", DEFAULT_SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(setupComponentVersionInfoDTO());

    //branch exist
    String branchName = branchNameGenerator.getBranchName(application, mavenComponent, DEFAULT_REMEDIATION_VERSION);
    SourceControlEvent sourceControlEvent = new SourceControlEvent();
    sourceControlEvent.setBranchName(branchName);
    sourceControlEvent.setApplicationId(application.getId());
    sourceControlEvent.setEventType(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    sourceControlEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    sourceControlEvent.setScanId(DEFAULT_SCAN_ID);
    sourceControlEventDAO.insert(sourceControlEvent);

    PullRequestSubmissionResultDTO result = manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        mavenComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true
    );
    assertThat(result.id()).isNotEmpty();
    //verify that the event is created
    SourceControlEvent resultEvent = sourceControlEventDAO.getById(result.id());
    assertThat(resultEvent.getEventType()).isEqualTo(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    assertThat(resultEvent.getApplicationId()).isEqualTo(application.getId());
    assertThat(resultEvent.getScanId()).isEqualTo(DEFAULT_SCAN_ID);
    assertThat(resultEvent.getComponentIdentifier()).isEqualTo(mavenComponent);
    assertThat(resultEvent.getBranchName()).isEqualTo(branchName);
    assertThat(resultEvent.getRemediationVersion()).isEqualTo(DEFAULT_REMEDIATION_VERSION);
    assertThat(resultEvent.getStageTypeId()).isEqualTo(stage.getStageTypeId());
    assertThat(resultEvent.getInitiator()).isEqualTo("manual request");
  }

  @Test
  public void testCreateManualRemediationPullRequest_noApplicableVersionChange() {
    ComponentIdentifier mavenComponentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "group", "artifact", DEFAULT_VERSION);
    setupPolicyEvaluationAndViolation();
    // Component version info with empty version changes (no applicable version)
    ComponentVersionInfoDTO versionInfoDTO = new ComponentVersionInfoDTO();
    versionInfoDTO.remediation = new ApiComponentRemediationValueDTO();
    versionInfoDTO.remediation.versionChanges = new ArrayList<>();
    versionInfoDTO.remediation.suggestedVersionChange = null;

    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        mavenComponentIdentifier, "build", "Sonatype", DEFAULT_SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(versionInfoDTO);

    assertThatThrownBy(() -> manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        mavenComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true
    )).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No applicable version change found for component " +
            ComponentDisplayNameUtil.fromIdentifier(mavenComponent));

    //no event was created
    List<SourceControlEvent> events = sourceControlEventDAO.getAll();
    assertThat(events).isEmpty();
    assertThat(logOutput).atDebugLevel().contains("Attempt to create manual PR");
  }

  @Test
  public void testCreateManualRemediationPullRequest_versionMismatch() {
    ComponentIdentifier mavenComponentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "group", "artifact", DEFAULT_VERSION);
    String requestedVersion = "1.5.0"; // Different from the remediation version

    setupPolicyEvaluationAndViolation();
    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        mavenComponentIdentifier, "build", "Sonatype", DEFAULT_SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(setupComponentVersionInfoDTO());

    assertThatThrownBy(() -> manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        mavenComponent,
        requestedVersion,
        "Sonatype",
        true
    )).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Target version " + requestedVersion + " does not match the applicable version change " +
            DEFAULT_REMEDIATION_VERSION + " for component " +
            ComponentDisplayNameUtil.fromIdentifier(mavenComponent));

    List<SourceControlEvent> events = sourceControlEventDAO.getAll();
    assertThat(events).isEmpty();
  }

  @Test
  public void testCreateManualRemediationPullRequest_notLatestScan() {
    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "old-scan-id", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(), "new-scan-id", time2);

    assertThatThrownBy(() -> manualPrService.createManualRemediationPullRequest(
        application.getId(),
        "old-scan-id",
        mavenComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true
    )).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("The provided scan ID does not match the latest evaluation for the stage");

    List<SourceControlEvent> events = sourceControlEventDAO.getAll();
    assertThat(events).isEmpty();
  }

  @Test
  public void testCreateManualRemediationPullRequest_nonDirectDependency() {
    //setup policy evaluation and mock component info data
    setupPolicyEvaluationAndViolation();

    assertThatThrownBy(() -> manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        mavenComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        false)) // non-direct dependency
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining(
            String.format("Manual pull request creation is not eligible for application %s component %s in stage %s",
                application.getPublicId(), ComponentDisplayNameUtil.fromIdentifier(mavenComponent),
                stage.getStageTypeId()));

    List<SourceControlEvent> events = sourceControlEventDAO.getAll();
    assertThat(events).isEmpty();
  }

  @Test
  public void testCreateManualRemediationPullRequest_innerSourceComponent_LATEST() throws IOException {
    ComponentIdentifier innerSourceComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example", "innerSource", DEFAULT_VERSION);

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent);
    InnerSourceApplication innerSourceApp = tempEntity.newInnerSourceApplication(
        packageUrl.getPackageUrl(), application);
    tempEntity.newInnerSourceVersion(innerSourceApp, DEFAULT_REMEDIATION_VERSION, StageTypes.RELEASE.getId());

    String branchName =
        branchNameGenerator.getBranchName(application, innerSourceComponent, DEFAULT_REMEDIATION_VERSION);

    setupPolicyEvaluationAndViolation();

    ComponentVersionInfoDTO versionInfoDTO = setupInnerSourceComponentVersionInfoDTO(DEFAULT_REMEDIATION_VERSION,
        ApiVersionChangeOptionType.INNER_SOURCE_LATEST);
    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        innerSourceComponent, "build", "Sonatype", DEFAULT_SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(versionInfoDTO);

    PullRequestSubmissionResultDTO result = manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        innerSourceComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true
    );

    assertThat(result.id()).isNotEmpty();
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.getById(result.id());
    assertThat(sourceControlEvent.getEventType()).isEqualTo(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(application.getId());
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(branchName);
    assertThat(sourceControlEvent.getRemediationVersion()).isEqualTo(DEFAULT_REMEDIATION_VERSION);

    assertThat(logOutput).atDebugLevel().contains(
        "InnerSource component detected, skipping policy violations for component"
    );
  }

  @Test
  public void testCreateManualRemediationPullRequest_innerSourceComponent_LATEST_NON_BREAKING() throws IOException {
    ComponentIdentifier innerSourceComponent = ComponentIdentifier.createMavenCoordinates(
        "com.example", "innerSource", "1.0.0");
    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(innerSourceComponent);
    InnerSourceApplication innerSourceApp = tempEntity.newInnerSourceApplication(
        packageUrl.getPackageUrl(), application);
    String nonBreakingVersion = "1.1.0";
    tempEntity.newInnerSourceVersion(innerSourceApp, nonBreakingVersion, StageTypes.RELEASE.getId());

    String branchName = branchNameGenerator.getBranchName(application, innerSourceComponent, nonBreakingVersion);

    setupPolicyEvaluationAndViolation();

    ComponentVersionInfoDTO versionInfoDTO = setupInnerSourceComponentVersionInfoDTO(nonBreakingVersion,
        ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING);

    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        innerSourceComponent, "build", "Sonatype", DEFAULT_SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(versionInfoDTO);

    PullRequestSubmissionResultDTO result = manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        innerSourceComponent,
        nonBreakingVersion,
        "Sonatype",
        true
    );

    assertThat(result.id()).isNotEmpty();
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.getById(result.id());
    assertThat(sourceControlEvent.getEventType()).isEqualTo(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    assertThat(sourceControlEvent.getRemediationVersion()).isEqualTo(nonBreakingVersion);
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(branchName);

    assertThat(logOutput).atDebugLevel().contains(
        "InnerSource component detected, skipping policy violations for component"
    );
  }

  @Test
  public void testCreateManualRemediationPullRequest_policyDeleted_success() throws Exception {
    ComponentIdentifier mavenComponentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "group", "artifact", DEFAULT_VERSION);
    String branchName = branchNameGenerator.getBranchName(application, mavenComponent, DEFAULT_REMEDIATION_VERSION);
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), stage.getStageTypeId(), DEFAULT_SCAN_ID);
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(evaluation, policy, mavenComponent, "abcd");
    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        mavenComponentIdentifier, "build", "Sonatype", DEFAULT_SCAN_ID, DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(setupComponentVersionInfoDTO());
    policyDAO.delete(policy);

    PullRequestSubmissionResultDTO result = manualPrService.createManualRemediationPullRequest(
        application.getId(),
        DEFAULT_SCAN_ID,
        mavenComponent,
        DEFAULT_REMEDIATION_VERSION,
        "Sonatype",
        true
    );

    assertThat(result.id()).isNotEmpty();
    SourceControlEvent sourceControlEvent = sourceControlEventDAO.getById(result.id());
    assertThat(sourceControlEvent.getEventType()).isEqualTo(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(application.getId());
    assertThat(sourceControlEvent.getScanId()).isEqualTo(DEFAULT_SCAN_ID);
    assertThat(sourceControlEvent.getComponentIdentifier()).isEqualTo(mavenComponent);
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(branchName);
    assertThat(sourceControlEvent.getRemediationVersion()).isEqualTo(DEFAULT_REMEDIATION_VERSION);
    assertThat(sourceControlEvent.getStageTypeId()).isEqualTo(stage.getStageTypeId());
    assertThat(sourceControlEvent.getInitiator()).isEqualTo("manual request");
  }

  private void setupPolicyEvaluationAndViolation() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), stage.getStageTypeId(), DEFAULT_SCAN_ID);
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(evaluation, policy, mavenComponent, "abcd");
  }

  protected static ComponentVersionInfoDTO setupComponentVersionInfoDTO() {
    ComponentVersionInfoDTO versionInfoDTO = new ComponentVersionInfoDTO();
    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", DEFAULT_VERSION);
    componentDetailsDTO.violatedPolicyCount = 5;
    componentDetailsDTO.breakingChangesCount = 10;

    ComponentDetailsDTO componentDetailsDTO2 = new ComponentDetailsDTO();
    componentDetailsDTO2.componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", DEFAULT_REMEDIATION_VERSION);
    componentDetailsDTO2.violatedPolicyCount = 0;
    componentDetailsDTO2.breakingChangesCount = 0;

    versionInfoDTO.allVersions = List.of(componentDetailsDTO, componentDetailsDTO2);
    versionInfoDTO.remediation = new ApiComponentRemediationValueDTO();
    versionInfoDTO.remediation.versionChanges = new ArrayList<>();

    ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("group", "artifact", DEFAULT_REMEDIATION_VERSION));
    ApiComponentChangeActionDTO actionDTO = new ApiComponentChangeActionDTO(componentDto);
    actionDTO.getComponent().breakingChangesCount = 0;

    ApiVersionChangeOptionDTO versionChangeDTO = new ApiVersionChangeOptionDTO();
    versionChangeDTO.setType(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES);
    versionChangeDTO.setData(actionDTO);
    versionInfoDTO.remediation.versionChanges.add(versionChangeDTO);

    return versionInfoDTO;
  }

  protected static ComponentVersionInfoDTO setupInnerSourceComponentVersionInfoDTO(
      String remediationVersion,
      ApiVersionChangeOptionType remediationType)
  {
    ComponentVersionInfoDTO versionInfoDTO = new ComponentVersionInfoDTO();

    versionInfoDTO.allVersions = List.of();
    versionInfoDTO.remediation = new ApiComponentRemediationValueDTO();
    versionInfoDTO.remediation.versionChanges = new ArrayList<>();

    ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("com.example", "innersource", remediationVersion));
    ApiComponentChangeActionDTO actionDTO = new ApiComponentChangeActionDTO(componentDto);
    actionDTO.getComponent().breakingChangesCount = 0;

    ApiSuggestedVersionChangeOptionDTO apiSuggestedVersionChangeOptionDTO = new ApiSuggestedVersionChangeOptionDTO();
    apiSuggestedVersionChangeOptionDTO.setType(remediationType);
    apiSuggestedVersionChangeOptionDTO.setData(actionDTO);
    versionInfoDTO.remediation.suggestedVersionChange = apiSuggestedVersionChangeOptionDTO;
    return versionInfoDTO;
  }
}
