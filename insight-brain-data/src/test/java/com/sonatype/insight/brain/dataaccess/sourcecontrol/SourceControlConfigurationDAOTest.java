/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalTime;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO.MIN_DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS;
import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO.MIN_PULL_REQUEST_MONITORING_INTERVAL_SECONDS;
import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO.NOT_FOUND_ERROR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SourceControlConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlConfigurationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSourceControlConfigurationDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    SourceControlConfiguration config = new SourceControlConfiguration();
    dao.insert(config);
    assertThat(config.getId()).isNotNull();

    // Read
    assertThat(dao.get()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(config);

    // Update
    config.setCloneDirectory(StringUtils.repeat("a", SourceControlConfigurationDAO.MAX_CLONE_DIRECTORY_LENGTH));
    config.setGitImplementation(GitImplementation.JAVA);
    config.setPrCommentPurgeWindow(Integer.MAX_VALUE);
    config.setPrEventPurgeWindow(Integer.MAX_VALUE);
    config.setGitExecutable(StringUtils.repeat("b", SourceControlConfigurationDAO.MAX_GIT_EXECUTABLE_LENGTH));
    config.setGitTimeoutSeconds(Integer.MAX_VALUE);
    config.setCommitUsername(StringUtils.repeat("c", SourceControlConfigurationDAO.MAX_COMMIT_USERNAME_LENGTH));
    config.setCommitEmail(createEmail(SourceControlConfigurationDAO.MAX_COMMIT_EMAIL_LENGTH));
    config.setUseUsernameInRepositoryCloneUrl(true);
    config.setDefaultBranchMonitoringStartTime(LocalTime.of(1, 11));
    config.setDefaultBranchMonitoringIntervalHours(Integer.MAX_VALUE);
    config.setPullRequestMonitoringIntervalSeconds(Integer.MAX_VALUE);
    dao.set(config);
    assertThat(dao.get()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(config);

    // Delete
    dao.delete();
    assertThat(dao.get()).isNull();
  }

  @Test
  public void testGetNotNull_Null() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(dao::getNotNull)
        .withMessageContaining(NOT_FOUND_ERROR_MSG);
  }

  @Test
  public void testGetNotNull() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    dao.insert(sourceControlConfiguration);

    assertThat(dao.getNotNull()).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(sourceControlConfiguration);
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(new SourceControlConfiguration());

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> dao.insert(new SourceControlConfiguration()))
        .withCauseInstanceOf(EntityExistsException.class);
    SourceControlConfiguration config = new SourceControlConfiguration();
    config.setId(TemporaryEntity.uuid());
    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> dao.insert(config))
        .withCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(new SourceControlConfiguration());

    SourceControlConfiguration config = new SourceControlConfiguration();
    config.setId(TemporaryEntity.uuid());
    dao.update(config);
    assertThat(dao.createQuery("SELECT entity FROM SourceControlConfiguration entity").getList())
        .extracting(SourceControlConfiguration::getId)
        .containsExactly(SourceControlConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testInsert_Validates() {
    SourceControlConfigurationDAO spy = spy(dao);
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

    spy.insert(sourceControlConfiguration);

    verify(spy).validate(sourceControlConfiguration);
  }

  @Test
  public void testUpdate_Validates() {
    SourceControlConfigurationDAO spy = spy(dao);
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

    spy.update(sourceControlConfiguration);

    verify(spy).validate(sourceControlConfiguration);
  }

  @Test
  public void testValidate_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(null))
        .withMessageContaining(SourceControlConfigurationDAO.NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testValidate_NullCloneDirectory() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCloneDirectory(null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.NO_CLONE_DIRECTORY_ERROR_MSG);
  }

  @Test
  public void testValidate_EmptyCloneDirectory() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCloneDirectory("");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.NO_CLONE_DIRECTORY_ERROR_MSG);
  }

  @Test
  public void testValidate_WhitespaceCloneDirectory() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCloneDirectory(" ");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.NO_CLONE_DIRECTORY_ERROR_MSG);
  }

  @Test
  public void testValidate_LongCloneDirectory() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCloneDirectory(
        StringUtils.repeat("a", SourceControlConfigurationDAO.MAX_CLONE_DIRECTORY_LENGTH + 1));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.LONG_CLONE_DIRECTORY_ERROR_MSG);
  }

  @Test
  public void testValidate_EmptyGitExecutable() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGitExecutable("");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.WHITESPACE_GIT_EXECUTABLE_ERROR_MSG);
  }

  @Test
  public void testValidate_WhitespaceGitExecutable() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGitExecutable(" ");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.WHITESPACE_GIT_EXECUTABLE_ERROR_MSG);
  }

  @Test
  public void testValidate_LongGitExecutable() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGitExecutable(
        StringUtils.repeat("a", SourceControlConfigurationDAO.MAX_GIT_EXECUTABLE_LENGTH + 1));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.LONG_GIT_EXECUTABLE_ERROR_MSG);
  }

  @Test
  public void testValidate_EmptyCommitUsername() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCommitUsername("");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.WHITESPACE_COMMIT_USERNAME_ERROR_MSG);
  }

  @Test
  public void testValidate_WhitespaceCommitUsername() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCommitUsername(" ");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.WHITESPACE_COMMIT_USERNAME_ERROR_MSG);
  }

  @Test
  public void testValidate_LongCommitUsername() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCommitUsername(
        StringUtils.repeat("a", SourceControlConfigurationDAO.MAX_COMMIT_USERNAME_LENGTH + 1));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.LONG_COMMIT_USERNAME_ERROR_MSG);
  }

  @Test
  public void testValidate_EmptyCommitEmail() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCommitEmail("");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.WHITESPACE_COMMIT_EMAIL_ERROR_MSG);
  }

  @Test
  public void testValidate_WhitespaceCommitEmail() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCommitEmail(" ");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.WHITESPACE_COMMIT_EMAIL_ERROR_MSG);
  }

  @Test
  public void testValidate_LongCommitEmail() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCommitEmail(createEmail(SourceControlConfigurationDAO.MAX_COMMIT_EMAIL_LENGTH + 1));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.LONG_COMMIT_EMAIL_ERROR_MSG);
  }

  @Test
  public void testValidate_InvalidEmail() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCommitEmail("invalid");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.INVALID_COMMIT_EMAIL_ERROR_MSG);
  }

  @Test
  public void testValidate_LowDefaultBranchMonitoringIntervalHours() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringIntervalHours(
        MIN_DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS - 1);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.LOW_DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS);
  }

  @Test
  public void testValidate_LowPullRequestMonitoringIntervalSeconds() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setPullRequestMonitoringIntervalSeconds(
        MIN_PULL_REQUEST_MONITORING_INTERVAL_SECONDS - 1);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.LOW_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  @Test
  public void testValidate_PullRequestMonitoringIntervalSeconds_ExactlyAtMinimum() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setPullRequestMonitoringIntervalSeconds(
        MIN_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);

    dao.validate(sourceControlConfiguration);
  }

  @Test
  public void testValidate_NegativePullRequestMonitoringIntervalSeconds() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setPullRequestMonitoringIntervalSeconds(-1);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.validate(sourceControlConfiguration))
        .withMessageContaining(SourceControlConfigurationDAO.LOW_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
  }

  private String createEmail(int length) {
    return StringUtils.repeat("a", length - 2) + "@d";
  }
}
