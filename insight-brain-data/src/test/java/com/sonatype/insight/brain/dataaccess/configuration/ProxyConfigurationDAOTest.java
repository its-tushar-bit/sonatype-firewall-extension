/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.ProxyConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ProxyConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private ProxyConfigurationDAO dao = new ProxyConfigurationDAO();

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    proxyConfiguration.setUsername("testuser");
    proxyConfiguration.setPassword("testpass".toCharArray());
    proxyConfiguration.setExcludeHosts("exclude.this");
    dao.set(proxyConfiguration);

    proxyConfiguration = dao.get();
    assertThat(proxyConfiguration).isNotNull();
    assertThat(proxyConfiguration.getHostname()).isEqualTo("localhost");
    assertThat(proxyConfiguration.getPort()).isEqualTo(1984);
    assertThat(proxyConfiguration.getUsername()).isEqualTo("testuser");
    assertThat(proxyConfiguration.getPassword()).isEqualTo("testpass".toCharArray());
    assertThat(proxyConfiguration.getExcludeHosts()).isEqualTo("exclude.this");

    proxyConfiguration.setHostname("example.com");
    proxyConfiguration.setPort(4891);
    proxyConfiguration.setUsername("testuser-updated");
    proxyConfiguration.setPassword("testpass-updated".toCharArray());
    proxyConfiguration.setExcludeHosts("exclude.this-updated");
    dao.set(proxyConfiguration);

    proxyConfiguration = dao.get();
    assertThat(proxyConfiguration).isNotNull();
    assertThat(proxyConfiguration.getHostname()).isEqualTo("example.com");
    assertThat(proxyConfiguration.getPort()).isEqualTo(4891);
    assertThat(proxyConfiguration.getUsername()).isEqualTo("testuser-updated");
    assertThat(proxyConfiguration.getPassword()).isEqualTo("testpass-updated".toCharArray());
    assertThat(proxyConfiguration.getExcludeHosts()).isEqualTo("exclude.this-updated");

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

  private ProxyConfiguration newValidProxyConfiguration() {
    ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
    proxyConfiguration.setHostname("localhost");
    proxyConfiguration.setPort(1984);
    return proxyConfiguration;
  }

  private void testInsert_ValidateHostname(String hostname) {
    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    proxyConfiguration.setHostname(hostname);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(proxyConfiguration);
    }).withMessage("Host is required.");
  }

  @Test
  public void testInsert_ValidatePort_LowerBound() {
    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    proxyConfiguration.setPort(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(proxyConfiguration);
    }).withMessage("The port must be from the range 1 - 65535.");

    proxyConfiguration.setPort(1);
    dao.insert(proxyConfiguration);
  }

  @Test
  public void testInsert_ValidatePort_UpperBound() {
    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    proxyConfiguration.setPort(65536);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.insert(proxyConfiguration);
    }).withMessage("The port must be from the range 1 - 65535.");

    proxyConfiguration.setPort(65535);
    dao.insert(proxyConfiguration);
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
    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    dao.insert(proxyConfiguration);
    proxyConfiguration.setHostname(hostname);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.update(proxyConfiguration);
    }).withMessage("Host is required.");
  }

  @Test
  public void testUpdate_ValidatePort_LowerBound() {
    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    dao.insert(proxyConfiguration);
    proxyConfiguration.setPort(0);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.update(proxyConfiguration);
    }).withMessage("The port must be from the range 1 - 65535.");

    proxyConfiguration.setPort(1);
    dao.update(proxyConfiguration);
  }

  @Test
  public void testUpdate_ValidatePort_UpperBound() {
    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    dao.insert(proxyConfiguration);
    proxyConfiguration.setPort(65536);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      dao.update(proxyConfiguration);
    }).withMessage("The port must be from the range 1 - 65535.");

    proxyConfiguration.setPort(65535);
    dao.update(proxyConfiguration);
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(newValidProxyConfiguration());

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
      dao.insert(newValidProxyConfiguration());
    }).withCauseInstanceOf(EntityExistsException.class);

    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    proxyConfiguration.setId("not-singleton-id");
    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
      dao.insert(proxyConfiguration);
    }).withCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(newValidProxyConfiguration());

    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    proxyConfiguration.setId("not-singleton-id");
    dao.update(proxyConfiguration);
    assertThat(dao.createQuery("SELECT entity FROM ProxyConfiguration entity").getList())
        .extracting(ProxyConfiguration::getId).containsExactly(ProxyConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testSet_UpdateSingleton() {
    dao.set(newValidProxyConfiguration());
    ProxyConfiguration proxyConfiguration = newValidProxyConfiguration();
    proxyConfiguration.setHostname("singleton");
    dao.set(proxyConfiguration);
    assertThat(dao.get().getHostname()).isEqualTo("singleton");
  }
}
