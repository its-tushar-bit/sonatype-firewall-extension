/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseReason;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.80
 */
@Named
public class RepositoryComponentDeleteService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryComponentDeleteService.class);

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  private final RepositoryDAO repositoryDAO;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  public RepositoryComponentDeleteService(
      FirewallIgnorePatternService firewallIgnorePatternService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ComponentLabelDAO componentLabelDAO,
      RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator,
      RepositoryDAO repositoryDAO,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory)
  {
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.repositoryComponentTelemetryCreator = repositoryComponentTelemetryCreator;
    this.repositoryDAO = repositoryDAO;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
  }

  public void deleteUnknownIgnoredComponents(Repository repository) {
    List<RepositoryComponent> unknownAndIgnoredComponents = findUnknownAndIgnoredComponents(repository);
    log.info("Deleting {} ignored components from repository: {}.",
        unknownAndIgnoredComponents.size(), repository.getPublicId());
    unknownAndIgnoredComponents.forEach(this::deleteComponent);
  }

  private List<RepositoryComponent> findUnknownAndIgnoredComponents(Repository repository) {
    Predicate<String> componentPathnameMatchesIgnorePattern =
        firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository);
    return repositoryComponentDAO.getByRepositoryIdAndMatchStateId(repository.getId(), MatchState.UNKNOWN.getId())
        .stream()
        .filter(repositoryComponent -> componentPathnameMatchesIgnorePattern.test(repositoryComponent.getPathname()))
        .collect(Collectors.toList());
  }

  public void deleteComponent(RepositoryComponent component) {
    String repoId = component.getRepositoryId();
    String componentPath = component.getPathname();
    String componentHash = component.getHash();

    try (TransactionContext tx = repositoryPolicyViolationDAO.createTransactionContext()) {
      tx.begin();

      // Delete related component labels
      componentLabelDAO.getByOwnerIdAndHash(tx, repoId, componentHash)
          .forEach(componentLabel -> componentLabelDAO.delete(tx, componentLabel));

      // Delete related policy waivers
      policyWaiverDAO.getByOwnerIdAndHash(tx, repoId, componentHash)
          .forEach(policyWaiver -> policyWaiverDAO.delete(tx, policyWaiver));

      // Delete related policy violations
      List<RepositoryPolicyViolation> repositoryPolicyViolations =
          repositoryPolicyViolationDAO.getByRepositoryIdAndPathname(tx, repoId, componentPath);
      repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);
      repositoryPolicyViolations
          .forEach(repositoryPolicyViolation -> repositoryPolicyViolationDAO.delete(tx, repositoryPolicyViolation));

      // Delete component itself
      repositoryComponentDAO.delete(tx, component);

      tx.commit();
      log.info("Deleted repository component {} (hash: {}).", componentPath, componentHash);

      if (!repositoryPolicyViolations.isEmpty()) {
        Repository repository = repositoryDAO.getById(component.getRepositoryId());

        // If the component was quarantined, treat deletion as a manual release with "Deleted" reason
        if (component.isQuarantined()) {
          repositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(component, repositoryPolicyViolations,
              repository.getRepositoryManagerId(), repository.getPublicId(),
              RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE,
              ReleaseQuarantineType.MANUAL, ReleaseReason.DELETED.getDescription(), Collections.emptyList());
        }

        repositoryComponentTelemetryCreator
            .sendRepositoryComponentTelemetry(component, repositoryPolicyViolations,
                repository.getRepositoryManagerId(), repository.getPublicId(),
                RepositoryComponentTelemetryEventType.DELETE, Collections.emptyList(), null);
      }
    }
  }
}
