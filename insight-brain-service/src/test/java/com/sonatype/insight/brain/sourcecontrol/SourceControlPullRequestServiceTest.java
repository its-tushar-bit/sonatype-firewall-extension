/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.PullRequestSubmissionDTO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.git.pullrequestcreationservice.PullRequestSubmissionResultDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatus;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO.PullRequestDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

public class SourceControlPullRequestServiceTest
    extends AbstractComponentTest
{
  @Mock
  private ScmRepoVisibilityService mockScmRepoVisibilityService;

  @Mock
  private ComponentInfoService mockComponentInfoService;

  @Inject
  private SourceControlPullRequestService service;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  public static final String DEFAULT_VERSION = "1.1.0";

  public static final String DEFAULT_REMEDIATION_VERSION = "1.2.0";

  @Before
  public void before() {
    setBaseUrl("http://baseUrl");
  }

  @Test
  public void testGetPullRequestStatus_IdNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getPullRequestStatus("doesNotExist"))
        .withMessageContaining("SourceControlEvent with ID doesNotExist does not exist.");
  }

  @Test
  public void testGetPullRequestStatus_IdNotRemediation() {
    Application app = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(app);
    event.setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId()))
        .withMessageContaining(
            "Pull request not found for application '" + app.getPublicId() + "' and for id '" + event.getId() + "'.");
  }

  @Test
  public void testGetPullRequestStatus_ApplicationId() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_ApplicationPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_Pending_StatusNew() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_Pending_StatusInProgress() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_PENDING);
    assertThat(dto).isInstanceOf(PullRequestCreationPendingDTO.class);
  }

  @Test
  public void testGetPullRequestStatus_Error_StatusError() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
    assertThat(dto).isInstanceOf(PullRequestCreationFailedDTO.class);
    PullRequestCreationFailedDTO pullRequestCreationFailedDTO = (PullRequestCreationFailedDTO) dto;
    assertThat(pullRequestCreationFailedDTO.reason).isEqualTo("An unknown error occurred.");
  }

  @Test
  public void testGetPullRequestStatus_Error_StatusError_WithReason() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    event.setEventStatusDetails("some error");
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST_CREATION_FAILED);
    assertThat(dto).isInstanceOf(PullRequestCreationFailedDTO.class);
    PullRequestCreationFailedDTO pullRequestCreationFailedDTO = (PullRequestCreationFailedDTO) dto;
    assertThat(pullRequestCreationFailedDTO.reason).isEqualTo("some error");
  }

  @Test
  public void testGetPullRequestStatus_Completed_StatusComplete() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("some pr link");
    event.setPullRequestNumber(1);
    sourceControlEventDAO.update(event);

    AutomatedRemediationStatusDTO dto = service.getPullRequestStatus(event.getId());

    assertThat(dto.status).isEqualTo(AutomatedRemediationStatus.PULL_REQUEST);
    assertThat(dto).isInstanceOf(PullRequestDTO.class);
    PullRequestDTO pullRequestDTO = (PullRequestDTO) dto;
    assertThat(pullRequestDTO.url).isEqualTo("some pr link");
    assertThat(pullRequestDTO.pullRequestId).isEqualTo(1);
  }

  @Test
  public void testGetPullRequestStatus_Completed_StatusComplete_UrlMissing() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId()))
        .withMessageContaining("URL missing from pull request for id '" + event.getId() + "'.");
  }

  @Test
  public void testGetPullRequestStatus_UnsupportedStatus() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_PARTIALLY_COMPLETE);
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId()))
        .withMessageContaining("Unsupported event status 'partially complete'.");
  }

  @Test
  public void testGetPullRequestStatus_UnknownStatus() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlEvent event = createRemediationEvent(application);
    event.setEventStatus("someUnknownStatus");
    sourceControlEventDAO.update(event);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
        () -> service.getPullRequestStatus(event.getId()))
        .withMessageContaining("Unsupported event status 'someUnknownStatus'.");
  }

  @Test
  public void testCreatePullRequest_Success() throws PlexusCipherException, IOException {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.1.0");
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    setupSourceControl(application);
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.2.0", "Sonatype",
            true);

    // mock private repository and componentInfo
    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        mavenComponentIdentifier, "build", "Sonatype", "scanId", DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(setupComponentVersionInfoDTO());
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(any(GitRepositoryInfo.class))).thenReturn(
        true);

    PullRequestSubmissionResultDTO dto = service.createPullRequest(submission);

    assertThat(dto.id()).isNotEmpty();
  }

  @Test
  public void testCreatePullRequest_Failure_NotApplicableVersionChange() throws PlexusCipherException {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.1.0");
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    setupSourceControl(application);
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.2.0", "Sonatype",
            true);

    // mock private repository, no applicable version return
    ComponentVersionInfoDTO componentVersionInfoDTO = new ComponentVersionInfoDTO();
    componentVersionInfoDTO.remediation = new ApiComponentRemediationValueDTO();
    componentVersionInfoDTO.remediation.versionChanges = new ArrayList<>();
    when(mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
        mavenComponentIdentifier, "build", "Sonatype", "scanId", DependencyType.DIRECT,
        SourceEndpoint.MANUAL_PULL_REQUEST,
        true)).thenReturn(componentVersionInfoDTO);
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(any(GitRepositoryInfo.class))).thenReturn(
        true);

    assertThatThrownBy(() -> service.createPullRequest(submission))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No applicable version change found for component " +
            ComponentDisplayNameUtil.fromIdentifier(mavenComponentIdentifier));
  }

  @Test
  public void testCreatePullRequest_Failure_PublicRepository() throws PlexusCipherException {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.1.0");
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    setupSourceControl(application);
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.2.0", "Sonatype",
            true);

    // mock public repository, no applicable version return
    when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(any(GitRepositoryInfo.class))).thenReturn(
        false);

    assertThatThrownBy((() -> service.createPullRequest(submission)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Manual pull request creation is not eligible");
  }

  @Test
  public void testCreatePullRequest_Failure_NonDirectDependency() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier mavenComponentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.1.0");
    tempEntity.newPolicyEvaluation(application.getId(), "build", "scanId");

    setupSourceControl(application);
    PullRequestSubmissionDTO submission =
        new PullRequestSubmissionDTO(application.getId(), "scanId", mavenComponentIdentifier, "1.2.0", "Sonatype",
            false);

    // mock private repository and componentInfo
    lenient().when(
        mockComponentInfoService.getComponentVersionInfoNoAuth(OwnerType.APPLICATION, application.getPublicId(),
            mavenComponentIdentifier, "build", "Sonatype", "scanId", DependencyType.TRANSITIVE,
            SourceEndpoint.MANUAL_PULL_REQUEST,
            true))
        .thenReturn(setupComponentVersionInfoDTO());
    lenient().when(mockScmRepoVisibilityService.isRepositoryValidForPullRequestFeatures(any(GitRepositoryInfo.class)))
        .thenReturn(true);

    assertThatThrownBy(() -> service.createPullRequest(submission))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining(
            "Manual pull request creation is not eligible for application " + application.getPublicId() +
                " component " + ComponentDisplayNameUtil.fromIdentifier(mavenComponentIdentifier) +
                " in stage " + StageTypes.BUILD.getId());
  }

  @Test
  public void testCreatePullRequest_ValidationFailures() {
    Application application = tempEntity.newApplicationWithParent();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("group", "artifact", "1.0.0");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.createPullRequest(null)).withMessage("Pull request submission cannot be null");

    PullRequestSubmissionDTO submissionWithWrongAppId =
        new PullRequestSubmissionDTO("wrongAppId", "scanId", componentIdentifier, "1.2.0", "Sonatype", true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.createPullRequest(submissionWithWrongAppId))
        .withMessage("Application not found for id 'wrongAppId'.");

    PullRequestSubmissionDTO submissionWithNullScanId =
        new PullRequestSubmissionDTO(application.getId(), null, componentIdentifier, "1.2.0", "Sonatype", true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.createPullRequest(submissionWithNullScanId)).withMessage("Scan ID cannot be null");

    PullRequestSubmissionDTO submissionWithNullComponentId =
        new PullRequestSubmissionDTO(application.getId(), "scanId", null, "1.2.0", "Sonatype", true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.createPullRequest(submissionWithNullComponentId))
        .withMessage("Component identifier cannot be null");

    PullRequestSubmissionDTO submissionWithNullTargetVersion =
        new PullRequestSubmissionDTO(application.getId(), "scanId", componentIdentifier, null, "Sonatype", true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.createPullRequest(submissionWithNullTargetVersion)).withMessage("Target version cannot be null");

    PullRequestSubmissionDTO submissionWithNullIdentificationSource =
        new PullRequestSubmissionDTO(application.getId(), "scanId", componentIdentifier, "1.2.0", null, true);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.createPullRequest(submissionWithNullIdentificationSource))
        .withMessage("Identification source cannot be null");
  }

  private void setupSourceControl(final Application application) throws PlexusCipherException {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(application.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);
  }

  private SourceControlEvent createRemediationEvent(final Application app) {
    SourceControlEvent event = new SourceControlEvent().forRemediationPullRequest().setApplicationId(app.getId());
    sourceControlEventDAO.insert(event);
    return event;
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
}
