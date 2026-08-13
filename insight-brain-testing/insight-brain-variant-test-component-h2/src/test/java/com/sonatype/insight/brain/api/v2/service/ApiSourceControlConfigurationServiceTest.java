/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.LocalTime;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static com.sonatype.insight.brain.api.v2.service.ApiSourceControlConfigurationService.BAD_CONFIG_ERROR_MSG;
import static com.sonatype.insight.brain.api.v2.service.ApiSourceControlConfigurationService.BAD_DEFAULT_BRANCH_MONITORING_START_TIME;
import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO.NOT_FOUND_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO.NO_CONFIG_ERROR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ComponentH2Test
public class ApiSourceControlConfigurationServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiSourceControlConfigurationService service;

  @Inject
  private SourceControlConfigurationDAO dao;

  @Inject
  private PasswordHandler passwordHandler;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private SourceControlConfigurationListener mockSourceControlConfigurationListener;

  @Test
  public void testGetConfiguration_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.getConfiguration())
        .withMessageContaining(NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testGetConfiguration() {
    SourceControlConfiguration config = tempEntity.newSourceControlConfiguration();

    ApiSourceControlConfigurationDTO configuration = service.getConfiguration();

    assertThat(configuration).usingRecursiveComparison()
        .ignoringFields("defaultBranchMonitoringStartTime")
        .isEqualTo(config);
    assertThat(configuration.defaultBranchMonitoringStartTime).isEqualTo(
        config.getDefaultBranchMonitoringStartTimeString());
  }

  @Test
  public void testSetConfiguration_NullConfig() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(null))
        .withMessageContaining(NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_BadConfig() {
    ObjectNode badConfig = new ObjectMapper().createObjectNode();
    badConfig.putArray("cloneDirectory");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(badConfig))
        .withMessageContaining(BAD_CONFIG_ERROR_MSG);
  }

  @Test
  public void testSetConfiguration_BadDefaultBranchMonitoringStartTime() {
    ObjectNode badConfig = new ObjectMapper().createObjectNode();
    badConfig.put("defaultBranchMonitoringStartTime", "bad");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.setConfiguration(badConfig))
        .withMessageContaining(BAD_DEFAULT_BRANCH_MONITORING_START_TIME);
  }

  @Test
  public void testSetConfiguration_New_Default() {
    ApiSourceControlConfigurationService spy = spy(service);

    spy.setConfiguration(new ObjectMapper().createObjectNode());

    SourceControlConfiguration sourceControlConfiguration = dao.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("defaultBranchMonitoringStartTime")
        .isEqualTo(new ApiSourceControlConfigurationDTO());
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo(
        new ApiSourceControlConfigurationDTO().defaultBranchMonitoringStartTime);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_New_Custom() {
    ApiSourceControlConfigurationService spy = spy(service);
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
    dto.pullRequestMonitoringIntervalSeconds = 180;
    dto.gpgSigningKey = "some-gpg-key";
    dto.gpgPassphrase = "some-passphrase";

    spy.setConfiguration(JsonUtils.asTree(dto));

    SourceControlConfiguration sourceControlConfiguration = dao.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("defaultBranchMonitoringStartTime", "gpgPassphrase")
        .isEqualTo(dto);
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo(
        dto.defaultBranchMonitoringStartTime);
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNotNull();
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNotEqualTo("some-passphrase");
    assertThat(sourceControlConfiguration.getGpgPassphrase()).hasSize(46);
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update_CloneDirectory() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("cloneDirectory", "updated-clone-directory"));
    assertThat(dao.get().getCloneDirectory()).isEqualTo("updated-clone-directory");
  }

  @Test
  public void testSetConfiguration_Update_GitImplementation() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("gitImplementation", GitImplementation.NATIVE.toString()));
    assertThat(dao.get().getGitImplementation()).isEqualTo(GitImplementation.NATIVE);
  }

  @Test
  public void testSetConfiguration_Update_PrCommentPurgeWindow() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("prCommentPurgeWindow", 10));
    assertThat(dao.get().getPrCommentPurgeWindow()).isEqualTo(10);
  }

  @Test
  public void testSetConfiguration_Update_PrEventPurgeWindow() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("prEventPurgeWindow", 10));
    assertThat(dao.get().getPrEventPurgeWindow()).isEqualTo(10);
  }

  @Test
  public void testSetConfiguration_Update_GitExecutable() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("gitExecutable", "updated-git-executable"));
    assertThat(dao.get().getGitExecutable()).isEqualTo("updated-git-executable");
  }

  @Test
  public void testSetConfiguration_Update_GitTimeoutSeconds() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("gitTimeoutSeconds", 10));
    assertThat(dao.get().getGitTimeoutSeconds()).isEqualTo(10);
  }

  @Test
  public void testSetConfiguration_Update_CommitUsername() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("commitUsername", "updated-commit-username"));
    assertThat(dao.get().getCommitUsername()).isEqualTo("updated-commit-username");
  }

  @Test
  public void testSetConfiguration_Update_CommitEmail() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("commitEmail", "updated-commit-email@d"));
    assertThat(dao.get().getCommitEmail()).isEqualTo("updated-commit-email@d");
  }

  @Test
  public void testSetConfiguration_Update_UseUsernameInRepositoryCloneUrl() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("useUsernameInRepositoryCloneUrl", true));
    assertThat(dao.get().isUseUsernameInRepositoryCloneUrl()).isTrue();
  }

  @Test
  public void testSetConfiguration_Update_DefaultBranchMonitoringStartTime() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("defaultBranchMonitoringStartTime", "1:11"));
    assertThat(dao.get().getDefaultBranchMonitoringStartTime()).isEqualTo(LocalTime.of(1, 11));
  }

  @Test
  public void testSetConfiguration_Update_DefaultBranchMonitoringIntervalHours() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("defaultBranchMonitoringIntervalHours", "2"));
    assertThat(dao.get().getDefaultBranchMonitoringIntervalHours()).isEqualTo(2);
  }

  @Test
  public void testSetConfiguration_Update_PullRequestMonitoringIntervalSeconds() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("pullRequestMonitoringIntervalSeconds", "240"));
    assertThat(dao.get().getPullRequestMonitoringIntervalSeconds()).isEqualTo(240);
  }

  @Test
  public void testSetConfiguration_Update_GpgSigningKey() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("gpgSigningKey", "updated-gpg-key"));
    assertThat(dao.get().getGpgSigningKey()).isEqualTo("updated-gpg-key");
  }

  @Test
  public void testSetConfiguration_Update_GpgPassphrase() {
    testSetConfiguration_Update_Field(
        new ObjectMapper().createObjectNode().put("gpgPassphrase", "updated-passphrase"));
    assertThat(dao.get().getGpgPassphrase()).isNotNull();
    assertThat(dao.get().getGpgPassphrase()).isNotEqualTo("updated-passphrase");
  }

  private void testSetConfiguration_Update_Field(ObjectNode objectNode) {
    ApiSourceControlConfigurationService spy = spy(service);
    SourceControlConfiguration existing = tempEntity.newSourceControlConfiguration();

    spy.setConfiguration(objectNode);

    String fieldName = objectNode.fieldNames().next();
    SourceControlConfiguration sourceControlConfiguration = dao.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields(fieldName, "defaultBranchMonitoringStartTimeString", "defaultBranchMonitoringStartTime")
        .isEqualTo(existing);
    if (!fieldName.equals("defaultBranchMonitoringStartTime")) {
      assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTime()).isEqualTo(
          existing.getDefaultBranchMonitoringStartTime());
    }
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testSetConfiguration_Update_All() {
    ApiSourceControlConfigurationService spy = spy(service);
    tempEntity.newSourceControlConfiguration();
    ApiSourceControlConfigurationDTO dto = new ApiSourceControlConfigurationDTO();
    dto.cloneDirectory = "updated-clone-directory";
    dto.gitImplementation = GitImplementation.NATIVE;
    dto.prCommentPurgeWindow = 10;
    dto.prEventPurgeWindow = 20;
    dto.gitExecutable = "updated-git-executable";
    dto.gitTimeoutSeconds = 30;
    dto.commitUsername = "updated-commit-username";
    dto.commitEmail = "updated-commit-email@d";
    dto.useUsernameInRepositoryCloneUrl = true;
    dto.defaultBranchMonitoringStartTime = "1:11";
    dto.defaultBranchMonitoringIntervalHours = 40;
    dto.pullRequestMonitoringIntervalSeconds = 60;
    dto.gpgSigningKey = "updated-gpg-key";
    dto.gpgPassphrase = "updated-passphrase";

    spy.setConfiguration(JsonUtils.asTree(dto));

    SourceControlConfiguration sourceControlConfiguration = dao.get();
    assertThat(sourceControlConfiguration).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringExpectedNullFields()
        .ignoringFields("defaultBranchMonitoringStartTime", "gpgPassphrase")
        .isEqualTo(dto);
    assertThat(sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString()).isEqualTo(
        dto.defaultBranchMonitoringStartTime);
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNotNull();
    assertThat(sourceControlConfiguration.getGpgPassphrase()).isNotEqualTo("updated-passphrase");
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testDeleteConfiguration_NotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.deleteConfiguration())
        .withMessageContaining(NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testDeleteConfiguration() {
    ApiSourceControlConfigurationService spy = spy(service);
    tempEntity.newSourceControlConfiguration();

    spy.deleteConfiguration();

    assertThat(dao.get()).isNull();
    verify(spy).updateAllClusterNodesFromConfiguration();
  }

  @Test
  public void testUpdateAllClusterNodesFromConfiguration() {
    ApiSourceControlConfigurationService spy = spy(service);

    spy.updateAllClusterNodesFromConfiguration();

    verify(spy).applySourceControlConfigurationToClients();
    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(spy);
  }

  @Test
  public void testApplySourceControlConfigurationToClients() {
    service.applySourceControlConfigurationToClients();

    verify(mockSourceControlConfigurationListener).sourceControlConfigurationChanged();
  }

  @Test
  public void testExecute() {
    ApiSourceControlConfigurationService spy = spy(service);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spy).applySourceControlConfigurationToClients();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spy.execute(mock(JobExecutionContext.class));
    }

    verify(spy).applySourceControlConfigurationToClients();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ApiSourceControlConfigurationService.class)
        .build()
        .isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testGetConfiguration_WithGpgPassphrase_ReturnsMasked() {
    SourceControlConfiguration config = tempEntity.newSourceControlConfiguration();
    config.setGpgPassphrase("encrypted-passphrase");
    dao.set(config);

    ApiSourceControlConfigurationDTO configuration = service.getConfiguration();

    assertThat(configuration.gpgPassphrase).isEqualTo("****");
  }

  @Test
  public void testGetConfiguration_WithoutGpgPassphrase_ReturnsNull() {
    SourceControlConfiguration config = tempEntity.newSourceControlConfiguration();
    config.setGpgPassphrase(null);
    dao.set(config);

    ApiSourceControlConfigurationDTO configuration = service.getConfiguration();

    assertThat(configuration.gpgPassphrase).isNull();
  }

  @Test
  public void testSetConfiguration_GpgPassphrase_EncryptsAndDecryptsSuccessfully() {
    String originalPassphrase = "my-secret-passphrase";
    ApiSourceControlConfigurationDTO dto = new ApiSourceControlConfigurationDTO();
    dto.gpgPassphrase = originalPassphrase;

    service.setConfiguration(JsonUtils.asTree(dto));

    SourceControlConfiguration storedConfig = dao.getNotNull();
    assertThat(storedConfig.getGpgPassphrase()).isNotNull();
    assertThat(storedConfig.getGpgPassphrase()).isNotEqualTo(originalPassphrase);

    String decryptedPassphrase = passwordHandler.decryptPassword(storedConfig.getGpgPassphrase());
    assertThat(decryptedPassphrase).isEqualTo(originalPassphrase);
  }

}
