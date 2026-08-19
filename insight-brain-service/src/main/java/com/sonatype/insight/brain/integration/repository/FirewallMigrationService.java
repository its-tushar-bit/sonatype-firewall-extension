/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryMigrationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.repository.migration.MigrationState.RUNNING;

/**
 * @since 1.32
 */
@Named
@Singleton
public class FirewallMigrationService
{
  static final String PROTOCOL_V1 = "v1";

  private static final Logger log = LoggerFactory.getLogger(FirewallMigrationService.class);

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryMigrationDAO repositoryMigrationDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ThreadPoolExecutor executor =
      new TenantThreadPoolExecutor(1, 1, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat("FirewallMigration-%d").build(), new AbortPolicy(),
          "firewall_migration", "FirewallMigrationService");

  private final VersionService versionService;

  private final ProductLicense productLicense;

  @Inject
  public FirewallMigrationService(
      final VersionService versionService,
      final ProductLicense productLicense,
      final RepositoryManagerDAO repositoryManagerDAO,
      final RepositoryDAO repositoryDAO,
      final RepositoryMigrationDAO repositoryMigrationDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      final LicenseOverrideDAO licenseOverrideDAO,
      final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final ShutdownHandler shutdownHandler)
  {
    this.versionService = versionService;
    this.productLicense = productLicense;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryMigrationDAO = repositoryMigrationDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.policyWaiverDAO = policyWaiverDAO;

    executor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(executor);
  }

  // Visible for testing
  ThreadPoolExecutor getExecutor() {
    return executor;
  }

  private void checkLicenseFeature() {
    productLicense.validateFeature(LicensedFeature.FIREWALL);
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

  void migrateRepositoryHistory(
      String sourceRepositoryManagerInstanceId,
      String sourceRepositoryPublicId,
      String targetRepositoryManagerInstanceId,
      String targetRepositoryPublicId)
  {
    AuditData.get()
        .setData("sourceRepositoryManagerInstanceId", sourceRepositoryManagerInstanceId)
        .setData("sourceRepositoryPublicId", sourceRepositoryPublicId)
        .setData("repositoryManagerInstanceId", targetRepositoryManagerInstanceId)
        .setRepositoryPublicId(targetRepositoryPublicId);

    checkLicenseFeature();

    Repository sourceRepository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        sourceRepositoryManagerInstanceId, sourceRepositoryPublicId);

    Repository targetRepository = createOrUpdateTargetRepository(sourceRepository,
        targetRepositoryManagerInstanceId, targetRepositoryPublicId);

    AuditData.get()
        .setData("sourceRepositoryId", sourceRepository.getId())
        .setRepository(targetRepository)
        .setData("quarantine", sourceRepository.isQuarantineEnabled() ? "enabled" : "disabled");

    log.debug("Migrating history of repository {}:{} ({}) to repository {}:{} ({}).", sourceRepositoryManagerInstanceId,
        sourceRepositoryPublicId, sourceRepository.getId(), targetRepositoryManagerInstanceId, targetRepositoryPublicId,
        targetRepository.getId());

    migrateRepositoryHistory(sourceRepository, targetRepository);
  }

  /**
   * This method is synchronized to avoid concurrent migration requests trying to insert the same repository manager in
   * the database.
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  synchronized Repository createOrUpdateTargetRepository(
      @AuthzContext(Key.REPOSITORY) Repository sourceRepository,
      String targetRepositoryManagerInstanceId,
      String targetRepositoryPublicId)
  {
    Repository targetRepository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicId(targetRepositoryManagerInstanceId, targetRepositoryPublicId);
    if (targetRepository == null) {
      RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(targetRepositoryManagerInstanceId);
      if (repositoryManager == null) {
        repositoryManager = new RepositoryManager(targetRepositoryManagerInstanceId);
        repositoryManagerDAO.insert(repositoryManager);
      }
      targetRepository = new Repository(repositoryManager.getId(), targetRepositoryPublicId);
    }
    targetRepository.setFormat(sourceRepository.getFormat());
    targetRepository.setAuditEnabled(sourceRepository.isAuditEnabled());
    targetRepository.setQuarantineEnabled(sourceRepository.isQuarantineEnabled());
    if (targetRepository.getId() == null) {
      repositoryDAO.insert(targetRepository);
    }
    else {
      repositoryDAO.update(targetRepository);
    }
    return targetRepository;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void migrateRepositoryHistory(
      Repository sourceRepository,
      @AuthzContext(Key.REPOSITORY) Repository targetRepository)
  {
    RepositoryMigration repositoryMigration = repositoryMigrationDAO.getByRepositoryId(targetRepository.getId());
    if (repositoryMigration != null) {
      if (RUNNING == repositoryMigration.getState()) {
        log.warn("Ignoring migration request for repository {}:{} ({}) because the migration is already running.",
            targetRepository.getRepositoryManagerId(), targetRepository.getPublicId(), targetRepository.getId());
        return;
      }
      else {
        log.info("Retrying migration attempt of repository {}:{} ({}), previous attempt's state was {}.",
            targetRepository.getRepositoryManagerId(), targetRepository.getPublicId(), targetRepository.getId(),
            repositoryMigration.getState());
        repositoryMigrationDAO.delete(repositoryMigration);
      }
    }

    repositoryMigration = new RepositoryMigration();
    repositoryMigration.setRepositoryId(targetRepository.getId());
    repositoryMigration.setState(RUNNING);
    if (repositoryMigrationDAO.tryInsert(repositoryMigration)) {
      executor.submit(
          new FirewallMigrationWorker(sourceRepository, targetRepository, repositoryMigration, repositoryDAO,
              proxyRepositoryComponentDAO, proxyRepositoryPolicyViolationDAO, licenseOverrideDAO,
              securityVulnerabilityOverrideDAO, policyWaiverDAO, repositoryMigrationDAO));
      log.info("Scheduled the history migration from {}:{} ({}) to {}:{} ({}).",
          sourceRepository.getRepositoryManagerId(), sourceRepository.getPublicId(), sourceRepository.getId(),
          targetRepository.getRepositoryManagerId(), targetRepository.getPublicId(), targetRepository.getId());
    }
    else {
      throw new IllegalStateException("Unable to add MigrationDetails for repository " + targetRepository.getId());
    }
  }

  MigrationDetails getRepositoryMigrationState(
      String targetRepositoryManagerInstanceId,
      String targetRepositoryPublicId)
  {
    checkLicenseFeature();

    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicIdNotNull(
        targetRepositoryManagerInstanceId, targetRepositoryPublicId);

    log.debug("Getting repository migration history state for repository {}:{} ({}).",
        targetRepositoryManagerInstanceId, targetRepositoryPublicId, repository.getId());

    return getRepositoryMigrationState(repository);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  MigrationDetails getRepositoryMigrationState(@AuthzContext(Key.REPOSITORY) Repository targetRepository) {
    RepositoryMigration repositoryMigration = repositoryMigrationDAO.getByRepositoryId(targetRepository.getId());
    if (repositoryMigration == null) {
      RepositoryManager targetRepositoryManager = repositoryManagerDAO
          .getById(targetRepository.getRepositoryManagerId());
      log.error("Migration was not started for repository {}:{} ({}).", targetRepositoryManager.getInstanceId(),
          targetRepository.getPublicId(), targetRepository.getId());
      return new MigrationDetails(MigrationState.FAILED);
    }
    return new MigrationDetails(repositoryMigration.getState());
  }

}
