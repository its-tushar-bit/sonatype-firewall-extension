/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.webhook;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;
import com.sonatype.insight.brain.webhook.dto.OrganizationSummary;
import com.sonatype.insight.brain.webhook.dto.RepositoryManagerSummary;
import com.sonatype.insight.brain.webhook.dto.RepositorySummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class OrganizationApplicationManagementEventService
{
  private static final Logger log = LoggerFactory.getLogger(OrganizationApplicationManagementEventService.class);

  private final AsyncEventBus asyncEventBus;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final CurrentUser currentUser;

  @Inject
  public OrganizationApplicationManagementEventService(
      final AsyncEventBus asyncEventBus,
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryDAO repositoryDAO,
      final CurrentUser currentUser)
  {
    this.asyncEventBus = asyncEventBus;
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.currentUser = currentUser;
  }

  /**
   * Post event for Lifecycle context operations (application/organization changes).
   * Only includes Lifecycle-specific data (organizations, applications).
   * Organizations are always included as they are shared between both products.
   */
  public void postEventForLifecycle() {
    try {
      // Always include organizations (shared between Lifecycle and Firewall)
      final List<OrganizationSummary> organizationSummaries = createOrganizationSummaries();

      // Include Lifecycle-specific data
      final List<ApplicationSummary> applicationSummaries = createApplicationSummaries();

      // Empty Firewall-specific data (this is a Lifecycle event)
      final List<RepositoryManagerSummary> repositoryManagerSummaries = Collections.emptyList();
      final List<RepositorySummary> repositorySummaries = Collections.emptyList();

      final OrganizationApplicationManagementEvent orgAppManagementEvent =
          new OrganizationApplicationManagementEvent(
              organizationSummaries,
              applicationSummaries,
              repositoryManagerSummaries,
              repositorySummaries);
      orgAppManagementEvent.initiator = currentUser.getUsernameOrSystem();

      asyncEventBus.post(orgAppManagementEvent);
    }
    catch (final RuntimeException e) {
      log.error("Lifecycle webhook not posted due to exception.", e);
    }
  }

  /**
   * Post event for Firewall context operations (repository/repository manager changes).
   * Only includes Firewall-specific data (repositories, repository managers).
   * Organizations are always included as they are shared between both products.
   */
  public void postEventForFirewall() {
    try {
      // Always include organizations (shared between Lifecycle and Firewall)
      final List<OrganizationSummary> organizationSummaries = createOrganizationSummaries();

      // Empty Lifecycle-specific data (this is a Firewall event)
      final List<ApplicationSummary> applicationSummaries = Collections.emptyList();

      // Include Firewall-specific data
      final List<RepositoryManagerSummary> repositoryManagerSummaries = createRepositoryManagerSummaries();
      final List<RepositorySummary> repositorySummaries = createRepositorySummaries();

      final OrganizationApplicationManagementEvent orgAppManagementEvent =
          new OrganizationApplicationManagementEvent(
              organizationSummaries,
              applicationSummaries,
              repositoryManagerSummaries,
              repositorySummaries);
      orgAppManagementEvent.initiator = currentUser.getUsernameOrSystem();

      asyncEventBus.post(orgAppManagementEvent);
    }
    catch (final RuntimeException e) {
      log.error("Firewall webhook not posted due to exception.", e);
    }
  }

  /**
   * @deprecated Use {@link #postEventForLifecycle()} or {@link #postEventForFirewall()} instead.
   *             This method is kept for backward compatibility with existing code during migration.
   *             It posts a Lifecycle event by default.
   */
  @Deprecated
  public void postEvent() {
    postEventForLifecycle();
  }

  private List<OrganizationSummary> createOrganizationSummaries() {
    // getAllWithoutRelatedRepositories excludes Firewall-created orgs (those with related_repository_manager_id,
    // related_repository_id, or related_repository_container_id set) and also excludes ROOT via the name filter below.
    return organizationDAO.getAllWithoutRelatedRepositories()
        // We only want to send customer organizations, so remove the root org built-in if it exists
        .stream()
        .filter(org -> !org.getId().equals(Organization.ROOT_ORGANIZATION_ID))
        .sorted(Comparator.comparing(orgSummary -> orgSummary.getName().toLowerCase(Locale.ROOT)))
        .map(OrganizationSummary::new)
        .collect(Collectors.toList());
  }

  private List<ApplicationSummary> createApplicationSummaries() {
    return applicationDAO.getAllWithoutRelatedRepositories()
        .stream()
        .sorted(Comparator.comparing(application -> application.getName().toLowerCase(Locale.ROOT)))
        .map(ApplicationSummary::new)
        .collect(Collectors.toList());
  }

  private List<RepositoryManagerSummary> createRepositoryManagerSummaries() {
    return repositoryManagerDAO.getAll()
        .stream()
        .sorted(Comparator.comparing(rm -> rm.getName() != null ? rm.getName().toLowerCase(Locale.ROOT) : ""))
        .map(RepositoryManagerSummary::new)
        .collect(Collectors.toList());
  }

  private List<RepositorySummary> createRepositorySummaries() {
    return repositoryDAO.getAll()
        .stream()
        .sorted(
            Comparator.comparing(repo -> repo.getPublicId() != null ? repo.getPublicId().toLowerCase(Locale.ROOT) : ""))
        .map(RepositorySummary::new)
        .collect(Collectors.toList());
  }
}
