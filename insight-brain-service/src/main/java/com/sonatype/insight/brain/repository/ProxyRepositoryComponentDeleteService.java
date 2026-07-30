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
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.ReleaseReason;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetryCreator;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.80
 */
@Named
public class ProxyRepositoryComponentDeleteService
{
  private static final Logger log = LoggerFactory.getLogger(ProxyRepositoryComponentDeleteService.class);

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator;

  private final RepositoryDAO repositoryDAO;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  public ProxyRepositoryComponentDeleteService(
      FirewallIgnorePatternService firewallIgnorePatternService,
      ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ComponentLabelDAO componentLabelDAO,
      ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator,
      RepositoryDAO repositoryDAO,
      ComponentDetailsLoaderFactory componentDetailsLoaderFactory)
  {
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.proxyRepositoryComponentTelemetryCreator = proxyRepositoryComponentTelemetryCreator;
    this.repositoryDAO = repositoryDAO;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
  }

  public void deleteUnknownIgnoredComponents(Repository repository) {
    List<ProxyRepositoryComponent> unknownAndIgnoredComponents = findUnknownAndIgnoredComponents(repository);
    log.info("Deleting {} ignored components from repository: {}.",
        unknownAndIgnoredComponents.size(), repository.getPublicId());
    unknownAndIgnoredComponents.forEach(this::deleteComponent);
  }

  private List<ProxyRepositoryComponent> findUnknownAndIgnoredComponents(Repository repository) {
    Predicate<String> componentPathnameMatchesIgnorePattern =
        firewallIgnorePatternService.componentPathnameMatchesIgnorePattern(repository);
    return proxyRepositoryComponentDAO.getByRepositoryIdAndMatchStateId(repository.getId(), MatchState.UNKNOWN.getId())
        .stream()
        .filter(proxyRepositoryComponent -> componentPathnameMatchesIgnorePattern
            .test(proxyRepositoryComponent.getPathname()))
        .collect(Collectors.toList());
  }

  public void deleteComponent(ProxyRepositoryComponent component) {
    String repoId = component.getRepositoryId();
    String componentPath = component.getPathname();
    String componentHash = component.getHash();

    try (TransactionContext tx = proxyRepositoryPolicyViolationDAO.createTransactionContext()) {
      tx.begin();

      // Delete related component labels
      componentLabelDAO.getByOwnerIdAndHash(tx, repoId, componentHash)
          .forEach(componentLabel -> componentLabelDAO.delete(tx, componentLabel));

      // Delete related policy waivers
      policyWaiverDAO.getByOwnerIdAndHash(tx, repoId, componentHash)
          .forEach(policyWaiver -> policyWaiverDAO.delete(tx, policyWaiver));

      // Delete related policy violations
      List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations =
          proxyRepositoryPolicyViolationDAO.getByRepositoryIdAndPathname(tx, repoId, componentPath);
      proxyRepositoryPolicyViolationDAO.loadConstraintFacts(proxyRepositoryPolicyViolations);
      proxyRepositoryPolicyViolations
          .forEach(proxyRepositoryPolicyViolation -> proxyRepositoryPolicyViolationDAO.delete(tx,
              proxyRepositoryPolicyViolation));

      // Delete component itself
      proxyRepositoryComponentDAO.delete(tx, component);

      tx.commit();
      log.info("Deleted repository component {} (hash: {}).", componentPath, componentHash);

      if (!proxyRepositoryPolicyViolations.isEmpty()) {
        Repository repository = repositoryDAO.getById(component.getRepositoryId());

        // If the component was quarantined, treat deletion as a manual release with "Deleted" reason
        if (component.isQuarantined()) {
          proxyRepositoryComponentTelemetryCreator.sendRepositoryComponentTelemetry(component,
              proxyRepositoryPolicyViolations,
              repository.getRepositoryManagerId(), repository.getPublicId(),
              RepositoryComponentTelemetryEventType.RELEASE_QUARANTINE,
              ReleaseQuarantineType.MANUAL, ReleaseReason.DELETED.getDescription(), Collections.emptyList());
        }

        proxyRepositoryComponentTelemetryCreator
            .sendRepositoryComponentTelemetry(component, proxyRepositoryPolicyViolations,
                repository.getRepositoryManagerId(), repository.getPublicId(),
                RepositoryComponentTelemetryEventType.DELETE, Collections.emptyList(), null);
      }
    }
  }
}
