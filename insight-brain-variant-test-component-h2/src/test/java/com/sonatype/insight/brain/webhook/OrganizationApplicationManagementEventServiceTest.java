/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.webhook;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;
import com.sonatype.insight.brain.webhook.dto.OrganizationSummary;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ComponentH2Test
public class OrganizationApplicationManagementEventServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private CurrentUser currentUser;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Captor
  private ArgumentCaptor<OrganizationApplicationManagementEvent> eventArgumentCaptor;

  @Test
  public void testPostEvent_OrganizationApplicationSummary() throws Exception {
    final TestEventHandler<OrganizationApplicationManagementEvent> handler =
        new TestEventHandler<>(new CountDownLatch(1), OrganizationApplicationManagementEvent.class);
    asyncEventBus.register(handler);

    final Organization organization = tempEntity.newOrganization();
    final OrganizationSummary organizationSummary = new OrganizationSummary(organization);
    final List<OrganizationSummary> organizationSummaries = Collections.singletonList(organizationSummary);

    final Application application = tempEntity.newApplication(organization.getId());
    final ApplicationSummary applicationSummary = new ApplicationSummary(application);
    final List<ApplicationSummary> applicationSummaries = Collections.singletonList(applicationSummary);

    organizationApplicationManagementEventService.postEvent();

    assertThat(handler.getLatch().await(1, TimeUnit.SECONDS)).isTrue();
    final OrganizationApplicationManagementEvent event = handler.getEvent();
    assertThat(event.initiator).isEqualTo(USERNAME);
    assertThat(event.organizations).usingRecursiveComparison()
        .isEqualTo(organizationSummaries);
    assertThat(event.applications).usingRecursiveComparison()
        .isEqualTo(applicationSummaries);
    assertThat(event.repositoryManagers).isEmpty();
    assertThat(event.repositories).isEmpty();

    asyncEventBus.unregister(handler);
  }

  @Test
  public void testPostEventForLifecycle_PopulatesOrganizationsAndApplicationsOnly() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization organization = tempEntity.newOrganization();
    final Application application = tempEntity.newApplication(organization.getId());
    tempEntity.newRepository();

    orgAppSummaryEventService.postEventForLifecycle();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final OrganizationApplicationManagementEvent event = eventArgumentCaptor.getValue();
    assertThat(event.organizations)
        .extracting(summary -> summary.id)
        .contains(organization.getId());
    assertThat(event.applications)
        .extracting(summary -> summary.id)
        .contains(application.getId());
    assertThat(event.repositoryManagers).isEmpty();
    assertThat(event.repositories).isEmpty();
  }

  @Test
  public void testPostEventForFirewall_PopulatesOrganizationsRepositoriesAndRepositoryManagersOnly() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization organization = tempEntity.newOrganization();
    tempEntity.newApplication(organization.getId());
    final RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager("instance-id", "repo-manager", "Nexus Repository", "3.0");
    final Repository repository = tempEntity.newRepository(repositoryManager, "docker-proxy");

    orgAppSummaryEventService.postEventForFirewall();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final OrganizationApplicationManagementEvent event = eventArgumentCaptor.getValue();
    assertThat(event.organizations)
        .extracting(summary -> summary.id)
        .contains(organization.getId());
    assertThat(event.applications).isEmpty();
    assertThat(event.repositoryManagers)
        .extracting(summary -> summary.id)
        .contains(repositoryManager.getId());
    assertThat(event.repositories)
        .extracting(summary -> summary.id)
        .contains(repository.getId());
  }

  @Test
  public void testPostEvent_DeprecatedCompatibilityPathDelegatesToLifecycle() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization organization = tempEntity.newOrganization();
    final Application application = tempEntity.newApplication(organization.getId());
    tempEntity.newRepository();

    orgAppSummaryEventService.postEvent();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final OrganizationApplicationManagementEvent event = eventArgumentCaptor.getValue();
    assertThat(event.organizations)
        .extracting(summary -> summary.id)
        .contains(organization.getId());
    assertThat(event.applications)
        .extracting(summary -> summary.id)
        .contains(application.getId());
    assertThat(event.repositoryManagers).isEmpty();
    assertThat(event.repositories).isEmpty();
  }

  @Test
  public void testPostEvent_CreateOrganizationSummariesWhenValid() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization organization1 = tempEntity.newOrganization("a");
    final Organization organization2 = tempEntity.newOrganization("b");

    orgAppSummaryEventService.postEvent();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final List<OrganizationSummary> organizationSummaries = eventArgumentCaptor.getValue().organizations;
    assertThat(organizationSummaries)
        .isNotEmpty();

    organizationSummaries.sort(Comparator.comparing(summary -> summary.name));

    final OrganizationSummary orgSummary1 = organizationSummaries.get(0);
    assertThat(orgSummary1.id)
        .isEqualTo(organization1.getId());
    assertThat(orgSummary1.name)
        .isEqualTo(organization1.getName());
    assertThat(orgSummary1.parentOrgId)
        .isEqualTo(organization1.getParentOrganizationId());

    final OrganizationSummary orgSummary2 = organizationSummaries.get(1);
    assertThat(orgSummary2.id)
        .isEqualTo(organization2.getId());
    assertThat(orgSummary2.name)
        .isEqualTo(organization2.getName());
    assertThat(orgSummary2.parentOrgId)
        .isEqualTo(organization2.getParentOrganizationId());
  }

  @Test
  public void testPostEvent_OrganizationSummaries_DoesNotContainRootOrg() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization organization1 = tempEntity.newOrganization();
    final Organization organization2 = tempEntity.newOrganization();

    orgAppSummaryEventService.postEvent();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final List<OrganizationSummary> organizationSummaries = eventArgumentCaptor.getValue().organizations;
    assertThat(organizationSummaries)
        .hasSize(2)
        .extracting("id")
        .contains(organization1.getId())
        .contains(organization2.getId())
        .doesNotContain(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testPostEvent_OrganizationSummaries_SortedByNameAlphabetically() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization organization1 = tempEntity.newOrganization("DEf");
    final Organization organization2 = tempEntity.newOrganization("abC");
    final Organization organization3 = tempEntity.newOrganization("123");

    orgAppSummaryEventService.postEvent();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final List<OrganizationSummary> organizationSummaries = eventArgumentCaptor.getValue().organizations;
    assertThat(organizationSummaries)
        .extracting("name")
        .containsExactly(organization3.getName(), organization2.getName(), organization1.getName());
  }

  @Test
  public void testPostEvent_CreateApplicationSummariesWhenValid() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization org = tempEntity.newOrganization();
    final Application application1 = tempEntity.newApplication("a", "a", org.getId());
    final Application application2 = tempEntity.newApplication("b", "b", org.getId());

    orgAppSummaryEventService.postEvent();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final List<ApplicationSummary> applicationSummaries = eventArgumentCaptor.getValue().applications;
    assertThat(applicationSummaries)
        .isNotEmpty();

    applicationSummaries.sort(Comparator.comparing(summary -> summary.name));

    final ApplicationSummary appSummary1 = applicationSummaries.get(0);
    assertThat(appSummary1.id)
        .isEqualTo(application1.getId());
    assertThat(appSummary1.publicId)
        .isEqualTo(application1.getPublicId());
    assertThat(appSummary1.name)
        .isEqualTo(application1.getName());
    assertThat(appSummary1.organizationId)
        .isEqualTo(application1.getOrganizationId());

    final ApplicationSummary appSummary2 = applicationSummaries.get(1);
    assertThat(appSummary2.id)
        .isEqualTo(application2.getId());
    assertThat(appSummary2.publicId)
        .isEqualTo(application2.getPublicId());
    assertThat(appSummary2.name)
        .isEqualTo(application2.getName());
    assertThat(appSummary2.organizationId)
        .isEqualTo(application2.getOrganizationId());
  }

  @Test
  public void testPostEvent_ApplicationSummaries_SortedByNameAlphabetically() {
    final AsyncEventBus eventBusSpy = spy(asyncEventBus);
    final OrganizationApplicationManagementEventService orgAppSummaryEventService =
        new OrganizationApplicationManagementEventService(
            eventBusSpy, organizationDAO, applicationDAO, repositoryManagerDAO, repositoryDAO, currentUser);

    final Organization organization = tempEntity.newOrganization();
    final Application application1 = tempEntity.newApplication("ZZz", "public-id1", organization.getId());
    final Application application2 = tempEntity.newApplication("78d", "public-id2", organization.getId());
    final Application application3 = tempEntity.newApplication("p2W", "public-id3", organization.getId());

    orgAppSummaryEventService.postEvent();
    verify(eventBusSpy).post(eventArgumentCaptor.capture());

    final List<ApplicationSummary> applicationSummaries = eventArgumentCaptor.getValue().applications;
    assertThat(applicationSummaries)
        .extracting("name")
        .containsExactly(application2.getName(), application3.getName(), application1.getName());
  }
}
