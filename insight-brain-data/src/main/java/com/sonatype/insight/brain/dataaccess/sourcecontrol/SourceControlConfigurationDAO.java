/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.mail.internet.InternetAddress;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SourceControlConfiguration.SOURCE_CONTROL_CONFIGURATION;

@Named
@Singleton
public class SourceControlConfigurationDAO
    extends AbstractOperationalSqlDAO<SourceControlConfiguration>
{
  public static final String SINGLETON_ENTITY_ID = "source-control-configuration";

  public static final String NO_CONFIG_ERROR_MSG = "A configuration must be given.";

  // Visible for testing
  static final String NO_CLONE_DIRECTORY_ERROR_MSG = "The clone directory path is required.";

  // Visible for testing
  static final int MAX_CLONE_DIRECTORY_LENGTH = 1000;

  // Visible for testing
  static final String LONG_CLONE_DIRECTORY_ERROR_MSG = "The clone directory path cannot exceed 1000 characters.";

  // Visible for testing
  static final int MAX_GIT_EXECUTABLE_LENGTH = 1000;

  // Visible for testing
  static final String LONG_GIT_EXECUTABLE_ERROR_MSG = "The git executable path cannot exceed 1000 characters.";

  // Visible for testing
  static final String WHITESPACE_GIT_EXECUTABLE_ERROR_MSG = "The git executable path cannot be whitespace.";

  // Visible for testing
  static final int MAX_COMMIT_USERNAME_LENGTH = 256;

  // Visible for testing
  static final String LONG_COMMIT_USERNAME_ERROR_MSG = "The commit username cannot exceed 256 characters.";

  // Visible for testing
  static final String WHITESPACE_COMMIT_USERNAME_ERROR_MSG = "The commit username cannot be whitespace.";

  // Visible for testing
  static final int MAX_COMMIT_EMAIL_LENGTH = 256;

  // Visible for testing
  static final String LONG_COMMIT_EMAIL_ERROR_MSG = "The commit email address cannot exceed 256 characters.";

  // Visible for testing
  static final String WHITESPACE_COMMIT_EMAIL_ERROR_MSG = "The commit email address cannot be whitespace.";

  // Visible for testing
  static final String INVALID_COMMIT_EMAIL_ERROR_MSG = "The commit email address is invalid.";

  // Visible for testing
  static final int MIN_DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS = 1;

  // Visible for testing
  static final String LOW_DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS =
      "The default branch monitoring interval hours must be at least 1.";

  // Visible for testing
  static final int MIN_PULL_REQUEST_MONITORING_INTERVAL_SECONDS = 60;

  public static final String LOW_PULL_REQUEST_MONITORING_INTERVAL_SECONDS =
      "The pull request monitoring interval seconds must be at least 60.";

  public static final String NOT_FOUND_ERROR_MSG = "Source control not configured.";

  @Inject
  public SourceControlConfigurationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SourceControlConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  // Use this method when you need to access the SourceControlConfiguration internally (as opposed to
  // for a REST GET response). Decrypt the gpgPassphrase using PasswordHandler#decryptPassword
  public SourceControlConfiguration getNotNull() {
    SourceControlConfiguration config = get();
    if (config == null) {
      throw new NotFoundException(NOT_FOUND_ERROR_MSG);
    }
    return config;
  }

  public void set(SourceControlConfiguration sourceControlConfiguration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      set(tx, sourceControlConfiguration);
      tx.commit();
    }
  }

  public void set(TransactionContext tx, SourceControlConfiguration sourceControlConfiguration) {
    SourceControlConfiguration existing = getById(tx, SINGLETON_ENTITY_ID);
    if (existing == null) {
      insert(tx, sourceControlConfiguration);
    }
    else {
      update(tx, sourceControlConfiguration);
    }
  }

  @Override
  public int insert(TransactionContext tx, SourceControlConfiguration configuration) {
    validate(configuration);
    configuration.setId(SINGLETON_ENTITY_ID);
    return super.insert(tx, configuration);
  }

  @Override
  public int update(TransactionContext tx, SourceControlConfiguration configuration) {
    validate(configuration);
    configuration.setId(SINGLETON_ENTITY_ID);
    return super.update(tx, configuration);
  }

  public void delete() {
    SourceControlConfiguration configuration = get();
    if (configuration != null) {
      delete(configuration);
    }
  }

  // Visible for testing
  void validate(SourceControlConfiguration config) {
    if (config == null) {
      throw new BadRequestException(NO_CONFIG_ERROR_MSG);
    }
    if (StringUtils.isBlank(config.getCloneDirectory())) {
      throw new BadRequestException(NO_CLONE_DIRECTORY_ERROR_MSG);
    }
    if (config.getCloneDirectory().length() > MAX_CLONE_DIRECTORY_LENGTH) {
      throw new BadRequestException(LONG_CLONE_DIRECTORY_ERROR_MSG);
    }
    if (StringUtils.isWhitespace(config.getGitExecutable())) {
      throw new BadRequestException(WHITESPACE_GIT_EXECUTABLE_ERROR_MSG);
    }
    if (StringUtils.length(config.getGitExecutable()) > MAX_GIT_EXECUTABLE_LENGTH) {
      throw new BadRequestException(LONG_GIT_EXECUTABLE_ERROR_MSG);
    }
    if (StringUtils.isWhitespace(config.getCommitUsername())) {
      throw new BadRequestException(WHITESPACE_COMMIT_USERNAME_ERROR_MSG);
    }
    if (StringUtils.length(config.getCommitUsername()) > MAX_COMMIT_USERNAME_LENGTH) {
      throw new BadRequestException(LONG_COMMIT_USERNAME_ERROR_MSG);
    }
    if (StringUtils.isWhitespace(config.getCommitEmail())) {
      throw new BadRequestException(WHITESPACE_COMMIT_EMAIL_ERROR_MSG);
    }
    if (StringUtils.length(config.getCommitEmail()) > MAX_COMMIT_EMAIL_LENGTH) {
      throw new BadRequestException(LONG_COMMIT_EMAIL_ERROR_MSG);
    }
    if (config.getCommitEmail() != null) {
      try {
        new InternetAddress(config.getCommitEmail(), true);
      }
      catch (Exception e) {
        throw new BadRequestException(INVALID_COMMIT_EMAIL_ERROR_MSG + " " + e.getMessage(), e);
      }
    }
    if (config.getDefaultBranchMonitoringIntervalHours() < MIN_DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS) {
      throw new BadRequestException(LOW_DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS);
    }
    if (config.getPullRequestMonitoringIntervalSeconds() < MIN_PULL_REQUEST_MONITORING_INTERVAL_SECONDS) {
      throw new BadRequestException(LOW_PULL_REQUEST_MONITORING_INTERVAL_SECONDS);
    }
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final SourceControlConfiguration entity) {
    record.from(entity);
    // Fix: record.from() uses Java bean introspection and maps the computed getDefaultBranchMonitoringStartTime()
    // getter (which returns LocalTime) to the default_branch_monitoring_start_time column (which is VARCHAR).
    // We need the actual String field value instead.
    record.set(SOURCE_CONTROL_CONFIGURATION.DEFAULT_BRANCH_MONITORING_START_TIME,
        entity.getDefaultBranchMonitoringStartTimeString());
    return record;
  }

  @Override
  public Table<?> getJooqTable() {
    return SOURCE_CONTROL_CONFIGURATION;
  }

  @Override
  public Class<SourceControlConfiguration> getEntityClass() {
    return SourceControlConfiguration.class;
  }
}
