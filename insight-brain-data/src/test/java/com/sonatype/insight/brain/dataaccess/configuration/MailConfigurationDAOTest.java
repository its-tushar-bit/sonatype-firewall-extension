/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.MailConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class MailConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private MailConfigurationDAO dao = new MailConfigurationDAO();

  @After
  public void exit() {
    dao.delete();
  }

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    MailConfiguration config = new MailConfiguration();
    config.setHostname("testhost");
    config.setPort(12345);
    config.setUsername("testuser");
    config.setPassword("testpass");
    config.setSystemEmail("test@localhost");
    config.setSslEnabled(true);

    dao.set(config);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getHostname()).isEqualTo("testhost");
    assertThat(config.getPort()).isEqualTo(12345);
    assertThat(config.getUsername()).isEqualTo("testuser");
    assertThat(config.getPassword()).isEqualTo("testpass");
    assertThat(config.isSslEnabled()).isEqualTo(true);
    assertThat(config.isStartTlsEnabled()).isEqualTo(false);
    assertThat(config.getSystemEmail()).isEqualTo("test@localhost");

    config.setPassword("secret");
    config.setPort(54321);
    config.setStartTlsEnabled(true);
    dao.set(config);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getHostname()).isEqualTo("testhost");
    assertThat(config.getPort()).isEqualTo(54321);
    assertThat(config.getUsername()).isEqualTo("testuser");
    assertThat(config.getPassword()).isEqualTo("secret");
    assertThat(config.isSslEnabled()).isEqualTo(true);
    assertThat(config.isStartTlsEnabled()).isEqualTo(true);
    assertThat(config.getSystemEmail()).isEqualTo("test@localhost");

    dao.delete();

    assertThat(dao.get()).isNull();
  }

  private MailConfiguration newValidConfiguration() {
    MailConfiguration config = new MailConfiguration();
    config.setHostname("localhost");
    config.setPort(12345);
    config.setSystemEmail("test@localhost");
    return config;
  }

  @Test
  public void testInsert_ValidateHostname_Null() {
    testInsert_ValidateHostname(null);
  }

  @Test
  public void testInsert_ValidateHostname_Empty() {
    testInsert_ValidateHostname("");
  }

  @Test
  public void testInsert_ValidateHostname_Blank() {
    testInsert_ValidateHostname("  ");
  }

  private void testInsert_ValidateHostname(String hostname) {
    MailConfiguration config = newValidConfiguration();
    config.setHostname(hostname);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(config);
    }).withMessageContaining("host is required");
  }

  @Test
  public void testInsert_ValidatePort_LowerBound() {
    MailConfiguration config = newValidConfiguration();
    config.setPort(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(config);
    }).withMessageContaining("port must be from the range 1 - 65535");

    config.setPort(1);
    dao.insert(config);
  }

  @Test
  public void testInsert_ValidatePort_UpperBound() {
    MailConfiguration config = newValidConfiguration();
    config.setPort(65536);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(config);
    }).withMessageContaining("port must be from the range 1 - 65535");

    config.setPort(65535);
    dao.insert(config);
  }

  @Test
  public void testInsert_ValidateSystemEmail_Null() {
    testInsert_ValidateSystemEmail(null);
  }

  @Test
  public void testInsert_ValidateSystemEmail_Empty() {
    testInsert_ValidateSystemEmail("");
  }

  @Test
  public void testInsert_ValidateSystemEmail_Blank() {
    testInsert_ValidateSystemEmail("  ");
  }

  private void testInsert_ValidateSystemEmail(String systemEmail) {
    MailConfiguration config = newValidConfiguration();
    config.setSystemEmail(systemEmail);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(config);
    }).withMessageContaining("system email address is required");
  }

  @Test
  public void testInsert_Validate_SystemEmail_Malformed() {
    MailConfiguration config = newValidConfiguration();
    config.setSystemEmail("malformed address");
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(config);
    }).withMessageContaining("system email address is malformed");
  }

  @Test
  public void testUpdate_ValidateHostname_Null() {
    testUpdate_ValidateHostname(null);
  }

  @Test
  public void testUpdate_ValidateHostname_Empty() {
    testUpdate_ValidateHostname("");
  }

  @Test
  public void testUpdate_ValidateHostname_Blank() {
    testUpdate_ValidateHostname("  ");
  }

  private void testUpdate_ValidateHostname(String hostname) {
    MailConfiguration config = newValidConfiguration();
    dao.insert(config);
    config.setHostname(hostname);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.update(config);
    }).withMessageContaining("host is required");
  }

  @Test
  public void testUpdate_ValidatePort_LowerBound() {
    MailConfiguration config = newValidConfiguration();
    dao.insert(config);
    config.setPort(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.update(config);
    }).withMessageContaining("port must be from the range 1 - 65535");

    config.setPort(1);
    dao.update(config);
  }

  @Test
  public void testUpdate_ValidatePort_UpperBound() {
    MailConfiguration config = newValidConfiguration();
    dao.insert(config);
    config.setPort(65536);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.update(config);
    }).withMessageContaining("port must be from the range 1 - 65535");

    config.setPort(65535);
    dao.update(config);
  }

  @Test
  public void testUpdate_ValidateSystemEmail_Null() {
    testUpdate_ValidateSystemEmail(null, "system email address is required");
  }

  @Test
  public void testUpdate_ValidateSystemEmail_Empty() {
    testUpdate_ValidateSystemEmail("", "system email address is required");
  }

  @Test
  public void testUpdate_ValidateSystemEmail_Blank() {
    testUpdate_ValidateSystemEmail("  ", "system email address is required");
  }

  @Test
  public void testUpdate_Validate_SystemEmail_Malformed() {
    testUpdate_ValidateSystemEmail("malformed address", "system email address is malformed");
  }

  private void testUpdate_ValidateSystemEmail(String systemEmail, String errMessage) {
    MailConfiguration config = newValidConfiguration();
    dao.insert(config);
    config.setSystemEmail(systemEmail);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.update(config);
    }).withMessageContaining(errMessage);
  }
}
