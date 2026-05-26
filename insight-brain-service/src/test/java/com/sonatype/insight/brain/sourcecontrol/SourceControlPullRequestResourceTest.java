/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.PullRequestSubmissionDTO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.PullRequestSubmissionResultDTO;
import com.sonatype.insight.brain.hds.AutomatedRemediationStatusDTO;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceControlPullRequestResourceTest
{
  private SourceControlPullRequestService sourceControlPullRequestService;

  private SourceControlPullRequestResource resource;

  @Before
  public void setUp() {
    sourceControlPullRequestService = mock(SourceControlPullRequestService.class);
    resource = new SourceControlPullRequestResource(sourceControlPullRequestService);
  }

  @Test
  public void testGetPullRequestStatus_Pending() {
    AutomatedRemediationStatusDTO expected =
        new AutomatedRemediationStatusDTO.PullRequestCreationPendingDTO("pull-request-id");
    when(sourceControlPullRequestService.getPullRequestStatus("pull-request-id")).thenReturn(expected);

    AutomatedRemediationStatusDTO actual = resource.getPullRequestStatus("pull-request-id");

    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void testGetPullRequestStatus_Success() {
    AutomatedRemediationStatusDTO expected =
        new AutomatedRemediationStatusDTO.PullRequestDTO("https://github.com/sonatype/insight-brain/pull/13397", 13397);
    when(sourceControlPullRequestService.getPullRequestStatus("pull-request-id")).thenReturn(expected);

    AutomatedRemediationStatusDTO actual = resource.getPullRequestStatus("pull-request-id");

    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void testGetPullRequestStatus_Failure() {
    AutomatedRemediationStatusDTO expected =
        new AutomatedRemediationStatusDTO.PullRequestCreationFailedDTO("Some error", null, null);
    when(sourceControlPullRequestService.getPullRequestStatus("pull-request-id")).thenReturn(expected);

    AutomatedRemediationStatusDTO actual = resource.getPullRequestStatus("pull-request-id");

    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void testCreatePullRequest_Success() throws Exception {
    PullRequestSubmissionDTO submission = createSubmission("app-id", "scanId", "1.3.16", true);
    PullRequestSubmissionResultDTO expected = new PullRequestSubmissionResultDTO("pull-request-id");
    when(sourceControlPullRequestService.createPullRequest(submission)).thenReturn(expected);

    PullRequestSubmissionResultDTO actual = resource.createPullRequest(submission);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testCreatePullRequest_Failure_NoApplicableVersionChange() throws Exception {
    PullRequestSubmissionDTO submission = createSubmission("app-id", "scanId", "1.3.16", true);
    when(sourceControlPullRequestService.createPullRequest(submission))
        .thenThrow(new BadRequestException("No applicable version change found"));

    assertThatThrownBy(() -> resource.createPullRequest(submission))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("No applicable version change found");
  }

  @Test
  public void testCreatePullRequest_Failure_TargetMismatched() throws Exception {
    PullRequestSubmissionDTO submission = createSubmission("app-id", "scanId", "1.3.15", true);
    when(sourceControlPullRequestService.createPullRequest(submission))
        .thenThrow(new BadRequestException("Target version does not match the applicable version change"));

    assertThatThrownBy(() -> resource.createPullRequest(submission))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Target version does not match the applicable version change");
  }

  @Test
  public void testCreatePullRequest_Failure_InvalidScanId() throws Exception {
    PullRequestSubmissionDTO submission = createSubmission("app-id", "nonExistentScanId", "1.3.16", true);
    when(sourceControlPullRequestService.createPullRequest(submission))
        .thenThrow(new NotFoundException("Scan does not exist"));

    assertThatThrownBy(() -> resource.createPullRequest(submission))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Scan does not exist");
  }

  @Test
  public void testCreatePullRequest_Failure_NoSourceControlConfigured() throws Exception {
    PullRequestSubmissionDTO submission = createSubmission("app-id", "scanId", "1.3.16", true);
    when(sourceControlPullRequestService.createPullRequest(submission))
        .thenThrow(new BadRequestException("Manual pull request creation is not eligible"));

    assertThatThrownBy(() -> resource.createPullRequest(submission))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Manual pull request creation is not eligible");
  }

  @Test
  public void testCreatePullRequest_Failure_NonDirectDependency() throws Exception {
    PullRequestSubmissionDTO submission = createSubmission("app-id", "scanId", "1.3.16", false);
    when(sourceControlPullRequestService.createPullRequest(submission))
        .thenThrow(new BadRequestException("Dependency must be direct"));

    assertThatThrownBy(() -> resource.createPullRequest(submission))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Dependency must be direct");
  }

  private PullRequestSubmissionDTO createSubmission(
      String applicationPublicId,
      String scanId,
      String targetVersion,
      boolean directDependency)
  {
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("ch.qos.logback", "logback-classic", "1.3.14", "", "jar");
    return new PullRequestSubmissionDTO(applicationPublicId, scanId, componentIdentifier, targetVersion, "Sonatype",
        directDependency);
  }
}
