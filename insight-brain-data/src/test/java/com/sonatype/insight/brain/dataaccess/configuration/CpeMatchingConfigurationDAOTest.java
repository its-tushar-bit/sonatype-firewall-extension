/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CpeMatchingConfiguration;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CpeMatchingConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private CpeMatchingConfigurationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createCpeMatchingConfigurationDAO();
  }

  @Test
  public void testCRUD() {
    Organization owner = tempEntity.newOrganization();
    CpeMatchingConfiguration cpeMatchingConfiguration = new CpeMatchingConfiguration(owner.getId());

    // create
    assertThat(cpeMatchingConfiguration.getId()).isNull();
    cpeMatchingConfiguration.setOwnerId(owner.getId());
    cpeMatchingConfiguration.setCpeEnabled(false);
    cpeMatchingConfiguration.setAllowOverride(true);
    dao.insert(cpeMatchingConfiguration);
    assertThat(cpeMatchingConfiguration.getId()).isNotNull();

    // read
    cpeMatchingConfiguration = dao.getById(cpeMatchingConfiguration.getId());
    assertThat(cpeMatchingConfiguration).isNotNull();
    assertThat(cpeMatchingConfiguration.getOwnerId()).isEqualTo(owner.getId());
    assertThat(cpeMatchingConfiguration.isCpeEnabled()).isFalse();
    assertThat(cpeMatchingConfiguration.isAllowOverride()).isTrue();

    // update
    cpeMatchingConfiguration.setCpeEnabled(true);
    dao.update(cpeMatchingConfiguration);
    cpeMatchingConfiguration = dao.getById(cpeMatchingConfiguration.getId());
    assertThat(cpeMatchingConfiguration.isCpeEnabled()).isTrue();

    // delete
    String id = cpeMatchingConfiguration.getId();
    dao.delete(cpeMatchingConfiguration);
    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void testDelete() {
    Organization owner = tempEntity.newOrganization();
    CpeMatchingConfiguration cpeMatchingConfiguration = new CpeMatchingConfiguration(owner.getId(), true, false);

    // create
    dao.insert(cpeMatchingConfiguration);

    // delete
    dao.delete(cpeMatchingConfiguration);

    // verify deletion
    assertThat(dao.getByOwnerId(cpeMatchingConfiguration.getOwnerId())).isNull();
  }
}
