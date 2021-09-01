/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSourceControlAdapterTest
{
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
    sourceControl.setSourceControlScansEnabled(true);
    sourceControl.setSourceControlScanTarget("/target/*");

    ApiSourceControlDTO dto = ApiSourceControlAdapter.convertToDTO(sourceControl);

    assertThat(dto.id).isEqualTo("id");
    assertThat(dto.ownerId).isEqualTo("ownerId");
    assertThat(dto.repositoryUrl).isEqualTo("repo_url");
    assertThat(dto.username).isEqualTo("username");
    assertThat(dto.token).isEqualTo("TOKEN");
    assertThat(dto.provider).isEqualTo("github");
    assertThat(dto.baseBranch).isEqualTo("master");
    assertThat(dto.remediationPullRequestsEnabled).isEqualTo(true);
    assertThat(dto.statusChecksEnabled).isEqualTo(false);
    assertThat(dto.pullRequestCommentingEnabled).isEqualTo(true);
    assertThat(dto.sourceControlScansEnabled).isEqualTo(true);
    assertThat(dto.sourceControlScanTarget).isEqualTo("/target/*");
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
    apiSourceControlDTO.sourceControlScansEnabled = true;
    apiSourceControlDTO.sourceControlScanTarget = "/target/*";

    SourceControl sourceControl = ApiSourceControlAdapter.convertFromDTO(apiSourceControlDTO);

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
    assertThat(sourceControl.getSourceControlScansEnabled()).isEqualTo(true);
    assertThat(sourceControl.getSourceControlScanTarget()).isEqualTo("/target/*");
  }
}
