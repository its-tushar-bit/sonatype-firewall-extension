/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.79
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

  @Inject
  public RepositoryComponentDeleteService(
      FirewallIgnorePatternService firewallIgnorePatternService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ComponentLabelDAO componentLabelDAO)
  {
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.componentLabelDAO = componentLabelDAO;
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
      repositoryPolicyViolationDAO.getByRepositoryIdAndPathname(tx, repoId, componentPath)
          .forEach(repositoryPolicyViolation -> repositoryPolicyViolationDAO.delete(tx, repositoryPolicyViolation));

      // Delete component itself
      repositoryComponentDAO.delete(tx, component);

      tx.commit();
      log.info("Deleted ignored repository component {} (hash: {}).", componentPath, componentHash);
    }
  }
}
