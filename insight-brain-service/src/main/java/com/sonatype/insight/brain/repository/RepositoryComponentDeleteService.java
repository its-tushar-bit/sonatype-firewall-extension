/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.integration.repository.RepositoryService;
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

  private final RepositoryService repositoryService;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final ComponentLabelDAO componentLabelDAO;

  @Inject
  public RepositoryComponentDeleteService(
      RepositoryService repositoryService,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      PolicyWaiverDAO policyWaiverDAO,
      ComponentLabelDAO componentLabelDAO)
  {
    this.repositoryService = repositoryService;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.componentLabelDAO = componentLabelDAO;
  }

  public void deleteUnknownIgnoredComponents(Repository repository) {
    List<RepositoryComponent> unknownAndIgnoredComponents = findUnknownAndIgnoredComponents(repository);
    log.info("Deleting {} ignored components from repository: {}.",
        unknownAndIgnoredComponents.size(), repository.getPublicId());
    unknownAndIgnoredComponents.forEach(component -> deleteComponent(repository, component));
  }

  private List<RepositoryComponent> findUnknownAndIgnoredComponents(Repository repository) {
    return repositoryComponentDAO.getByRepositoryIdAndMatchStateId(repository.getId(), MatchState.UNKNOWN.getId())
        .stream().filter(componentPathnameMatchesIgnorePattern(repository)).collect(Collectors.toList());
  }

  private Predicate<RepositoryComponent> componentPathnameMatchesIgnorePattern(Repository repository) {
    FirewallIgnorePatterns firewallIgnorePatterns = repositoryService.getIgnorePatterns();

    List<String> regexForRepository = firewallIgnorePatterns.regexpsByRepositoryFormat.get(repository.getFormat());
    if (regexForRepository == null) {
      return component -> false;
    }

    List<Pattern> patterns = regexForRepository.stream().map(Pattern::compile).collect(Collectors.toList());
    return component -> patterns.stream().anyMatch(pattern -> pattern.matcher(component.getPathname()).matches());
  }

  private void deleteComponent(Repository repository, RepositoryComponent component) {
    String repoId = repository.getId();
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
