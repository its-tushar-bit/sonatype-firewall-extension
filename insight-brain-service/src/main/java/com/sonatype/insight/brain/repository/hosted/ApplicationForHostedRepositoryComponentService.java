/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets or creates a synthetic Application for each hosted repository component,
 * following the same pattern as container images (ApplicationForContainerImageFirewallService).
 * This enables the full IQ report page experience for hosted repository components.
 */
@Named
@Singleton
public class ApplicationForHostedRepositoryComponentService
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationForHostedRepositoryComponentService.class);

  static final String ORGANIZATION_NAME_HOSTED_COMPONENTS = "Hosted Repository Components";

  // Must match ApplicationDAO.MAX_PUBLIC_ID_LENGTH and the application.public_id VARCHAR(200) column
  private static final int MAX_PUBLIC_ID_LENGTH = 200;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public ApplicationForHostedRepositoryComponentService(
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final RepositoryDAO repositoryDAO)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryDAO = repositoryDAO;
  }

  /**
   * Gets or creates a synthetic Application for a hosted repository component.
   * The applicationPublicId is derived from repositoryPublicId + sanitized pathname.
   */
  public Application getOrCreateApplication(
      final String repositoryId,
      final String pathname)
  {
    try (TransactionContext tx = applicationDAO.createTransactionContext()) {
      tx.begin();
      Repository repository = repositoryDAO.getById(repositoryId);
      if (repository == null) {
        return null;
      }

      String applicationPublicId = generatePublicId(repository.getPublicId(), pathname);

      Application existing = applicationDAO.getByPublicId(tx, applicationPublicId);
      if (existing != null) {
        tx.commit();
        return existing;
      }

      // Determine parent organization — root org cannot own apps (sidebar NPE)
      String organizationId = resolveOrganizationId(tx, repository);
      if (organizationId == null) {
        log.warn("No valid organization found for repositoryId={}, skipping synthetic app creation", repositoryId);
        tx.commit();
        return null;
      }

      Application application = new Application();
      application.setPublicId(applicationPublicId);
      application.setName(applicationPublicId);
      application.setOrganizationId(organizationId);
      try {
        applicationDAO.insert(tx, application);
        tx.commit();
      }
      catch (Exception e) {
        tx.rollback();
        // Another thread inserted concurrently — return the winner's application
        Application concurrent = applicationDAO.getByPublicId(applicationPublicId);
        if (concurrent != null) {
          return concurrent;
        }
        throw e;
      }

      log.debug("Created synthetic application for hosted component: {}", applicationPublicId);
      return application;
    }
  }

  private String resolveOrganizationId(final TransactionContext tx, final Repository repository) {
    if (repository.getRelatedOrganizationId() != null) {
      Organization org = organizationDAO.getById(tx, repository.getRelatedOrganizationId());
      if (org != null) {
        return org.getId();
      }
    }
    // No org linked — create a dedicated org for this repository under "Hosted Repository Components"
    Organization parentOrg = getOrCreateHostedComponentsOrg(tx);
    Organization repoOrg = new Organization();
    repoOrg.setName(repository.getPublicId() != null ? repository.getPublicId() : repository.getId());
    repoOrg.setParentOrganizationId(parentOrg.getId());
    repoOrg.setRelatedRepositoryId(repository.getId());
    organizationDAO.insert(tx, repoOrg);

    repository.setRelatedOrganizationId(repoOrg.getId());
    repositoryDAO.update(tx, repository);

    log.info("Created dedicated org for hosted repository {}: {}", repository.getId(), repoOrg.getId());
    return repoOrg.getId();
  }

  private Organization getOrCreateHostedComponentsOrg(final TransactionContext tx) {
    Organization existing = organizationDAO.getByName(tx, ORGANIZATION_NAME_HOSTED_COMPONENTS);
    if (existing != null) {
      return existing;
    }
    Organization org = new Organization();
    org.setName(ORGANIZATION_NAME_HOSTED_COMPONENTS);
    org.setParentOrganizationId(Organization.ROOT_ORGANIZATION_ID);
    organizationDAO.insert(tx, org);
    log.info("Created top-level org for hosted repository components: {}", org.getId());
    return org;
  }

  public static String generatePublicId(final String repositoryPublicId, final String pathname) {
    String sanitized = pathname != null
        ? pathname.replaceAll("[^a-zA-Z0-9\\-._]", "_")
        : "unknown";
    String repoPrefix = repositoryPublicId != null
        ? repositoryPublicId.replaceAll("[^a-zA-Z0-9\\-._]", "_")
        : "repo";
    String publicId = repoPrefix + "_" + sanitized;
    if (publicId.length() <= MAX_PUBLIC_ID_LENGTH) {
      return publicId;
    }
    // Preserve a fixed repo prefix slice for uniqueness; fill remainder with pathname tail.
    // Split budget: half to repo prefix (min 10), half to pathname.
    int prefixBudget = Math.max(10, MAX_PUBLIC_ID_LENGTH / 2);
    String truncatedPrefix = StringUtils.left(repoPrefix, prefixBudget);
    int remaining = MAX_PUBLIC_ID_LENGTH - truncatedPrefix.length() - 1; // -1 for separator
    return truncatedPrefix + "_" + StringUtils.right(sanitized, remaining);
  }
}
