/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSourceControlConfigurationResourceTest
    extends AbstractResourceTest
{
  private SourceControlConfigurationDAO dao;

  @Before
  public void setUp() {
    dao = lookup(SourceControlConfigurationDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SOURCE_CONTROL_CONFIG_RESOURCE_PATH_V2);
  }

  @Test
  public void testGetConfiguration() throws Exception {
    SourceControlConfiguration config = tempEntity.newSourceControlConfiguration();

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    ApiSourceControlConfigurationDTO dto = response.getBody(ApiSourceControlConfigurationDTO.class);
    assertThat(dto).usingRecursiveComparison().ignoringExpectedNullFields()
        .ignoringFields("defaultBranchMonitoringStartTime").isEqualTo(config);
    assertThat(dto.defaultBranchMonitoringStartTime).isEqualTo(config.getDefaultBranchMonitoringStartTimeString());
  }

  @Test
  public void testGetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(SourceControlConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration() throws Exception {
    ApiSourceControlConfigurationDTO dto = new ApiSourceControlConfigurationDTO();
    dto.cloneDirectory = "some-clone-directory";
    dto.gitImplementation = GitImplementation.JAVA;
    dto.prCommentPurgeWindow = 1;
    dto.prEventPurgeWindow = 2;
    dto.gitExecutable = "some-git-executable";
    dto.gitTimeoutSeconds = 3;
    dto.commitUsername = "some-commit-username";
    dto.commitEmail = "some-commit-email@d";
    dto.useUsernameInRepositoryCloneUrl = true;
    dto.defaultBranchMonitoringStartTime = "1:11";
    dto.defaultBranchMonitoringIntervalHours = 4;
    dto.pullRequestMonitoringIntervalSeconds = 60;
    dto.gpgSigningKey = "some-gpg-key";
    dto.gpgPassphrase = "some-passphrase";

    HttpResponse response = restRequest().body(dto).put();

    assertResponseStatus(204, response);
    SourceControlConfiguration sourceControlConfiguration = dao.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison().ignoringExpectedNullFields()
        .ignoringFields("defaultBranchMonitoringStartTime", "gpgPassphrase").isEqualTo(dto);
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo(
        dto.defaultBranchMonitoringStartTime);
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNotEqualTo("some-passphrase").hasSize(46);
  }

  @Test
  public void testSetConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains(SourceControlConfigurationDAO.NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_PullRequestMonitoringIntervalSeconds_TooLow() throws Exception {
    ApiSourceControlConfigurationDTO dto = new ApiSourceControlConfigurationDTO();
    dto.pullRequestMonitoringIntervalSeconds = 59;

    HttpResponse response = restRequest().body(dto).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .contains(SourceControlConfigurationDAO.LOW_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @Test
  public void testDeleteConfiguration() throws Exception {
    tempEntity.newSourceControlConfiguration();

    HttpResponse response = restRequest().delete();

    assertResponseStatus(204, response);
    assertThat(dao.get()).isNull();
  }

  @Test
  public void testDeleteConfiguration_Error() throws Exception {
    HttpResponse response = restRequest().delete();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains(SourceControlConfigurationDAO.NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration_WithGpgPassphrase_ReturnsMasked() throws Exception {
    SourceControlConfiguration config = tempEntity.newSourceControlConfiguration();
    config.setGpgPassphrase("encrypted-passphrase");
    dao.set(config);

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    ApiSourceControlConfigurationDTO dto = response.getBody(ApiSourceControlConfigurationDTO.class);
    assertThat(dto.gpgPassphrase).isEqualTo("****");
  }

  @Test
  public void testGetConfiguration_WithoutGpgPassphrase_ReturnsNull() throws Exception {
    SourceControlConfiguration config = tempEntity.newSourceControlConfiguration();
    config.setGpgPassphrase(null);
    dao.set(config);

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);
    ApiSourceControlConfigurationDTO dto = response.getBody(ApiSourceControlConfigurationDTO.class);
    assertThat(dto.gpgPassphrase).isNull();
  }
}
