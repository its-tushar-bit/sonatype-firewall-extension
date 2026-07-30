/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryMigrationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.32
 */
public class FirewallMigrationWorker
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(FirewallMigrationWorker.class);

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final RepositoryMigrationDAO repositoryMigrationDAO;

  private final Repository sourceRepository;

  private final Repository targetRepository;

  private final RepositoryMigration repositoryMigration;

  FirewallMigrationWorker(
      Repository sourceRepository,
      Repository targetRepository,
      RepositoryMigration repositoryMigration,
      RepositoryDAO repositoryDAO,
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      LicenseOverrideDAO licenseOverrideDAO,
      SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      PolicyWaiverDAO policyWaiverDAO,
      RepositoryMigrationDAO repositoryMigrationDAO)
  {
    this.sourceRepository = sourceRepository;
    this.targetRepository = targetRepository;
    this.repositoryMigration = repositoryMigration;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.repositoryMigrationDAO = repositoryMigrationDAO;
  }

  @Override
  public void run() {
    try {
      log.info("Starting history migration for repository {}:{} ({})", targetRepository.getRepositoryManagerId(),
          targetRepository.getPublicId(), targetRepository.getId());

      repositoryDAO.cascadeDelete(targetRepository, false /* includeRepositoryMigration */);
      migrateRepositoryComponents();
      migratePolicyViolations();
      migrateLicenseOverrides();
      migrateSecurityVulnerabilityOverrides();
      migratePolicyWaivers();
      repositoryMigration.setState(MigrationState.COMPLETED);
      repositoryMigrationDAO.update(repositoryMigration);

      log.info("History migration completed for repository {}:{} ({})", targetRepository.getRepositoryManagerId(),
          targetRepository.getPublicId(), targetRepository.getId());
    }
    catch (Exception e) {
      log.error("Failed to migrate repository history {}:{} ({}); {}", targetRepository.getRepositoryManagerId(),
          targetRepository.getPublicId(), targetRepository.getId(), e.getMessage(), e);

      repositoryMigration.setState(MigrationState.FAILED);
      repositoryMigrationDAO.update(repositoryMigration);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  private void migrateRepositoryComponents() {
    long start = System.currentTimeMillis();

    List<ProxyRepositoryComponent> repositoryComponents =
        proxyRepositoryComponentDAO.getByRepositoryId(sourceRepository.getId());
    log.info("Starting the migration of {} repository components for repository {}:{} ({})...",
        repositoryComponents.size(), targetRepository.getRepositoryManagerId(), targetRepository.getPublicId(),
        targetRepository.getId());
    for (ProxyRepositoryComponent proxyRepositoryComponent : repositoryComponents) {
      log.trace("Migrating repository component {}", proxyRepositoryComponent.getPathname());
      proxyRepositoryComponent.setId(null);
      proxyRepositoryComponent.setRepositoryId(targetRepository.getId());
      proxyRepositoryComponentDAO.insert(proxyRepositoryComponent);
    }

    log.info("Migrated {} repository components in {} ms.", repositoryComponents.size(),
        System.currentTimeMillis() - start);
  }

  private void migratePolicyViolations() {
    long start = System.currentTimeMillis();

    List<ProxyRepositoryPolicyViolation> policyViolations = proxyRepositoryPolicyViolationDAO
        .getByRepositoryId(sourceRepository.getId());
    proxyRepositoryPolicyViolationDAO.loadConstraintFacts(policyViolations);
    log.info("Starting the migration of {} policy violations for repository {}:{} ({})...", policyViolations.size(),
        targetRepository.getRepositoryManagerId(), targetRepository.getPublicId(), targetRepository.getId());
    for (ProxyRepositoryPolicyViolation violation : policyViolations) {
      violation.setId(null);
      violation.setRepositoryId(targetRepository.getId());
      proxyRepositoryPolicyViolationDAO.insert(violation);
    }

    log.info("Migrated {} policy violations in {} ms.", policyViolations.size(), System.currentTimeMillis() - start);
  }

  private void migrateLicenseOverrides() {
    long start = System.currentTimeMillis();

    List<LicenseOverride> licenseOverrides = licenseOverrideDAO.getByOwnerId(sourceRepository.getId());
    log.info("Starting the migration of {} license overrides for repository {}:{} ({})...", licenseOverrides.size(),
        targetRepository.getRepositoryManagerId(), targetRepository.getPublicId(), targetRepository.getId());
    for (LicenseOverride licenseOverride : licenseOverrides) {
      licenseOverride.setId(null);
      licenseOverride.setOwnerId(targetRepository.getId());
      licenseOverrideDAO.insert(licenseOverride);
    }

    log.info("Migrated {} license overrides in {} ms.", licenseOverrides.size(), System.currentTimeMillis() - start);
  }

  private void migrateSecurityVulnerabilityOverrides() {
    long start = System.currentTimeMillis();

    List<SecurityVulnerabilityOverride> securityVulnerabilityOverrides = securityVulnerabilityOverrideDAO
        .getByOwnerId(sourceRepository.getId());
    log.info("Starting the migration of {} security vulnerability overrides for repository {}:{} ({})...",
        securityVulnerabilityOverrides.size(), targetRepository.getRepositoryManagerId(),
        targetRepository.getPublicId(), targetRepository.getId());
    for (SecurityVulnerabilityOverride securityVulnerabilityOverride : securityVulnerabilityOverrides) {
      securityVulnerabilityOverride.setId(null);
      securityVulnerabilityOverride.setOwnerId(targetRepository.getId());
      securityVulnerabilityOverrideDAO.insert(securityVulnerabilityOverride);
    }

    log.info("Migrated {} security vulnerability overrides in {} ms.", securityVulnerabilityOverrides.size(),
        System.currentTimeMillis() - start);
  }

  private void migratePolicyWaivers() {
    long start = System.currentTimeMillis();

    List<PolicyWaiver> waivers = policyWaiverDAO.getByOwnerId(sourceRepository.getId());
    log.info("Starting the migration of {} policy waivers for repository {}:{} ({})...", waivers.size(),
        targetRepository.getRepositoryManagerId(), targetRepository.getPublicId(), targetRepository.getId());
    for (PolicyWaiver waiver : waivers) {
      waiver.setId(null);
      waiver.setOwnerId(targetRepository.getId());
      policyWaiverDAO.insert(waiver);
    }

    log.info("Migrated {} policy waivers in {} ms.", waivers.size(), System.currentTimeMillis() - start);
  }
}
