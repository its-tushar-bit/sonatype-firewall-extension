/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RootOrganizationConfigMigrationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig insightConfig;

  private RootOrganizationConfigMigrationService service;

  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Before
  public void setup() throws IOException {
    insightConfig.setSonatypeWork(tempDir.newFolder().getAbsolutePath());
    InsightWork insightWork = new InsightWork(insightConfig);
    migrationUtils = new RootOrganizationConfigMigrationUtils(insightWork);

    service = new RootOrganizationConfigMigrationService(new OrganizationDAO(), migrationUtils);
  }

  @Test
  public void testSetRootOrganizationTemplate() throws IOException {
    Organization org = tempEntity.newOrganization();
    service.setRootOrganizationTemplate(org.getId());

    assertTrue(migrationUtils.isMigrationScheduled());
    assertThat(migrationUtils.getSourceOrganizationId(), is(org.getId()));
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyScheduled() throws IOException {
    migrationUtils.setSourceOrganizationId("bla");
    Organization org = tempEntity.newOrganization();
    try {
      service.setRootOrganizationTemplate(org.getId());
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
    assertThat(migrationUtils.getSourceOrganizationId(), not(is(org.getId())));
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyMigrated() throws IOException {
    migrationUtils.setMigrated();
    Organization org = tempEntity.newOrganization();
    try {
      service.setRootOrganizationTemplate(org.getId());
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
  }

  @Test
  public void testSetRootOrganizationTemplate_missingOrg() throws IOException {
    try {
      service.setRootOrganizationTemplate("missing-org");
      fail("Did not throw exception");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Cannot find organization with ID missing-org."));
    }
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() throws IOException {
    service.setRootOrganizationEmptyTemplate();
    assertTrue(migrationUtils.isMigrated());
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyScheduled() throws IOException {
    migrationUtils.setSourceOrganizationId("bla");
    try {
      service.setRootOrganizationEmptyTemplate();
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
    assertFalse(migrationUtils.isMigrated());
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyMigrated() throws IOException {
    migrationUtils.setMigrated();
    try {
      service.setRootOrganizationEmptyTemplate();
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
  }
}
