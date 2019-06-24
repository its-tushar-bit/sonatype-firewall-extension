/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RootOrganizationConfigMigrationResourceTest
    extends AbstractResourceTest
{
  private Organization org;

  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    migrationUtils = new RootOrganizationConfigMigrationUtils(new MigrationTrackerDAO());
    clean();
  }

  @After
  public void tearDown() {
    clean();
    migrationUtils.setMigrated();
  }

  @Test
  public void testSetRootOrganizationTemplate() throws Exception {
    HttpResponse response = restRequest().path(RootOrganizationConfigMigrationResource.RESOURCE_PATH, org.getId())
        .post();
    assertResponseStatus(204, response);
    assertThat(migrationUtils.isMigrationScheduled()).isTrue();
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() throws Exception {
    HttpResponse response = restRequest().path(RootOrganizationConfigMigrationResource.RESOURCE_PATH).post();
    assertResponseStatus(204, response);
    assertThat(migrationUtils.isMigrated()).isTrue();
  }

  public void clean() {
    MigrationTrackerDAO migrationTrackerDAO = new MigrationTrackerDAO();
    migrationTrackerDAO.delete(migrationTrackerDAO.getById(RootOrganizationConfigMigrationUtils.MIGRATION_CONFIG_ID));
    migrationTrackerDAO.delete(migrationTrackerDAO.getById(RootOrganizationConfigMigrationUtils.MIGRATION_ID));
  }
}
