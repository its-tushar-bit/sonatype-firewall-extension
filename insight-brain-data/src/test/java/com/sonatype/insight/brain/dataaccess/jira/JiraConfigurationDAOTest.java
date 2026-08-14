/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.jira;

import java.sql.SQLException;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class JiraConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private JiraConfigurationDAO dao;

  private DAOSecretRotator daoSecretRotator;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createJiraConfigurationDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testCRUD() {
    // Create
    JiraConfiguration config = new JiraConfiguration(
        createUrlWithLength(MAX_URL_LENGTH),
        StringUtils.repeat("b", MAX_USERNAME_LENGTH),
        StringUtils.repeat("c", MAX_PASSWORD_LENGTH).toCharArray(),
        null);
    config.setCustomFieldsJson(createJsonWithLength(MAX_CUSTOM_FIELDS_JSON_LENGTH));
    dao.insert(config);
    assertThat(config.getId()).isNotNull();

    // Read
    assertThat(dao.get()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(config);

    // Update
    config.setUrl("http://other");
    config.setUsername(null);
    config.setPassword(null);
    config.setCustomFields(null);
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
    JiraConfiguration config = new JiraConfiguration();
    config.setUrl("http://url");
    dao.insert(config);

    assertThat(dao.getNotNull()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(config);
  }

  @Test
  public void testInsert_EnforceSingleton() {
    JiraConfiguration jiraConfiguration1 = new JiraConfiguration();
    jiraConfiguration1.setUrl("http://url");

    dao.insert(jiraConfiguration1);

    JiraConfiguration jiraConfiguration2 = new JiraConfiguration();
    jiraConfiguration2.setUrl("http://url");
    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(jiraConfiguration2));
    JiraConfiguration jiraConfiguration3 = new JiraConfiguration();
    jiraConfiguration3.setUrl("http://url");
    jiraConfiguration3.setId(TemporaryEntity.uuid());
    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(jiraConfiguration3));
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    JiraConfiguration jiraConfiguration1 = new JiraConfiguration();
    jiraConfiguration1.setUrl("http://url");
    dao.insert(jiraConfiguration1);
    JiraConfiguration jiraConfiguration2 = new JiraConfiguration();
    jiraConfiguration2.setUrl("http://url");
    jiraConfiguration2.setId(TemporaryEntity.uuid());

    dao.update(jiraConfiguration2);

    assertThat(dao.getAll())
        .extracting(JiraConfiguration::getId)
        .containsExactly(JiraConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testInsert_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(null))
        .withMessageContaining(NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testInsert_NullUrl() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(new JiraConfiguration()))
        .withMessageContaining(BLANK_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_EmptyUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(BLANK_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_BlankUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(BLANK_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_InvalidUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("invalid");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(INVALID_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_LongUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl(createUrlWithLength(MAX_URL_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(LONG_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_LongUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername(StringUtils.repeat("a", MAX_USERNAME_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(LONG_USERNAME_ERROR_MSG);
  }

  @Test
  public void testInsert_NullUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername(null);

    dao.insert(jiraConfiguration);

    assertThat(jiraConfiguration.getId()).isNotNull();
  }

  @Test
  public void testInsert_EmptyUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_USERNAME_ERROR_MSG);
  }

  @Test
  public void testInsert_WhitespaceUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_USERNAME_ERROR_MSG);
  }

  @Test
  public void testInsert_LongPassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword(StringUtils.repeat("a", MAX_PASSWORD_LENGTH + 1).toCharArray());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(LONG_PASSWORD_ERROR_MSG);
  }

  @Test
  public void testInsert_NullPassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword(null);

    dao.insert(jiraConfiguration);

    assertThat(jiraConfiguration.getId()).isNotNull();
  }

  @Test
  public void testInsert_EmptyPassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword("".toCharArray());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_PASSWORD_ERROR_MSG);
  }

  @Test
  public void testInsert_WhitespacePassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword(" ".toCharArray());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_PASSWORD_ERROR_MSG);
  }

  @Test
  public void testInsert_LongCustomFieldsJson() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setCustomFieldsJson(createJsonWithLength(MAX_CUSTOM_FIELDS_JSON_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(jiraConfiguration))
        .withMessageContaining(LONG_CUSTOM_FIELDS_JSON_ERROR_MSG);
  }

  @Test
  public void testUpdate_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(null))
        .withMessageContaining(NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testUpdate_NullUrl() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(new JiraConfiguration()))
        .withMessageContaining(BLANK_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_EmptyUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(BLANK_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_BlankUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(BLANK_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_InvalidUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("invalid");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(INVALID_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_LongUrl() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl(createUrlWithLength(MAX_URL_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(LONG_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_LongUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername(StringUtils.repeat("a", MAX_USERNAME_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(LONG_USERNAME_ERROR_MSG);
  }

  @Test
  public void testUpdate_NullUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername(null);

    dao.update(jiraConfiguration);

    assertThat(jiraConfiguration.getId()).isNotNull();
  }

  @Test
  public void testUpdate_EmptyUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_USERNAME_ERROR_MSG);
  }

  @Test
  public void testUpdate_WhitespaceUsername() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setUsername(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_USERNAME_ERROR_MSG);
  }

  @Test
  public void testUpdate_LongPassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword(StringUtils.repeat("a", MAX_PASSWORD_LENGTH + 1).toCharArray());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(LONG_PASSWORD_ERROR_MSG);
  }

  @Test
  public void testUpdate_NullPassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword(null);

    dao.update(jiraConfiguration);

    assertThat(jiraConfiguration.getId()).isNotNull();
  }

  @Test
  public void testUpdate_EmptyPassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword("".toCharArray());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_PASSWORD_ERROR_MSG);
  }

  @Test
  public void testUpdate_WhitespacePassword() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setPassword(" ".toCharArray());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(EMPTY_OR_WHITESPACE_PASSWORD_ERROR_MSG);
  }

  @Test
  public void testUpdate_LongCustomFieldsJson() {
    JiraConfiguration jiraConfiguration = new JiraConfiguration();
    jiraConfiguration.setUrl("http://url");
    jiraConfiguration.setCustomFieldsJson(createJsonWithLength(MAX_CUSTOM_FIELDS_JSON_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(jiraConfiguration))
        .withMessageContaining(LONG_CUSTOM_FIELDS_JSON_ERROR_MSG);
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    tempEntity.newJiraConfiguration("http://url", "userName", "passwordOld1".toCharArray(), null);
    Function<String, String> secretRotator = secret -> secret.replace("Old", "New");

    daoSecretRotator.rotateEncryptedSecrets(dao, secretRotator);

    JiraConfiguration result = dao.get();
    assertThat(String.valueOf(result.getPassword())).isEqualTo("passwordNew1");
  }

  private String createUrlWithLength(int length) {
    return "http://" + StringUtils.repeat("a", length - 7);
  }

  private String createJsonWithLength(int length) {
    return "{\"f\":\"" + StringUtils.repeat("a", length - 8) + "\"}";
  }
}
