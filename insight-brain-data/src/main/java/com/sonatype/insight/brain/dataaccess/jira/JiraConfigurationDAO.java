/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.jira;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.139
 */
@Named
@Singleton
public class JiraConfigurationDAO
    extends AbstractOperationalSqlDAO<JiraConfiguration>
    implements RotatableSecrets
{
  public static final String SINGLETON_ENTITY_ID = "jira-configuration";

  // Visible for testing
  static final String NO_CONFIG_ERROR_MSG = "A JIRA configuration must be given.";

  // Visible for testing
  static final String BLANK_URL_ERROR_MSG = "The JIRA server address is required.";

  // Visible for testing
  static final String INVALID_URL_ERROR_MSG = "The JIRA server address is invalid.";

  // Visible for testing
  static final int MAX_URL_LENGTH = 2048;

  // Visible for testing
  static final String LONG_URL_ERROR_MSG = "The JIRA server address cannot exceed 2048 characters.";

  // Visible for testing
  static final int MAX_USERNAME_LENGTH = 255;

  // Visible for testing
  static final String LONG_USERNAME_ERROR_MSG = "The username cannot exceed 255 characters.";

  // Visible for testing
  static final String EMPTY_OR_WHITESPACE_USERNAME_ERROR_MSG = "The username cannot be empty or only whitespace.";

  // Visible for testing
  static final int MAX_PASSWORD_LENGTH = 2000; // This is maximum encrypted password length allowed..

  // Visible for testing
  static final String LONG_PASSWORD_ERROR_MSG = "The password is too long.";

  // Visible for testing
  static final String EMPTY_OR_WHITESPACE_PASSWORD_ERROR_MSG = "The password cannot be empty or only whitespace.";

  // Visible for testing
  static final int MAX_CUSTOM_FIELDS_JSON_LENGTH = 8192;

  // Visible for testing
  static final String LONG_CUSTOM_FIELDS_JSON_ERROR_MSG = "The custom fields json cannot exceed 8192 characters.";

  public static final String NOT_FOUND_ERROR_MSG = "JIRA not configured.";

  @Inject
  public JiraConfigurationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public JiraConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public JiraConfiguration getNotNull() {
    JiraConfiguration config = get();
    if (config == null) {
      throw new NotFoundException(NOT_FOUND_ERROR_MSG);
    }
    return config;
  }

  @Override
  public JiraConfiguration getById(TransactionContext tx, String id) {
    return super.getById(tx, SINGLETON_ENTITY_ID);
  }

  public void set(JiraConfiguration jiraConfiguration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      set(tx, jiraConfiguration);
      tx.commit();
    }
  }

  public void set(TransactionContext tx, JiraConfiguration jiraConfiguration) {
    update(tx, jiraConfiguration);
  }

  @Override
  public void insert(TransactionContext tx, JiraConfiguration jiraConfiguration) {
    validate(jiraConfiguration);
    jiraConfiguration.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, jiraConfiguration);
  }

  @Override
  public void update(TransactionContext tx, JiraConfiguration jiraConfiguration) {
    validate(jiraConfiguration);
    jiraConfiguration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, jiraConfiguration);
  }

  public void validate(JiraConfiguration jiraConfiguration) {
    if (jiraConfiguration == null) {
      throw new BadRequestException(NO_CONFIG_ERROR_MSG);
    }
    if (StringUtils.isBlank(jiraConfiguration.getUrl())) {
      throw new BadRequestException(BLANK_URL_ERROR_MSG);
    }
    try {
      new URL(jiraConfiguration.getUrl()).toURI();
    }
    catch (MalformedURLException | URISyntaxException e) {
      throw new BadRequestException(INVALID_URL_ERROR_MSG, e);
    }
    if (jiraConfiguration.getUrl().length() > MAX_URL_LENGTH) {
      throw new BadRequestException(LONG_URL_ERROR_MSG);
    }
    if (StringUtils.isWhitespace(jiraConfiguration.getUsername())) {
      throw new BadRequestException(EMPTY_OR_WHITESPACE_USERNAME_ERROR_MSG);
    }
    if (jiraConfiguration.getPassword() != null &&
        StringUtils.isWhitespace(CharBuffer.wrap(jiraConfiguration.getPassword())))
    {
      throw new BadRequestException(EMPTY_OR_WHITESPACE_PASSWORD_ERROR_MSG);
    }
    if (jiraConfiguration.getUsername() != null && jiraConfiguration.getUsername().length() > MAX_USERNAME_LENGTH) {
      throw new BadRequestException(LONG_USERNAME_ERROR_MSG);
    }
    if (jiraConfiguration.getPassword() != null && jiraConfiguration.getPassword().length > MAX_PASSWORD_LENGTH) {
      throw new BadRequestException(LONG_PASSWORD_ERROR_MSG);
    }
    if (jiraConfiguration.getCustomFieldsJson() != null &&
        jiraConfiguration.getCustomFieldsJson().length() > MAX_CUSTOM_FIELDS_JSON_LENGTH)
    {
      throw new BadRequestException(LONG_CUSTOM_FIELDS_JSON_ERROR_MSG);
    }
  }

  public void delete() {
    JiraConfiguration jiraConfiguration = get();
    if (jiraConfiguration != null) {
      delete(jiraConfiguration);
    }
  }
}
