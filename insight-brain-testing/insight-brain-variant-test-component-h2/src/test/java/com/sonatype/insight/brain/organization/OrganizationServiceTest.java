/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.webhook.EventAction.CREATED;
import static com.sonatype.insight.brain.webhook.EventAction.DELETED;
import static com.sonatype.insight.brain.webhook.EventAction.UPDATED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.service.githubapp.GitHubAppDeletionService;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogDTOAssert;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEvent;
import com.sonatype.insight.brain.webhook.TestEventHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

@ComponentH2Test
public class OrganizationServiceTest
    extends AbstractComponentH2Test
{
  private static final String TEST_GITHUB_APP_SLUG = "test-app";

  private static final String TEST_CLIENT_ID = "test-client-id";

  private static final String TEST_CLIENT_SECRET = "test-client-secret";

  private int appIdCounter = 10000;

  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  @Inject
  private OrganizationService organizationService;

  @Inject
  private InsightWork work;

  @Inject
  private AsyncEventBus eventBus;

  @Inject
  private PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  @Inject
  private ApplicationCleaner applicationCleaner;

  @Inject
  private com.sonatype.insight.brain.common.io.FileCleaner fileCleaner;

  @Inject
  private com.sonatype.insight.brain.webhook.ManagementEventService managementEventService;

  @Inject
  private com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  @Mock
  private CurrentUser currentUser;

  @Mock
  private WaivedComponentUpgradeScheduler waivedComponentUpgradeScheduler;

  @Mock
  private OwnerMaintenanceTelemetryCreator mockOwnerMaintenanceTelemetryCreator;

  @Mock
  private GitHubAppDeletionService mockGitHubAppDeletionService;

  private ListAppender<ILoggingEvent> loggingEventListAppender;

  @BeforeEach
  public void before() {
    Logger organizationServiceLogger = (Logger) LoggerFactory.getLogger(OrganizationService.class);
    loggingEventListAppender = new ListAppender<>();
    loggingEventListAppender.start();
    organizationServiceLogger.addAppender(loggingEventListAppender);
    applyBeanFieldOverride(PolicyViolationLoggerFactory.class, "currentUser", currentUser);
  }

  /**
   * There's a similar protection at the DAO layer but given the order of operations, the service layer needs to prevent
   * deletion of the root org as well before it starts carrying out any other destructive actions like cleaning the
   * filesystem (e.g. icons).
   */
  @Test
  public void testDeleteOrganization_RootOrgCannotBeDeleted() throws Exception {
    File iconDir = new File(work.getOrganizationIconDir(), Organization.ROOT_ORGANIZATION_ID);
    assertThat(iconDir.mkdirs()).isTrue();
    File iconFile = new File(iconDir, "icon.png");
    assertThat(iconFile.createNewFile()).isTrue();

    Organization childOrg = tempEntity.newOrganization();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> organizationService.deleteOrganization(Organization.ROOT_ORGANIZATION_ID))
        .withMessageContaining("root organization cannot be deleted");
    assertThat(organizationDAO.getById(childOrg.getId())).isNotNull();
    assertThat(iconFile).isFile();
    assertThat(iconDir).isDirectory();
    verifyNoInteractions(mockOwnerMaintenanceTelemetryCreator);
  }

  @Test
  public void testDeleteOrganization_AutomaticApplicationsParentOrgCannotBeDeleted_OrgWithNoChildrenToDelete() {
    Organization organization = tempEntity.newOrganization("organization");
    String organizationId = organization.getId();
    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> organizationService.deleteOrganization(organizationId))
        .withMessageContaining(
            "Cannot delete the parent organization for automatic application creation: " + organization.getName()
                + ".");
    assertThat(organizationDAO.getById(organizationId)).isNotNull();
    verifyNoInteractions(mockOwnerMaintenanceTelemetryCreator);
  }

  @Test
  public void testDeleteOrganization_AutomaticApplicationsParentOrgCannotBeDeleted_OrgWithChildrenToDelete() {
    List<Organization> testList = tempEntity.newRelatedOrganizationsAsList(1, 7, 0);
    Organization organization = testList.get(4);
    String organizationId = organization.getId();
    automaticApplicationsConfigurationDAO.setEnabled(true);
    automaticApplicationsConfigurationDAO.setOrganizationId(organizationId);

    assertThatExceptionOfType(PartialDeletionException.class)
        .isThrownBy(() -> organizationService.deleteOrganization(organizationId))
        .withMessageContaining("The delete operation was partially successful." +
            " Some sub-Orgs and applications of this Org were deleted," +
            " while some failed with error(s) below." +
            "\n" + "Cannot delete the parent organization for automatic application creation: " +
            organization.getName() + ".");

    for (Organization currentOrg : testList.subList(0, 4)) {
      assertThat(organizationDAO.getById(currentOrg.getId())).isNull();
    }

    for (Organization currentOrg : testList.subList(4, 7)) {
      assertThat(organizationDAO.getById(currentOrg.getId())).isNotNull();
    }

    verify(mockOwnerMaintenanceTelemetryCreator, times(4))
        .sendOwnerMaintenanceTelemetry(any(Organization.class), eq(OwnerMaintenanceTelemetry.TYPE_DELETE));
  }

  @Test
  public void testGetAll() {
    OrganizationService organizationService =
        new OrganizationService(null, null, null, organizationDAO, applicationDAO, null, policyViolationLoggerFactory,
            null, mockOwnerMaintenanceTelemetryCreator, mockGitHubAppDeletionService);

    tempEntity.newOrganizationWithRepositoryManager("org-with-repo-man");

    List<Organization> orgs = organizationService.getAll();
    assertThat(orgs).hasSize(2);
  }

  @Test
  public void testGetAllWithoutRelatedRepositories() {
    OrganizationService organizationService =
        new OrganizationService(
            null, null, null, organizationDAO, applicationDAO, null, policyViolationLoggerFactory,
            null, mockOwnerMaintenanceTelemetryCreator, mockGitHubAppDeletionService);

    tempEntity.newOrganizationWithRepositoryManager("org-with-repo-man");

    List<Organization> orgs = organizationService.getAllWithoutRelatedRepositories();
    assertThat(orgs).hasSize(1);
  }

  @Test
  public void testGetOrganization() {
    OrganizationService organizationService =
        new OrganizationService(null, null, null, organizationDAO, applicationDAO, null, policyViolationLoggerFactory,
            null, mockOwnerMaintenanceTelemetryCreator, mockGitHubAppDeletionService);

    Organization testOrg = tempEntity.newOrganization();

    Organization resultOrg = organizationService.getOrganization(testOrg.getId());
    assertThat(resultOrg).isNotNull();
    assertThat(resultOrg.getName()).isEqualTo(testOrg.getName());
    assertThat(resultOrg.getId()).isEqualTo(testOrg.getId());
    verifyNoInteractions(mockOwnerMaintenanceTelemetryCreator);
  }

  @Test
  public void testGetOrganization_idDoesNotExist() {
    OrganizationService organizationService =
        new OrganizationService(null, null, null, organizationDAO, applicationDAO, null, policyViolationLoggerFactory,
            null, mockOwnerMaintenanceTelemetryCreator, mockGitHubAppDeletionService);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> organizationService.getOrganization("NOT_REAL_ID"))
        .withMessageContaining("Organization with ID NOT_REAL_ID does not exist.");
    verifyNoInteractions(mockOwnerMaintenanceTelemetryCreator);
  }

  @Test
  public void testAddUpdateAndDeleteOrganizationPostEvents() throws Exception {
    TestEventHandler<WebhookEvent> handler = new TestEventHandler<>(new CountDownLatch(2), WebhookEvent.class);
    eventBus.register(handler);

    Organization org = new Organization("testOrg");
    Organization created = organizationService.addOrganization(org);
    final String organizationId = created.getId();

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    OwnerEvent ownerEvent = (OwnerEvent) handler.getAllEvents()
        .stream()
        .filter(event -> event instanceof OwnerEvent)
        .findFirst()
        .get();
    OrganizationApplicationManagementEvent orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents()
            .stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent)
            .findFirst()
            .get();

    assertThat(ownerEvent.action).isEqualTo(CREATED);
    assertThat(ownerEvent.ownerId).isEqualTo(organizationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(organizationId);
    assertThat(orgAppSummaryEvent.organizations).hasSize(1);
    assertThat(orgAppSummaryEvent.applications).isEmpty();
    verify(mockOwnerMaintenanceTelemetryCreator)
        .sendOwnerMaintenanceTelemetry(eq(org), eq(OwnerMaintenanceTelemetry.TYPE_ADD));

    handler.setLatch(new CountDownLatch(2));

    created.setName("new appId");
    created = organizationService.updateOrganization(created);

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    ownerEvent = (OwnerEvent) handler.getAllEvents()
        .stream()
        .filter(event -> event instanceof OwnerEvent)
        .findFirst()
        .get();
    orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents()
            .stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent)
            .findFirst()
            .get();

    assertThat(ownerEvent.action).isEqualTo(UPDATED);
    assertThat(ownerEvent.ownerId).isEqualTo(organizationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(organizationId);
    assertThat(orgAppSummaryEvent.organizations).hasSize(1);
    assertThat(orgAppSummaryEvent.applications).isEmpty();
    verify(mockOwnerMaintenanceTelemetryCreator)
        .sendOwnerMaintenanceTelemetry(eq(created), eq(OwnerMaintenanceTelemetry.TYPE_UPDATE));

    handler.setLatch(new CountDownLatch(2));

    organizationService.deleteOrganization(created.getId());

    assertThat(handler.getLatch().await(5, SECONDS)).isTrue();

    ownerEvent = (OwnerEvent) handler.getAllEvents()
        .stream()
        .filter(event -> event instanceof OwnerEvent)
        .findFirst()
        .get();
    orgAppSummaryEvent =
        (OrganizationApplicationManagementEvent) handler.getAllEvents()
            .stream()
            .filter(event -> event instanceof OrganizationApplicationManagementEvent)
            .findFirst()
            .get();

    assertThat(ownerEvent.action).isEqualTo(DELETED);
    assertThat(ownerEvent.ownerId).isEqualTo(organizationId);
    assertThat(ownerEvent.owner.getId()).isEqualTo(organizationId);
    assertThat(orgAppSummaryEvent.organizations).isEmpty();
    assertThat(orgAppSummaryEvent.applications).isEmpty();
    verify(mockOwnerMaintenanceTelemetryCreator)
        .sendOwnerMaintenanceTelemetry(any(Organization.class), eq(OwnerMaintenanceTelemetry.TYPE_DELETE));

    eventBus.unregister(handler);
  }

  @Test
  public void testUpdateOrganization_CannotChangeParentOrganization() throws Exception {
    Organization org = tempEntity.newOrganization();
    Organization otherOrg = tempEntity.newOrganization();

    org.setParentOrganizationId(otherOrg.getId());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      organizationService.updateOrganization(org);
    }).withMessage("Cannot change the parent organization. Use move organization instead.");
  }

  @Test
  public void testDeleteOrganization_NLevel_CascadeToChildOrganizations() throws Exception {
    List<Organization> testList = tempEntity.newRelatedOrganizationsAsList(1, 7, 0);
    List<Organization> deletedOrgs = testList.subList(0, 6);
    TestEventHandler<OwnerEvent> handler =
        new TestEventHandler<>(new CountDownLatch(deletedOrgs.size()), OwnerEvent.class);
    Collection<OwnerEvent> deleteOrgEvents;
    eventBus.register(handler);
    organizationService.deleteOrganization(testList.get(5).getId());

    assertThat(handler.getLatch().await(10, SECONDS)).isTrue();
    deleteOrgEvents = handler.getAllEvents();
    assertThat(deleteOrgEvents).hasSameSizeAs(deletedOrgs);

    for (Organization currentOrg : deletedOrgs) {
      Optional<OwnerEvent> currentEventOptional = deleteOrgEvents.stream()
          .filter(event -> event.ownerId.equals(currentOrg.getId()))
          .findFirst();

      assertThat(currentEventOptional).isPresent();
      OwnerEvent currentEvent = currentEventOptional.get();
      assertThat(organizationDAO.getById(currentOrg.getId())).isNull();
      assertThat(currentEvent.action).isEqualTo(DELETED);
      assertThat(currentEvent.ownerId).isEqualTo(currentOrg.getId());
      assertThat(currentEvent.owner.getId()).isEqualTo(currentOrg.getId());
    }
    verify(mockOwnerMaintenanceTelemetryCreator, times(6))
        .sendOwnerMaintenanceTelemetry(any(Organization.class), eq(OwnerMaintenanceTelemetry.TYPE_DELETE));

    eventBus.unregister(handler);
  }

  @Test
  public void testDeleteOrganization_PolicyViolationLogger_LogsClearEvent() throws Exception {
    when(currentUser.getUsernameOrSystem()).thenReturn(USERNAME);
    Organization organization = tempEntity.newOrganization();

    Date before = new Date();
    organizationService.deleteOrganization(organization.getId());
    Date after = new Date();

    List<PolicyViolationLogDTO> policyViolationLogDTOs = PolicyViolationLogDTOAssert
        .assertPolicyViolationLogDTOs(logOutput, 1);
    PolicyViolationLogDTOAssert
        .assertOrganizationPolicyViolationData(policyViolationLogDTOs.get(0), PolicyViolationLogEvent.CLEAR,
            organization, before, after, USERNAME);
    verify(mockOwnerMaintenanceTelemetryCreator)
        .sendOwnerMaintenanceTelemetry(any(Organization.class), eq(OwnerMaintenanceTelemetry.TYPE_DELETE));
  }

  @Test
  public void testDeleteOrganization_DeactivatesGitHubApps() throws Exception {
    OrganizationService customService = new OrganizationService(
        work,
        applicationCleaner,
        fileCleaner,
        organizationDAO,
        applicationDAO,
        managementEventService,
        policyViolationLoggerFactory,
        organizationApplicationManagementEventService,
        mockOwnerMaintenanceTelemetryCreator,
        mockGitHubAppDeletionService);

    Organization organization = tempEntity.newOrganization();
    GitHubApp gitHubApp1 = createGitHubApp(organization.getId(), 100L, true);
    GitHubApp gitHubApp2 = createGitHubApp(organization.getId(), 200L, false);

    assertThat(gitHubAppDAO.getById(gitHubApp1.getId())).isNotNull();
    assertThat(gitHubAppDAO.getById(gitHubApp2.getId())).isNotNull();

    customService.deleteOrganization(organization.getId());

    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(organization.getId()));
    assertThat(organizationDAO.getById(organization.getId())).isNull();
  }

  @Test
  public void testDeleteOrganization_NoGitHubApps_StillSucceeds() throws Exception {
    OrganizationService customService = new OrganizationService(
        work,
        applicationCleaner,
        fileCleaner,
        organizationDAO,
        applicationDAO,
        managementEventService,
        policyViolationLoggerFactory,
        organizationApplicationManagementEventService,
        mockOwnerMaintenanceTelemetryCreator,
        mockGitHubAppDeletionService);

    Organization organization = tempEntity.newOrganization();
    assertThat(gitHubAppDAO.getByOwnerId(organization.getId())).isEmpty();

    customService.deleteOrganization(organization.getId());

    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(organization.getId()));
    assertThat(organizationDAO.getById(organization.getId())).isNull();
  }

  @Test
  public void testGetAllParentOrgsNoAuthz_GetSameParentOrgsOfApps() {
    Organization parentOrganization1 = tempEntity.newOrganization("Parent Org 1");
    Organization parentOrganization2 = tempEntity.newOrganization("Parent Org 2", parentOrganization1);
    Organization parentOrganization3 = tempEntity.newOrganization("Parent Org 3", parentOrganization2);
    Organization organization = tempEntity.newOrganization("Org", parentOrganization3);
    Application application1 = tempEntity.newApplication("MyApp1", organization.getId());
    Application application2 = tempEntity.newApplication("MyApp2", organization.getId());

    Map<String, Organization> parentOrgs = organizationService
        .getAllParentOrgsNoAuthz(Arrays.asList(application1, application2));

    assertThat(parentOrgs).hasSize(5)
        .containsKey(organization.getId())
        .containsKey(parentOrganization1.getId())
        .containsKey(parentOrganization2.getId())
        .containsKey(parentOrganization3.getId())
        .containsKey(ROOT_ORGANIZATION_ID);
    verifyNoInteractions(mockOwnerMaintenanceTelemetryCreator);
  }

  @Test
  public void testGetAllParentOrgsNoAuthz_GetDifferentParentOrgsOfApps() {
    Organization parentOrganization1 = tempEntity.newOrganization("Parent Org 1");
    Organization parentOrganization2 = tempEntity.newOrganization("Parent Org 2", parentOrganization1);
    Organization parentOrganization3 = tempEntity.newOrganization("Parent Org 3", parentOrganization2);
    Organization organization1 = tempEntity.newOrganization("Org1", parentOrganization3);
    Application application1 = tempEntity.newApplication("MyApp1", organization1.getId());

    Organization parentOrganization4 = tempEntity.newOrganization("Parent Org 4", parentOrganization3);
    Organization parentOrganization5 = tempEntity.newOrganization("Parent Org 5", parentOrganization4);
    Organization organization2 = tempEntity.newOrganization("Org2", parentOrganization5);
    Application application2 = tempEntity.newApplication("MyApp2", organization2.getId());

    Map<String, Organization> parentOrgs = organizationService
        .getAllParentOrgsNoAuthz(Arrays.asList(application1, application2));

    assertThat(parentOrgs).hasSize(8)
        .containsKey(organization1.getId())
        .containsKey(organization2.getId())
        .containsKey(parentOrganization1.getId())
        .containsKey(parentOrganization2.getId())
        .containsKey(parentOrganization3.getId())
        .containsKey(parentOrganization4.getId())
        .containsKey(parentOrganization5.getId())
        .containsKey(ROOT_ORGANIZATION_ID);
    verifyNoInteractions(mockOwnerMaintenanceTelemetryCreator);
  }

  @Test
  public void testDeleteOrganization_WithMultipleApps_DeactivatesAllGitHubApps() throws Exception {
    applyBeanFieldOverride(ApplicationCleaner.class, "gitHubAppDeletionService", mockGitHubAppDeletionService);
    applyBeanFieldOverride(OrganizationService.class, "gitHubAppDeletionService", mockGitHubAppDeletionService);

    Organization organization = tempEntity.newOrganization();

    Application app1 = tempEntity.newApplication("App1", organization.getId());
    createGitHubApp(organization.getId(), 100L, true);
    createGitHubApp(app1.getId(), 200L, true);

    Application app2 = tempEntity.newApplication("App2", organization.getId());
    createGitHubApp(app2.getId(), 300L, true);

    organizationService.deleteOrganization(organization.getId());

    assertThat(organizationDAO.getById(organization.getId())).isNull();
    assertThat(applicationDAO.getById(app1.getId())).isNull();
    assertThat(applicationDAO.getById(app2.getId())).isNull();

    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(organization.getId()));
    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(app1.getId()));
    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(app2.getId()));
  }

  @Test
  public void testDeleteOrganization_DeepHierarchy_DeactivatesAllGitHubApps() throws Exception {
    applyBeanFieldOverride(ApplicationCleaner.class, "gitHubAppDeletionService", mockGitHubAppDeletionService);
    applyBeanFieldOverride(OrganizationService.class, "gitHubAppDeletionService", mockGitHubAppDeletionService);

    Organization level1 = tempEntity.newOrganization("Level1");
    createGitHubApp(level1.getId(), 100L, true);

    Organization level2 = tempEntity.newOrganization("Level2", level1);
    Application level2App = tempEntity.newApplication("Level2App", level2.getId());
    createGitHubApp(level2App.getId(), 200L, true);

    Organization level3 = tempEntity.newOrganization("Level3", level2);
    createGitHubApp(level3.getId(), 300L, true);

    Application level3App = tempEntity.newApplication("Level3App", level3.getId());
    createGitHubApp(level3App.getId(), 400L, true);

    organizationService.deleteOrganization(level1.getId());

    assertThat(organizationDAO.getById(level1.getId())).isNull();
    assertThat(organizationDAO.getById(level2.getId())).isNull();
    assertThat(organizationDAO.getById(level3.getId())).isNull();
    assertThat(applicationDAO.getById(level2App.getId())).isNull();
    assertThat(applicationDAO.getById(level3App.getId())).isNull();

    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(level1.getId()));
    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(level2App.getId()));
    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(level3.getId()));
    verify(mockGitHubAppDeletionService).deactivateGitHubApps(any(), eq(level3App.getId()));
  }

  private GitHubApp createGitHubApp(String ownerId, long installationId, boolean isActive) {
    appIdCounter++;

    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(java.util.UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(appIdCounter);
    gitHubApp.setSlug(TEST_GITHUB_APP_SLUG);
    gitHubApp.setGithubOrganizationName("myOrg");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setClientId(TEST_CLIENT_ID);
    gitHubApp.setClientSecret(TEST_CLIENT_SECRET);
    gitHubApp.setPrivateKey("test-private-key");
    gitHubApp.setInstallationId(installationId);
    gitHubApp.setActive(isActive);
    return tempEntity.newGitHubApp(gitHubApp);
  }
}
