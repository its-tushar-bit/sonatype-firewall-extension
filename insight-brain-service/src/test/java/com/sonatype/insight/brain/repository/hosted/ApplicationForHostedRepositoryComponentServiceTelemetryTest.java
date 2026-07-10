/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetry;
import com.sonatype.insight.brain.telemetry.OwnerMaintenanceTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying REAL_OWNER_IDS telemetry emission from
 * {@link ApplicationForHostedRepositoryComponentService}.
 * <p>
 * CLM-42079: Without these emissions, Databricks' {@code application_id_name} lookup table
 * has no entries for synthetic hosted apps — producing the 99.8% mapping gap Dariush reported.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationForHostedRepositoryComponentServiceTelemetryTest
{
  @Mock
  private ApplicationDAO applicationDAO;

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private OwnerMaintenanceTelemetryCreator ownerMaintenanceTelemetryCreator;

  @Mock
  private TransactionContext tx;

  private ApplicationForHostedRepositoryComponentService service;

  @Before
  public void setUp() {
    service = new ApplicationForHostedRepositoryComponentService(
        applicationDAO, organizationDAO, repositoryDAO, ownerMaintenanceTelemetryCreator);
    when(applicationDAO.createTransactionContext()).thenReturn(tx);
    // organizationDAO.insert() normally generates and sets an id on the passed instance —
    // Mockito default returns void without side effects, so mimic the real behaviour here so
    // downstream code that reads org.getId() sees a non-null value.
    doAnswer(inv -> {
      Organization o = inv.getArgument(1, Organization.class);
      if (o.getId() == null) {
        o.setId(UUID.randomUUID().toString().replace("-", ""));
      }
      return null;
    }).when(organizationDAO).insert(any(TransactionContext.class), any(Organization.class));
    // Same for applicationDAO.insert — REAL_OWNER_IDS telemetry reads application.getId(),
    // so tests observing that value need the mock to populate it.
    doAnswer(inv -> {
      Application app = inv.getArgument(1, Application.class);
      if (app.getId() == null) {
        app.setId(UUID.randomUUID().toString().replace("-", ""));
      }
      return null;
    }).when(applicationDAO).insert(any(TransactionContext.class), any(Application.class));
  }

  @Test
  public void getOrCreateApplication_emitsRealOwnerIdsForNewApplication() {
    // Given: a repository with an existing related organization
    Repository repository = new Repository();
    repository.setId("repo-1");
    repository.setPublicId("maven-releases");
    repository.setRelatedOrganizationId("org-1");

    when(repositoryDAO.getById("repo-1")).thenReturn(repository);
    when(organizationDAO.getById(tx, "org-1")).thenReturn(newOrg("org-1"));
    when(applicationDAO.getByPublicId(eq(tx), any())).thenReturn(null);

    // When
    Application result = service.getOrCreateApplication("repo-1", "com/example/lib-1.0.jar");

    // Then: application creation must emit REAL_OWNER_IDS with TYPE_ADD
    assertThat(result).isNotNull();
    verify(ownerMaintenanceTelemetryCreator).sendOwnerMaintenanceTelemetry(
        eq(result), eq(OwnerMaintenanceTelemetry.TYPE_ADD));
  }

  @Test
  public void getOrCreateApplication_doesNotEmitForExistingApplication() {
    // Given: a repository whose synthetic app already exists
    Repository repository = new Repository();
    repository.setId("repo-1");
    repository.setPublicId("maven-releases");
    repository.setRelatedOrganizationId("org-1");

    Application existingApp = new Application();
    existingApp.setId("app-existing");
    existingApp.setPublicId("maven-releases_com_example_lib-1.0.jar");

    when(repositoryDAO.getById("repo-1")).thenReturn(repository);
    when(applicationDAO.getByPublicId(eq(tx), any())).thenReturn(existingApp);

    // When
    Application result = service.getOrCreateApplication("repo-1", "com/example/lib-1.0.jar");

    // Then: existing app returned — no telemetry emitted (would duplicate lookup entries)
    assertThat(result).isSameAs(existingApp);
    verify(ownerMaintenanceTelemetryCreator, never()).sendOwnerMaintenanceTelemetry(any(), any());
  }

  @Test
  public void getOrCreateApplication_doesNotEmitWhenRepositoryMissing() {
    // Given: repository lookup returns null (repo was deleted)
    when(repositoryDAO.getById("repo-missing")).thenReturn(null);

    // When
    Application result = service.getOrCreateApplication("repo-missing", "any/path.jar");

    // Then: no app created, no telemetry emitted
    assertThat(result).isNull();
    verify(ownerMaintenanceTelemetryCreator, never()).sendOwnerMaintenanceTelemetry(any(), any());
  }

  @Test
  public void getOrCreateApplication_emitsRealOwnerIdsForNewPerRepoOrgAndParentOrg() {
    // Given: a repository with NO related organization yet
    Repository repository = new Repository();
    repository.setId("repo-2");
    repository.setPublicId("npm-hosted");
    repository.setRelatedOrganizationId(null);

    when(repositoryDAO.getById("repo-2")).thenReturn(repository);
    when(applicationDAO.getByPublicId(eq(tx), any())).thenReturn(null);
    // Parent "Hosted Repository Components" org does not exist yet → will be created
    when(organizationDAO.getByName(tx,
        ApplicationForHostedRepositoryComponentService.ORGANIZATION_NAME_HOSTED_COMPONENTS))
            .thenReturn(null);

    // When
    Application result = service.getOrCreateApplication("repo-2", "pkg/lib.tgz");

    // Then: telemetry must fire for BOTH the parent org, per-repo org, and the synthetic app
    // 3 emissions total: parent org + per-repo org + application
    assertThat(result).isNotNull();
    verify(ownerMaintenanceTelemetryCreator, times(3))
        .sendOwnerMaintenanceTelemetry(any(), eq(OwnerMaintenanceTelemetry.TYPE_ADD));
  }

  @Test
  public void getOrCreateApplication_reusesExistingParentOrg() {
    // Given: parent "Hosted Repository Components" org already exists
    Repository repository = new Repository();
    repository.setId("repo-3");
    repository.setPublicId("pypi-hosted");
    repository.setRelatedOrganizationId(null);

    Organization existingParentOrg = new Organization();
    existingParentOrg.setId("parent-org-id");
    existingParentOrg.setName(ApplicationForHostedRepositoryComponentService.ORGANIZATION_NAME_HOSTED_COMPONENTS);

    when(repositoryDAO.getById("repo-3")).thenReturn(repository);
    when(applicationDAO.getByPublicId(eq(tx), any())).thenReturn(null);
    when(organizationDAO.getByName(tx,
        ApplicationForHostedRepositoryComponentService.ORGANIZATION_NAME_HOSTED_COMPONENTS))
            .thenReturn(existingParentOrg);

    // When
    Application result = service.getOrCreateApplication("repo-3", "pkg/lib.tar.gz");

    // Then: 2 emissions expected — per-repo org + synthetic app (parent org already existed, no emission)
    assertThat(result).isNotNull();
    verify(ownerMaintenanceTelemetryCreator, times(2))
        .sendOwnerMaintenanceTelemetry(any(), eq(OwnerMaintenanceTelemetry.TYPE_ADD));
  }

  @Test
  public void getOrCreateApplication_returnsCreatedApplicationEvenWhenTelemetryEmissionThrows() {
    // Given: telemetry creator will throw on emission (simulates HDS/queue outage).
    Repository repository = new Repository();
    repository.setId("repo-resilience");
    repository.setPublicId("maven-releases");
    repository.setRelatedOrganizationId("org-1");

    when(repositoryDAO.getById("repo-resilience")).thenReturn(repository);
    when(organizationDAO.getById(tx, "org-1")).thenReturn(newOrg("org-1"));
    when(applicationDAO.getByPublicId(eq(tx), any())).thenReturn(null);

    org.mockito.Mockito.doThrow(new RuntimeException("simulated telemetry outage"))
        .when(ownerMaintenanceTelemetryCreator)
        .sendOwnerMaintenanceTelemetry(any(), any());

    // When
    Application result = service.getOrCreateApplication("repo-resilience", "com/example/lib-1.0.jar");

    // Then: telemetry failure must NOT fail the synthetic-app creation path — the caller
    // (hosted scan queue consumer) treats a null Application as a warn-and-fallback and we
    // don't want to trigger that just because Databricks was unreachable.
    assertThat(result).as("telemetry outage must not fail synthetic app creation").isNotNull();
    assertThat(result.getPublicId()).isEqualTo("maven-releases_com_example_lib-1.0.jar");
    verify(ownerMaintenanceTelemetryCreator).sendOwnerMaintenanceTelemetry(
        eq(result), eq(OwnerMaintenanceTelemetry.TYPE_ADD));
  }

  private Organization newOrg(final String id) {
    Organization org = new Organization();
    org.setId(id);
    return org;
  }
}
