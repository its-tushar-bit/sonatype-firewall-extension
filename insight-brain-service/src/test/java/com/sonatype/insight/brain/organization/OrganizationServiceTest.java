/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
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

  @Inject
  private AsyncEventBus eventBus;

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
    RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils = Mockito
        .mock(RootOrganizationConfigMigrationUtils.class);
    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);

    List<Organization> orgs = new OrganizationService(null, null, null, new OrganizationDAO(),
        rootOrganizationConfigMigrationUtils, null).getAll();
    assertThat(orgs, hasSize(0));

    Mockito.when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
    OrganizationService organizationService = new OrganizationService(null, null, null, new OrganizationDAO(),
        rootOrganizationConfigMigrationUtils, null);

    orgs = organizationService.getAll();
    assertThat(orgs, hasSize(1));
  }


  @Test
  public void testAddUpdateAndDeleteOrganizationPostEvents() throws Exception {
    TestEventHandler<OwnerEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization org = new Organization("testOrg");
    Organization created = organizationService.addOrganization(org);
    final String organizationId = created.getId();

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(CREATED));
    assertThat(handler.getEvent().ownerId, is(organizationId));
    assertThat(handler.getEvent().owner.getId(), is(organizationId));

    handler.setLatch(new CountDownLatch(1));

    created.setName("new appId");
    created = organizationService.updateOrganization(created);

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(UPDATED));
    assertThat(handler.getEvent().ownerId, is(organizationId));
    assertThat(handler.getEvent().owner.getId(), is(organizationId));

    handler.setLatch(new CountDownLatch(1));

    organizationService.deleteOrganization(created.getId());

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(DELETED));
    assertThat(handler.getEvent().ownerId, is(organizationId));
    assertThat(handler.getEvent().owner.getId(), is(organizationId));

    eventBus.unregister(handler);
  }
}
