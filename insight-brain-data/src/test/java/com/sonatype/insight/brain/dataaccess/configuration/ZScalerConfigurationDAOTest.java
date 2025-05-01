/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO.SINGLETON_ENTITY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ZScalerConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private ZScalerConfigurationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createZScalerConfigurationDAO();
  }

  @After
  public void exit() {
    dao.delete();
  }

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    ZScalerConfiguration config = new ZScalerConfiguration();
    config.setUsername("testuser");
    config.setPassword("testpass");
    config.setHostname("testhost");
    config.setApikey("testapikey");
    dao.set(config);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getUsername()).isEqualTo("testuser");
    assertThat(config.getPassword()).isEqualTo("testpass");
    assertThat(config.getHostname()).isEqualTo("testhost");
    assertThat(config.getApikey()).isEqualTo("testapikey");

    config.setHostname("newhost");
    config.setApikey("newapikey");
    dao.set(config);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getHostname()).isEqualTo("newhost");
    assertThat(config.getApikey()).isEqualTo("newapikey");

    dao.delete();

    assertThat(dao.get()).isNull();

    dao.delete();
    // No exception should be thrown
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
    testInsert_ValidateHostname("  ");
  }

  private void testInsert_ValidateHostname(String hostname) {
    ZScalerConfiguration config = newValidConfiguration();
    config.setHostname(hostname);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(config))
        .withMessageContaining("The zScaler host is required.");
  }

  @Test
  public void testInsert_EnforceSingleton() {
    dao.insert(newValidConfiguration());

    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> dao.insert(newValidConfiguration()))
        .withCauseInstanceOf(EntityExistsException.class);

    ZScalerConfiguration config = newValidConfiguration();
    config.setId("not-singleton-id");
    assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> dao.insert(config))
        .withCauseInstanceOf(EntityExistsException.class);
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(newValidConfiguration());

    ZScalerConfiguration config = newValidConfiguration();
    config.setId("not-singleton-id");
    dao.update(config);
    assertThat(dao.createQuery("SELECT entity FROM ZScalerConfiguration entity").getList())
        .extracting(ZScalerConfiguration::getId).containsExactly(SINGLETON_ENTITY_ID);
  }

  @Test
  public void testQueryValidation() {
    ZScalerConfiguration config = newValidConfiguration();
    dao.insert(config);

    List<ZScalerConfiguration> result = dao.createQuery(ZScalerConfigurationDAO.QUERY, SINGLETON_ENTITY_ID).getList();
    assertThat(result).isNotEmpty();
    assertThat(result.get(0).getId()).isEqualTo(SINGLETON_ENTITY_ID);
  }

  private ZScalerConfiguration newValidConfiguration() {
    ZScalerConfiguration config = new ZScalerConfiguration();
    config.setUsername("testuser");
    config.setPassword("testpass");
    config.setHostname("testhost");
    config.setApikey("testapikey");
    return config;
  }
}
