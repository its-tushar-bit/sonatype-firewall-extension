/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.InvalidApplicationException;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ApplicationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationService applicationService;

  private Organization org;
  private Application app1;
  private Application app2;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication(org.getId());
    app2 = tempEntity.newApplicationWithParent("app2");
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_NullParams() {
    List<Application> apps = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null /* organisationIds */, null /* applicationIds */,
            null /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_EmptyParams() {
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.<String> emptySet() /* organisationIds */, Collections.<String> emptySet() /* applicationIds */,
        Collections.<String> emptySet() /* tagIds */);
    assertThat(apps, hasSize(2));
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_AppId() {
    List<Application> apps = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Collections.singleton(app1.getId()), null /* tagIds */);
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app1.getId()));
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgId() {
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(app1.getParentOwnerId()), null, null /* tagIds */);
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app1.getId()));
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_TagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        null /* organisationIds */, null /* applicationIds */, Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(1));
    assertThat(apps.get(0).getId(), is(app2.getId()));
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_AppIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null,
        Collections.singleton(app1.getId()), Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(0));
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(app1.getParentOwnerId()), null, Collections.singleton(tag.getId()));
    assertThat(apps, hasSize(0));
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgWithNoChildrenAndNullTagIds() {
    Organization org = tempEntity.newOrganization();
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(org.getId()), null, null);
    assertThat(apps, hasSize(0));
  }

  @Test
  public void testAddApplication_LicenseWithoutApplicationLimit() throws Exception {
    productLicenseManager.setApplicationLimit(null);
    clmLicenseManager.installLicense(null);
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
  }

  @Test
  public void testAddApplication_RootOrgIsNoValidParent() {
    Application app = new Application("appPublicId", "appName", Organization.ROOT_ORGANIZATION_ID);
    try {
      applicationService.addApplication(app);
      fail("Expected exception");
    }
    catch (InvalidApplicationException e) {
      assertThat(e.getMessage(), is("Applications cannot have the root organization as parent."));
    }
  }

  @Test
  public void testAddApplication_addsUserToOwnerRole() {
    Organization org = tempEntity.newOrganization();
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    List<MembershipMapping> mappings = new MembershipMappingDAO().getByContextIdAndRoleId(app.getId(),
        Role.OWNER_ROLE_ID);
    assertThat(mappings.size(), is(1));
    assertThat(mappings.get(0).getMemberName(), is(USERNAME));
    assertThat(mappings.get(0).getMemberType(), is(MemberType.USER));
  }

  @Test
  public void testAddUpdateAndDeleteApplicationPostEvents() throws Exception {
    TestEventHandler<OwnerEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization org = tempEntity.newOrganization();
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    final String applicationId = app.getId();

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(CREATED));
    assertThat(handler.getEvent().ownerId, is(applicationId));
    assertThat(handler.getEvent().owner.getId(), is(applicationId));

    handler.setLatch(new CountDownLatch(1));

    app.setName("new appId");
    applicationService.updateApplication(app);

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(UPDATED));
    assertThat(handler.getEvent().ownerId, is(applicationId));
    assertThat(handler.getEvent().owner.getId(), is(applicationId));

    handler.setLatch(new CountDownLatch(1));

    applicationService.deleteApplicationByPublicId(app.getPublicId());

    assertThat(handler.getLatch().await(5, SECONDS), is(true));
    assertThat(handler.getEvent().action, is(DELETED));
    assertThat(handler.getEvent().ownerId, is(applicationId));
    assertThat(handler.getEvent().owner.getId(), is(applicationId));

    eventBus.unregister(handler);
  }

  @Test
  public void testAddApplication_NoOrganization() throws Exception {
    String applicationPublicId = "testAddApplication_NoOrganization";
    String applicationName = "testAddApplication-NoOrganization";

    Application application = new Application();
    application.setName(applicationName);
    application.setPublicId(applicationPublicId);

    try {
      applicationService.addApplication(application);
      fail("Expected exception");
    }
    catch (InvalidApplicationException expected) {
      assertEquals("Application must have a parent organization.", expected.getMessage());
    }
  }

  @Test
  public void testGetApplicationIdsByOrganizationIds() {
    String sameOrgAppId = tempEntity.newApplication(org.getId()).getId();

    Set<String> applicationIds = applicationService
        .getApplicationIdsByOrganizationIds(Collections.singleton(org.getId()));
    assertThat(applicationIds, containsInAnyOrder(app1.getId(), sameOrgAppId));
  }

  @Test
  public void testGetApplicationIdsByOrganizationIds_null() {
    tempEntity.newApplication(org.getId()).getId();

    Set<String> applicationIds = applicationService.getApplicationIdsByOrganizationIds(null);
    assertThat(applicationIds, notNullValue());
  }

  @Test
  public void testDeleteApplicationByPublicId_ApplicationWithData() throws Exception {
    final String applicationId = app1.getId();

    final InsightWork insightWork = lookup(InsightWork.class);
    Files.createDirectories(insightWork.getScanDir(applicationId).toPath());
    Files.createDirectories(insightWork.getAuditDir(applicationId).toPath());
    Files.createDirectories(insightWork.getReportDir(applicationId).toPath());

    applicationService.deleteApplicationByPublicId(app1.getPublicId());
    assertNull(new ApplicationDAO().getById(applicationId));

    assertFalse(insightWork.getScanDir(applicationId).exists());
    assertFalse(insightWork.getAuditDir(applicationId).exists());
    assertFalse(insightWork.getReportDir(applicationId).exists());
  }

  @Test
  public void testDeleteApplicationByPublicId_NonExistingApplication() throws Exception {
    String applicationPublicId = "NoSuchAppPublicId";
    try {
      applicationService.deleteApplicationByPublicId(applicationPublicId);
      fail("Expected exception");
    }
    catch (NotFoundException expected) {
      assertEquals("Could not find an application with public ID " + applicationPublicId + ".", expected.getMessage());
    }
  }

  @Test
  public void testUpdateApplication_NoOrganization() throws Exception {
    app1.setOrganizationId(null);

    try {
      applicationService.updateApplication(app1);
      fail("Expected exception");
    }
    catch (InvalidApplicationException expected) {
      assertEquals("Cannot change the parent organization of an application.", expected.getMessage());
    }
  }

  @Test
  public void testUpdateApplication_ChangeOrganization() throws Exception {
    app1.setOrganizationId("newOrganizationId");

    try {
      applicationService.updateApplication(app1);
      fail("Expected exception");
    }
    catch (InvalidApplicationException expected) {
      assertEquals("Cannot change the parent organization of an application.", expected.getMessage());
    }
  }
}
