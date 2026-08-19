/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import org.jooq.exception.IntegrityConstraintViolationException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.EMPTY_LOGOUT_URL_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.INVALID_LOGOUT_URL_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.LONG_LOGOUT_URL_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.LONG_USERNAME_HEADER_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.MAX_LOGOUT_URL_LENGTH;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.MAX_USERNAME_HEADER_LENGTH;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.NOT_FOUND_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.NO_CONFIG_ERROR_MSG;
import static com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO.NO_USERNAME_HEADER_ERROR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ReverseProxyAuthenticationConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private ReverseProxyAuthenticationConfigurationDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createReverseProxyAuthenticationConfigurationDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration(true,
        StringUtils.repeat("a", MAX_USERNAME_HEADER_LENGTH), true, StringUtils.repeat("b", MAX_LOGOUT_URL_LENGTH));
    dao.insert(config);
    assertThat(config.getId()).isNotNull();

    // Read
    assertThat(dao.get()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(config);

    // Update
    config.setEnabled(false);
    config.setUsernameHeader(StringUtils.repeat("c", MAX_USERNAME_HEADER_LENGTH));
    config.setCsrfProtectionDisabled(false);
    config.setLogoutUrl(null);
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
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    dao.insert(config);

    assertThat(dao.getNotNull()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(config);
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(new ReverseProxyAuthenticationConfiguration());

    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(new ReverseProxyAuthenticationConfiguration()));
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setId(TemporaryEntity.uuid());
    assertThatExceptionOfType(IntegrityConstraintViolationException.class).isThrownBy(() -> dao.insert(config));
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(new ReverseProxyAuthenticationConfiguration());

    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setId(TemporaryEntity.uuid());
    dao.update(config);
    assertThat(dao.getAll())
        .extracting(ReverseProxyAuthenticationConfiguration::getId)
        .containsExactly(ReverseProxyAuthenticationConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testInsert_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(null))
        .withMessageContaining(NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testInsert_UsernameHeader_Null() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader(null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(NO_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testInsert_UsernameHeader_Empty() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(NO_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testInsert_UsernameHeader_Blank() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(NO_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testInsert_UsernameHeader_TooLong() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader(StringUtils.repeat("a", MAX_USERNAME_HEADER_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(LONG_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testInsert_LogoutUrl_Empty() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(EMPTY_LOGOUT_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_LogoutUrl_Blank() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(EMPTY_LOGOUT_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_LogoutUrl_TooLong() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl(StringUtils.repeat("a", MAX_LOGOUT_URL_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(LONG_LOGOUT_URL_ERROR_MSG);
  }

  @Test
  public void testInsert_LogoutUrl_Invalid() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl(":");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining(INVALID_LOGOUT_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(null))
        .withMessageContaining(NO_CONFIG_ERROR_MSG);
  }

  @Test
  public void testUpdate_UsernameHeader_Null() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader(null);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(NO_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testUpdate_UsernameHeader_Empty() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(NO_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testUpdate_UsernameHeader_Blank() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(NO_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testUpdate_UsernameHeader_TooLong() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setUsernameHeader(StringUtils.repeat("a", MAX_USERNAME_HEADER_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(LONG_USERNAME_HEADER_ERROR_MSG);
  }

  @Test
  public void testUpdate_LogoutUrl_Empty() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl("");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(EMPTY_LOGOUT_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_LogoutUrl_Blank() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl(" ");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(EMPTY_LOGOUT_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_LogoutUrl_TooLong() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl(StringUtils.repeat("a", MAX_LOGOUT_URL_LENGTH + 1));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(LONG_LOGOUT_URL_ERROR_MSG);
  }

  @Test
  public void testUpdate_LogoutUrl_Invalid() {
    ReverseProxyAuthenticationConfiguration config = new ReverseProxyAuthenticationConfiguration();
    config.setLogoutUrl(":");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(config))
        .withMessageContaining(INVALID_LOGOUT_URL_ERROR_MSG);
  }
}
