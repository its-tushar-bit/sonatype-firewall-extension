/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import org.jooq.exception.IntegrityConstraintViolationException;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class RepositoryClientConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryClientConfigurationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryClientConfigurationDAO();
  }

  @After
  public void exit() {
    dao.delete();
  }

  @Test
  public void testCRUD() {
    assertThat(dao.get()).isNull();

    RepositoryClientConfiguration config = new RepositoryClientConfiguration();
    config.setConnectionTimeout(10);
    config.setSocketTimeout(20);
    dao.set(config);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getConnectionTimeout()).isEqualTo(10);
    assertThat(config.getSocketTimeout()).isEqualTo(20);

    config.setConnectionTimeout(15);
    config.setSocketTimeout(25);
    dao.set(config);

    config = dao.get();
    assertThat(config).isNotNull();
    assertThat(config.getConnectionTimeout()).isEqualTo(15);
    assertThat(config.getSocketTimeout()).isEqualTo(25);

    dao.delete();

    assertThat(dao.get()).isNull();
  }

  @Test
  public void testInsert_EnforceSingleton() {
    RepositoryClientConfiguration config = newValidConfiguration();
    dao.insert(config);

    assertThatExceptionOfType(IntegrityConstraintViolationException.class)
        .isThrownBy(() -> dao.insert(newValidConfiguration()));

    RepositoryClientConfiguration anotherConfig = newValidConfiguration();
    config.setId("not-singleton-id");
    assertThatExceptionOfType(IntegrityConstraintViolationException.class).isThrownBy(() -> dao.insert(anotherConfig));
  }

  @Test
  public void testUpdate_EnforceSingleton() {
    dao.insert(newValidConfiguration());

    RepositoryClientConfiguration config = newValidConfiguration();
    config.setId("not-singleton-id");
    dao.update(config);
    assertThat(dao.getAll())
        .extracting(RepositoryClientConfiguration::getId)
        .containsExactly(RepositoryClientConfigurationDAO.SINGLETON_ENTITY_ID);
  }

  @Test
  public void testSet_UpdateSingleton() {
    dao.set(newValidConfiguration());
    RepositoryClientConfiguration config = newValidConfiguration();
    config.setSocketTimeout(30);
    dao.set(config);
    assertThat(dao.get().getSocketTimeout()).isEqualTo(30);
  }

  private RepositoryClientConfiguration newValidConfiguration() {
    RepositoryClientConfiguration config = new RepositoryClientConfiguration();
    config.setConnectionTimeout(10);
    config.setSocketTimeout(10);
    return config;
  }
}
