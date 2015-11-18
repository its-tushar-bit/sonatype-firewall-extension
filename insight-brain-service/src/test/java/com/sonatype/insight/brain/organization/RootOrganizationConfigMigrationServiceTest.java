/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.FileUtils;
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

  @Before
  public void setup() throws IOException {
    insightConfig.setSonatypeWork(tempDir.newFolder().getAbsolutePath());
    InsightWork insightWork = new InsightWork(insightConfig);

    service = new RootOrganizationConfigMigrationService(new OrganizationDAO(), insightWork);
  }

  @Test
  public void testIsMigrated() throws IOException {
    FileUtils.touch(getMigratedFile());
    assertTrue(service.isMigrated());
  }

  @Test
  public void testIsMigrated_noFile() throws IOException {
    assertFalse(service.isMigrated());
  }

  @Test
  public void testIsMigrationScheduled() throws IOException {
    FileUtils.touch(getMigrationFile());
    assertTrue(service.isMigrationScheduled());
  }

  @Test
  public void testSetRootOrganizationTemplate() throws IOException {
    Organization org = tempEntity.newOrganization();
    service.setRootOrganizationTemplate(org.getId());

    assertTrue(service.isMigrationScheduled());
    assertThat(FileUtils.readFileToString(getMigrationFile()), is(org.getId()));
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyScheduled() throws IOException {
    Organization org = tempEntity.newOrganization();
    FileUtils.touch(getMigrationFile());
    try {
      service.setRootOrganizationTemplate(org.getId());
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
    assertThat(FileUtils.readFileToString(getMigrationFile()), not(is(org.getId())));
  }

  @Test
  public void testSetRootOrganizationTemplate_previouslyMigrated() throws IOException {
    Organization org = tempEntity.newOrganization();
    FileUtils.touch(getMigratedFile());
    try {
      service.setRootOrganizationTemplate(org.getId());
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
    assertFalse(getMigrationFile().exists());
  }

  public void testSetRootOrganizationTemplate_missingOrg() throws IOException {
    try {
      service.setRootOrganizationTemplate("missing-org");
      fail("Did not throw exception");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Cannot find organization with ID missing-org"));
    }
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() throws IOException {
    service.setRootOrganizationEmptyTemplate();
    assertTrue(getMigratedFile().exists());
    assertTrue(service.isMigrated());
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyScheduled() throws IOException {
    FileUtils.touch(getMigrationFile());
    try {
      service.setRootOrganizationEmptyTemplate();
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
    assertFalse(getMigratedFile().exists());
    assertFalse(service.isMigrated());
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate_alreadyMigrated() throws IOException {
    FileUtils.touch(getMigratedFile());
    try {
      service.setRootOrganizationEmptyTemplate();
      fail("Did not throw exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Migration has previously been scheduled or performed."));
    }
  }

  private File getMigratedFile() {
    return new File(insightConfig.getSonatypeWork(), RootOrganizationConfigMigrationService.MIGRATED_FILE);
  }

  private File getMigrationFile() {
    return new File(insightConfig.getSonatypeWork(), RootOrganizationConfigMigrationService.MIGRATE_FILE);
  }
}
