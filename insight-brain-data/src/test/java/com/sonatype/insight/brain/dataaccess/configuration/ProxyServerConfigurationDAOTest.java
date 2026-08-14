/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.sql.SQLException;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DAOSecretRotator;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProxyServerConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private ProxyServerConfigurationDAO dao;

  private DAOSecretRotator daoSecretRotator;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createProxyServerConfigurationDAO();
    daoSecretRotator = new DAOSecretRotator();
  }

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    proxyServerConfiguration.setUsername("testuser");
    proxyServerConfiguration.setPassword("testpass".toCharArray());
    proxyServerConfiguration.setExcludeHosts("exclude.this");
    dao.set(proxyServerConfiguration);

    proxyServerConfiguration = dao.get();
    assertThat(proxyServerConfiguration).isNotNull();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo("localhost");
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(1984);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo("testuser");
    assertThat(proxyServerConfiguration.getPassword()).isEqualTo("testpass".toCharArray());
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("exclude.this");

    proxyServerConfiguration.setHostname("example.com");
    proxyServerConfiguration.setPort(4891);
    proxyServerConfiguration.setUsername("testuser-updated");
    proxyServerConfiguration.setPassword("testpass-updated".toCharArray());
    proxyServerConfiguration.setExcludeHosts("exclude.this-updated");
    dao.set(proxyServerConfiguration);

    proxyServerConfiguration = dao.get();
    assertThat(proxyServerConfiguration).isNotNull();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo("example.com");
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(4891);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo("testuser-updated");
    assertThat(proxyServerConfiguration.getPassword()).isEqualTo("testpass-updated".toCharArray());
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("exclude.this-updated");

    dao.delete();
    assertThat(dao.get()).isNull();
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
    testInsert_ValidateHostname("    ");
  }

  private ProxyServerConfiguration newValidProxyServerConfiguration() {
    ProxyServerConfiguration proxyServerConfiguration = new ProxyServerConfiguration();
    proxyServerConfiguration.setHostname("localhost");
    proxyServerConfiguration.setPort(1984);
    return proxyServerConfiguration;
  }

  private void testInsert_ValidateHostname(String hostname) {
    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    proxyServerConfiguration.setHostname(hostname);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(proxyServerConfiguration))
        .withMessage("Host is required.");
  }

  @Test
  public void testInsert_ValidatePort_LowerBound() {
    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    proxyServerConfiguration.setPort(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(proxyServerConfiguration))
        .withMessage("The port must be from the range 1 - 65535.");

    proxyServerConfiguration.setPort(1);
    dao.insert(proxyServerConfiguration);
  }

  @Test
  public void testInsert_ValidatePort_UpperBound() {
    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    proxyServerConfiguration.setPort(65536);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(proxyServerConfiguration))
        .withMessage("The port must be from the range 1 - 65535.");

    proxyServerConfiguration.setPort(65535);
    dao.insert(proxyServerConfiguration);
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
    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    dao.insert(proxyServerConfiguration);
    proxyServerConfiguration.setHostname(hostname);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(proxyServerConfiguration))
        .withMessage("Host is required.");
  }

  @Test
  public void testUpdate_ValidatePort_LowerBound() {
    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    dao.insert(proxyServerConfiguration);
    proxyServerConfiguration.setPort(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(proxyServerConfiguration))
        .withMessage("The port must be from the range 1 - 65535.");

    proxyServerConfiguration.setPort(1);
    dao.update(proxyServerConfiguration);
  }

  @Test
  public void testUpdate_ValidatePort_UpperBound() {
    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    dao.insert(proxyServerConfiguration);
    proxyServerConfiguration.setPort(65536);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(proxyServerConfiguration))
        .withMessage("The port must be from the range 1 - 65535.");

    proxyServerConfiguration.setPort(65535);
    dao.update(proxyServerConfiguration);
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(newValidProxyServerConfiguration());

    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(newValidProxyServerConfiguration()));

    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    proxyServerConfiguration.setId("not-singleton-id");
    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(proxyServerConfiguration));
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(newValidProxyServerConfiguration());

    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    proxyServerConfiguration.setId("not-singleton-id");
    dao.update(proxyServerConfiguration);
    assertThat(dao.getAll())
        .extracting(ProxyServerConfiguration::getId)
        .containsExactly(ProxyServerConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testSet_UpdateSingleton() {
    dao.set(newValidProxyServerConfiguration());
    ProxyServerConfiguration proxyServerConfiguration = newValidProxyServerConfiguration();
    proxyServerConfiguration.setHostname("singleton");
    dao.set(proxyServerConfiguration);
    assertThat(dao.get().getHostname()).isEqualTo("singleton");
  }

  @Test
  public void testRotateEncryptedSecrets() throws SQLException {
    tempEntity.setProxyServerConfiguration("localhost", 1984, "userName", "passwordOld1".toCharArray(), "exclude.this");
    Function<String, String> secretRotator = secret -> secret.replace("Old", "New");

    daoSecretRotator.rotateEncryptedSecrets(dao, secretRotator);

    ProxyServerConfiguration result = dao.get();
    assertThat(String.valueOf(result.getPassword())).isEqualTo("passwordNew1");
  }
}
