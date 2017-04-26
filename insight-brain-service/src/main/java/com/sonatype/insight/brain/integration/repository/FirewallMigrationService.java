/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.28
 */
@Named
public class FirewallMigrationService
{
  static final String PROTOCOL_V1 = "v1";

  private static final Logger log = LoggerFactory.getLogger(FirewallMigrationService.class);

  private static final RepositoryDAO repositoryDAO = new RepositoryDAO();

  private final VersionService versionService;

  private final CLMLicenseManager licenseManager;

  @Inject
  public FirewallMigrationService(final VersionService versionService, final CLMLicenseManager licenseManager) {
    this.versionService = versionService;
    this.licenseManager = licenseManager;
  }

  private void checkLicenseFeature() {
    if (!licenseManager.hasRepositoryFirewall()) {
      throw new InvalidLicenseException();
    }
  }

  /**
   * Check whether the migration is supported for the specified protocol version.
   */
  void verifyMigrationSupport(String protocolVersion) {
    checkLicenseFeature();

    if (!PROTOCOL_V1.equals(protocolVersion)) {
      throw new BadRequestException(
          "IQ Server " + versionService.getVersion() + " does not support migration protocol " + protocolVersion +
              ", please update your IQ Server.");
    }
  }

  void migrateRepositoryHistory(String repositoryManagerInstanceId,
                                String repositoryPublicId,
                                String sourceRepositoryManagerInstanceId,
                                String sourceRepositoryPublicId,
                                String lastMigratedPathname)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);

    Repository sourceRepository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(sourceRepositoryManagerInstanceId,
            sourceRepositoryPublicId);

    log.debug("Migrating history of repository {}:{} ({}) to repository {}:{} ({}).", sourceRepositoryManagerInstanceId,
        sourceRepositoryPublicId, sourceRepository.getId(), repositoryManagerInstanceId, repositoryPublicId,
        repository.getId());

    migrateRepositoryHistory(repository, sourceRepository, lastMigratedPathname);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void migrateRepositoryHistory(@AuthzContext(Key.REPOSITORY) Repository repository,
                                Repository sourceRepository,
                                String lastMigratedPathname)
  {
  }

  MigrationState getRepositoryMigrationState(String repositoryManagerInstanceId, String repositoryPublicId) {
    checkLicenseFeature();

    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);

    log.debug("Getting repository migration history state for repository {}:{} ({}).", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());

    return getRepositoryMigrationState(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  MigrationState getRepositoryMigrationState(@AuthzContext(Key.REPOSITORY) Repository repository)
  {
    return MigrationState.COMPLETED;
  }
}
