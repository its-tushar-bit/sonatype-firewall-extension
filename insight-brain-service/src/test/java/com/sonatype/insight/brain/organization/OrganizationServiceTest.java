/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

public class OrganizationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private OrganizationService organizationService;

  @Inject
  private InsightWork work;

  /**
   * There's a similar protection at the DAO layer but given the order of operations, the service layer needs to prevent
   * deletion of the root org as well before it starts carrying out any other destructive actions like cleaning the
   * filesystem (e.g. icons).
   */
  @Test
  public void testDeleteOrganization_RootOrgCannotBeDeleted() throws Exception {
    File iconDir = new File(work.getOrganizationIconDir(), Organization.ROOT_ORGANIZATION_ID);
    assertThat(iconDir.mkdirs(), is(true));
    File iconFile = new File(iconDir, "icon.png");
    assertThat(iconFile.createNewFile(), is(true));

    Organization childOrg = tempEntity.newOrganization();

    try {
      organizationService.deleteOrganization(Organization.ROOT_ORGANIZATION_ID);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(new OrganizationDAO().getById(childOrg.getId()), is(notNullValue()));
      assertThat(iconFile.isFile(), is(true));
      assertThat(iconDir.isDirectory(), is(true));
      assertThat(e.getMessage(), is("The root organization cannot be deleted."));
    }
  }

  @Test
  public void testGetAll() throws Exception {
    RootOrganizationConfigMigrationService service = Mockito.mock(RootOrganizationConfigMigrationService.class);
    Mockito.when(service.isMigrated()).thenReturn(false);

    List<Organization> orgs = new OrganizationService(null, null, null, new OrganizationDAO(), service).getAll();
    assertThat(orgs, hasSize(0));

    Mockito.when(service.isMigrated()).thenReturn(true);
    OrganizationService organizationService = new OrganizationService(null, null, null, new OrganizationDAO(), service);

    orgs = organizationService.getAll();
    assertThat(orgs, hasSize(1));
  }
}
