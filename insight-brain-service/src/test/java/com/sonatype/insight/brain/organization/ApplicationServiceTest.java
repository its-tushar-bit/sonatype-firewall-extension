/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

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
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApplicationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private ApplicationService applicationService;

  private Organization org;

  private Application app1;

  private Application app2;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private TestProductLicense testProductLicense;

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
    assertThat(apps).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_EmptyParams() {
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.emptySet() /* organisationIds */, Collections.emptySet() /* applicationIds */,
        Collections.emptySet() /* tagIds */);
    assertThat(apps).hasSize(2);
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_AppId() {
    List<Application> apps = applicationService
        .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, Collections.singleton(app1.getId()), null /* tagIds */);
    assertThat(apps).extracting(Application::getId).containsExactlyInAnyOrder(app1.getId());
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgId() {
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(app1.getParentOwnerId()), null, null /* tagIds */);
    assertThat(apps).extracting(Application::getId).containsExactlyInAnyOrder(app1.getId());
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_TagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        null /* organisationIds */, null /* applicationIds */, Collections.singleton(tag.getId()));
    assertThat(apps).extracting(Application::getId).containsExactlyInAnyOrder(app2.getId());
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_AppIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(null,
        Collections.singleton(app1.getId()), Collections.singleton(tag.getId()));
    assertThat(apps).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgIdAndTagId() {
    Tag tag = tempEntity.newTag(org.getParentOwnerId());
    tempEntity.newApplicationTag(app2.getId(), tag.getId());
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(app1.getParentOwnerId()), null, Collections.singleton(tag.getId()));
    assertThat(apps).isEmpty();
  }

  @Test
  public void testGetApplicationsByIdsAndOrganizationsAndTagIds_OrgWithNoChildrenAndNullTagIds() {
    Organization org = tempEntity.newOrganization();
    List<Application> apps = applicationService.getApplicationsByIdsAndOrganizationIdsAndTagIds(
        Collections.singleton(org.getId()), null, null);
    assertThat(apps).isEmpty();
  }

  @Test
  public void testAddApplication_LicenseWithoutApplicationLimit() throws Exception {
    testProductLicense.setMaxApplications(null);
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
  }

  @Test
  public void testAddApplication_RootOrgIsNoValidParent() {
    Application app = new Application("appPublicId", "appName", Organization.ROOT_ORGANIZATION_ID);
    assertThatExceptionOfType(InvalidApplicationException.class).isThrownBy(() -> {
      applicationService.addApplication(app);
    }).withMessageContaining("cannot have the root organization as parent");
  }

  @Test
  public void testAddApplication_addsUserToOwnerRole() {
    Organization org = tempEntity.newOrganization();
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    List<MembershipMapping> mappings = new MembershipMappingDAO().getByContextIdAndRoleId(app.getId(),
        Role.OWNER_ROLE_ID);
    assertThat(mappings).hasSize(1);
    assertThat(mappings.get(0).getMemberName()).isEqualTo(USERNAME);
    assertThat(mappings.get(0).getMemberType()).isEqualTo(MemberType.USER);
  }

  @Test
  public void testAddUpdateAndDeleteApplicationPostEvents() throws Exception {
    TestEventHandler<OwnerEvent> handler = new TestEventHandler<>(new CountDownLatch(1));
    eventBus.register(handler);

    Organization org = tempEntity.newOrganization();
    Application app = new Application("appPublicId", "appName", org.getId());
    app = applicationService.addApplication(app);
    final String applicationId = app.getId();

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(CREATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(applicationId);
    assertThat(handler.getEvent().owner.getId()).isEqualTo(applicationId);

    handler.setLatch(new CountDownLatch(1));

    app.setName("new appId");
    applicationService.updateApplication(app);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(UPDATED);
    assertThat(handler.getEvent().ownerId).isEqualTo(applicationId);
    assertThat(handler.getEvent().owner.getId()).isEqualTo(applicationId);

    handler.setLatch(new CountDownLatch(1));

    applicationService.deleteApplicationByPublicId(app.getPublicId());

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();
    assertThat(handler.getEvent().action).isEqualTo(DELETED);
    assertThat(handler.getEvent().ownerId).isEqualTo(applicationId);
    assertThat(handler.getEvent().owner.getId()).isEqualTo(applicationId);

    eventBus.unregister(handler);
  }

  @Test
  public void testAddApplication_NoOrganization() throws Exception {
    String applicationPublicId = "testAddApplication_NoOrganization";
    String applicationName = "testAddApplication-NoOrganization";

    Application application = new Application();
    application.setName(applicationName);
    application.setPublicId(applicationPublicId);

    assertThatExceptionOfType(InvalidApplicationException.class).isThrownBy(() -> {
      applicationService.addApplication(application);
    }).withMessageContaining("must have a parent organization");
  }

  @Test
  public void testGetApplicationIdsByOrganizationIds() {
    String sameOrgAppId = tempEntity.newApplication(org.getId()).getId();

    Set<String> applicationIds = applicationService
        .getApplicationIdsByOrganizationIds(Collections.singleton(org.getId()));
    assertThat(applicationIds).containsExactlyInAnyOrder(app1.getId(), sameOrgAppId);
  }

  @Test
  public void testGetApplicationIdsByOrganizationIds_null() {
    tempEntity.newApplication(org.getId()).getId();

    Set<String> applicationIds = applicationService.getApplicationIdsByOrganizationIds(null);
    assertThat(applicationIds).isNotNull();
  }

  @Test
  public void testDeleteApplicationByPublicId_ApplicationWithData() throws Exception {
    final String applicationId = app1.getId();

    final InsightWork insightWork = lookup(InsightWork.class);
    Files.createDirectories(insightWork.getScanDir(applicationId).toPath());
    Files.createDirectories(insightWork.getAuditDir(applicationId).toPath());
    Files.createDirectories(insightWork.getReportDir(applicationId).toPath());

    applicationService.deleteApplicationByPublicId(app1.getPublicId());
    assertThat(new ApplicationDAO().getById(applicationId)).isNull();

    assertThat(insightWork.getScanDir(applicationId)).doesNotExist();
    assertThat(insightWork.getAuditDir(applicationId)).doesNotExist();
    assertThat(insightWork.getReportDir(applicationId)).doesNotExist();
  }

  @Test
  public void testDeleteApplicationByPublicId_NonExistingApplication() throws Exception {
    String applicationPublicId = "NoSuchAppPublicId";
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      applicationService.deleteApplicationByPublicId(applicationPublicId);
    }).withMessage("Could not find an application with public ID " + applicationPublicId + ".");
  }

  @Test
  public void testUpdateApplication_NoOrganization() throws Exception {
    app1.setOrganizationId(null);

    assertThatExceptionOfType(InvalidApplicationException.class).isThrownBy(() -> {
      applicationService.updateApplication(app1);
    }).withMessageContaining("not change the parent organization of an application");
  }

  @Test
  public void testUpdateApplication_ChangeOrganization() throws Exception {
    app1.setOrganizationId("newOrganizationId");

    assertThatExceptionOfType(InvalidApplicationException.class).isThrownBy(() -> {
      applicationService.updateApplication(app1);
    }).withMessageContaining("not change the parent organization of an application");
  }

  @Test
  public void testDeleteApplicationByPublicId_PolicyViolationLogger_LogsClearEvent() throws Exception {
    Date before = new Date();
    applicationService.deleteApplicationByPublicId(app1.getPublicId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(logOutput, 1);
    PolicyViolationLogDTOAssert
        .assertApplicationPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR, org, app1,
            before, after);
  }
}
