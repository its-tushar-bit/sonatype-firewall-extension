/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;

import java.util.ArrayList;
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

  private ZscalerFormatDAO zscalerFormatDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createZScalerConfigurationDAO();
    zscalerFormatDAO = daoFactory.createZscalerFormatDAO();
  }

  @After
  public void exit() {
    dao.delete();
  }

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();
    assertThat(zscalerFormatDAO.getAll()).isEmpty();

    ZScalerConfiguration config = new ZScalerConfiguration();
    config.setUsername("testuser");
    config.setPassword("testpass");
    config.setHostname("testhost");
    config.setApikey("testapikey");
    List<ZscalerFormat> zscalerFormats = new ArrayList<>();
    zscalerFormats.add(new ZscalerFormat("maven", true));
    zscalerFormats.add(new ZscalerFormat("npm", true));
    zscalerFormats.add(new ZscalerFormat("pypi", false));
    zscalerFormats.add(new ZscalerFormat("nuget", false));
    dao.set(config, zscalerFormats);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getUsername()).isEqualTo("testuser");
    assertThat(config.getPassword()).isEqualTo("testpass");
    assertThat(config.getHostname()).isEqualTo("testhost");
    assertThat(config.getApikey()).isEqualTo("testapikey");
    List<ZscalerFormat> configuredFormats = zscalerFormatDAO.getAll();
    assertThat(configuredFormats).hasSize(4);
    for (ZscalerFormat format : configuredFormats) {
      switch (format.getFormat()) {
        case "maven", "npm" -> assertThat(format.isEnabled()).isTrue();
        case "nuget", "pypi" -> assertThat(format.isEnabled()).isFalse();
        default -> throw new AssertionError("Unexpected format: " + format.getFormat());
      }
    }

    config.setHostname("newhost");
    config.setApikey("newapikey");
    for (ZscalerFormat format : configuredFormats) {
      if ("pypi".equals(format.getFormat()) || "nuget".equals(format.getFormat())) {
        format.setEnabled(true);
      }
    }
    dao.set(config, configuredFormats);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getHostname()).isEqualTo("newhost");
    assertThat(config.getApikey()).isEqualTo("newapikey");
    List<ZscalerFormat> updatedConfiguredFormats = zscalerFormatDAO.getAll();
    assertThat(configuredFormats).hasSize(4);
    for (ZscalerFormat format : updatedConfiguredFormats) {
      switch (format.getFormat()) {
        case "maven", "npm", "pypi", "nuget" -> assertThat(format.isEnabled()).isTrue();
        default -> throw new AssertionError("Unexpected format: " + format.getFormat());
      }
    }

    dao.delete();

    assertThat(dao.get()).isNull();
    assertThat(zscalerFormatDAO.getAll()).isEmpty();

    dao.delete();
    // No exception should be thrown
    assertThat(dao.get()).isNull();
    assertThat(zscalerFormatDAO.getAll()).isEmpty();
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
        .extracting(ZScalerConfiguration::getId)
        .containsExactly(SINGLETON_ENTITY_ID);
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
