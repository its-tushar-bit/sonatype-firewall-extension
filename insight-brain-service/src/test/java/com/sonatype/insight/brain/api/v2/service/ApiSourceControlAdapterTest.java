/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSourceControlAdapterTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSourceControlAdapter apiSourceControlAdapter;

  @SuppressWarnings("deprecation")
  @Test
  public void convertToDTO() {
    SourceControl sourceControl = new SourceControl();
    sourceControl.setId("id");
    sourceControl.setOwnerId("ownerId");
    sourceControl.setRepositoryUrl("repo_url");
    sourceControl.setUsername("username");
    sourceControl.setToken("TOKEN");
    sourceControl.setProvider(SourceControlProvider.GITHUB);
    sourceControl.setBaseBranch("master");
    sourceControl.setRemediationPullRequestsEnabled(true);
    sourceControl.setStatusChecksEnabled(false);
    sourceControl.setPullRequestCommentingEnabled(true);
    sourceControl.setSourceControlEvaluationsEnabled(true);
    sourceControl.setSourceControlScanTarget("/target/*");
    sourceControl.setSshEnabled(true);
    sourceControl.setCommitStatusEnabled(false);
    sourceControl.setManualPullRequestsEnabled(false);
    sourceControl.setInnerSourceAutomatedUpdatesEnabled(false);
    sourceControl.setClosePrOnFailedChecksEnabled(true);
    sourceControl.setClosePrAfterDaysOpenEnabled(true);
    sourceControl.setClosePrAfterDays(7);
    sourceControl.setAuthenticationType(SourceControl.AuthenticationType.PAT);

    ApiSourceControlDTO dto = apiSourceControlAdapter.convertToDTO(sourceControl);

    assertThat(dto.id).isEqualTo("id");
    assertThat(dto.ownerId).isEqualTo("ownerId");
    assertThat(dto.repositoryUrl).isEqualTo("repo_url");
    assertThat(dto.username).isEqualTo("username");
    assertThat(dto.token).isEqualTo("TOKEN");
    assertThat(dto.provider).isEqualTo("github");
    assertThat(dto.baseBranch).isEqualTo("master");
    assertThat(dto.remediationPullRequestsEnabled).isEqualTo(true);
    assertThat(dto.enablePullRequests).isEqualTo(dto.remediationPullRequestsEnabled);
    assertThat(dto.statusChecksEnabled).isEqualTo(false);
    assertThat(dto.enableStatusChecks).isEqualTo(dto.statusChecksEnabled);
    assertThat(dto.pullRequestCommentingEnabled).isEqualTo(true);
    assertThat(dto.sourceControlEvaluationsEnabled).isEqualTo(true);
    assertThat(dto.sourceControlScanTarget).isEqualTo("/target/*");
    assertThat(dto.sshEnabled).isTrue();
    assertThat(dto.commitStatusEnabled).isFalse();
    assertThat(dto.manualPullRequestsEnabled).isFalse();
    assertThat(dto.innerSourceAutomatedUpdatesEnabled).isFalse();
    assertThat(dto.closePrOnFailedChecksEnabled).isTrue();
    assertThat(dto.closePrAfterDaysOpenEnabled).isTrue();
    assertThat(dto.closePrAfterDays).isEqualTo(7);
    assertThat(dto.authenticationType).isEqualTo("PAT");
  }

  @Test
  public void convertFromDTO() {
    ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
    apiSourceControlDTO.id = "id";
    apiSourceControlDTO.ownerId = "ownerId";
    apiSourceControlDTO.repositoryUrl = "repo_url";
    apiSourceControlDTO.username = "username";
    apiSourceControlDTO.token = "TOKEN";
    apiSourceControlDTO.provider = "github";
    apiSourceControlDTO.baseBranch = "master";
    apiSourceControlDTO.remediationPullRequestsEnabled = true;
    apiSourceControlDTO.statusChecksEnabled = false;
    apiSourceControlDTO.pullRequestCommentingEnabled = true;
    apiSourceControlDTO.sourceControlEvaluationsEnabled = true;
    apiSourceControlDTO.sourceControlScanTarget = "/target/*";
    apiSourceControlDTO.sshEnabled = true;
    apiSourceControlDTO.commitStatusEnabled = false;
    apiSourceControlDTO.manualPullRequestsEnabled = false;
    apiSourceControlDTO.innerSourceAutomatedUpdatesEnabled = false;
    apiSourceControlDTO.closePrOnFailedChecksEnabled = true;
    apiSourceControlDTO.closePrAfterDaysOpenEnabled = true;
    apiSourceControlDTO.closePrAfterDays = 7;
    apiSourceControlDTO.authenticationType = "GITHUB_APP";

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(apiSourceControlDTO);

    assertThat(sourceControl.getId()).isNull();
    assertThat(sourceControl.getOwnerId()).isEqualTo("ownerId");
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo("repo_url");
    assertThat(sourceControl.getUsername()).isEqualTo("username");
    assertThat(sourceControl.getToken()).isEqualTo("TOKEN");
    assertThat(sourceControl.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(sourceControl.getBaseBranch()).isEqualTo("master");
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getStatusChecksEnabled()).isEqualTo(false);
    assertThat(sourceControl.getPullRequestCommentingEnabled()).isEqualTo(true);
    assertThat(sourceControl.getSourceControlEvaluationsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getSourceControlScanTarget()).isEqualTo("/target/*");
    assertThat(sourceControl.getSshEnabled()).isTrue();
    assertThat(sourceControl.getCommitStatusEnabled()).isFalse();
    assertThat(sourceControl.getManualPullRequestsEnabled()).isFalse();
    assertThat(sourceControl.getInnerSourceAutomatedUpdatesEnabled()).isFalse();
    assertThat(sourceControl.getClosePrOnFailedChecksEnabled()).isTrue();
    assertThat(sourceControl.getClosePrAfterDaysOpenEnabled()).isTrue();
    assertThat(sourceControl.getClosePrAfterDays()).isEqualTo(7);
    assertThat(sourceControl.getAuthenticationType()).isEqualTo(SourceControl.AuthenticationType.GITHUB_APP);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void convertFromDTO_DeprecatedFields() {
    ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
    apiSourceControlDTO.enablePullRequests = true;
    apiSourceControlDTO.enableStatusChecks = false;

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(apiSourceControlDTO);

    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getStatusChecksEnabled()).isEqualTo(false);
  }
}
