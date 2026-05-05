/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted.monitoring;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs continuous monitoring for hosted repositories.
 * <p>
 * For each hosted repository with {@code monitoring_enabled=true}, fetches all known components
 * from {@code repository_component} and re-evaluates them against current policies via HDS.
 * Results are persisted back to {@code repository_component} (updated {@code last_evaluation_time})
 * and {@code repository_policy_violation}.
 * <p>
 * Guarded by {@link SystemConfigurationPropertyFeature#HOSTED_REPOSITORY_EVALUATION}.
 */
@Named
public class HostedRepositoryMonitor
{
  private static final Logger log = LoggerFactory.getLogger(HostedRepositoryMonitor.class);

  static final int COMPONENT_PAGE_SIZE = 500;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  public HostedRepositoryMonitor(
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO)
  {
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  public void run() {
    if (!SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      log.debug("Hosted repository evaluation feature is disabled, skipping CM run");
      return;
    }

    List<Repository> monitoredRepos = repositoryDAO.getHostedRepositoriesWithMonitoringEnabled();
    if (monitoredRepos.isEmpty()) {
      log.debug("No hosted repositories with monitoring enabled, skipping CM run");
      return;
    }

    log.info("Starting hosted repository CM run for {} monitored repositories", monitoredRepos.size());
    long start = System.currentTimeMillis();
    int evaluated = 0;
    int skipped = 0;

    for (Repository repository : monitoredRepos) {
      try {
        boolean didEvaluate = evaluateRepository(repository);
        if (didEvaluate) {
          evaluated++;
        }
        else {
          skipped++;
        }
      }
      catch (Exception e) {
        log.warn("CM evaluation failed for hosted repository id={}, publicId={}: {}",
            repository.getId(), repository.getPublicId(), e.getMessage(), e);
      }
    }

    log.info("Hosted repository CM run completed in {} ms: {} evaluated, {} skipped",
        System.currentTimeMillis() - start, evaluated, skipped);
  }

  private List<RepositoryComponent> loadAllComponents(final String repositoryId) {
    List<RepositoryComponent> all = new ArrayList<>();
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      int offset = 0;
      List<RepositoryComponent> page;
      do {
        page = repositoryComponentDAO.getByRepositoryId(tx, repositoryId, COMPONENT_PAGE_SIZE, offset);
        all.addAll(page);
        offset += page.size();
      }
      while (page.size() == COMPONENT_PAGE_SIZE);
      tx.commit();
    }
    return all;
  }

  private boolean evaluateRepository(final Repository repository) {
    String repositoryId = repository.getId();
    List<RepositoryComponent> components = loadAllComponents(repositoryId);

    if (components.isEmpty()) {
      log.debug("No components found for hosted repository id={}, skipping", repositoryId);
      return false;
    }

    String repoFormat = repository.getFormat();
    if (repoFormat == null) {
      log.warn("Repository id={} has no format set, skipping CM evaluation", repositoryId);
      return false;
    }

    // All components in a repo share the same stage (repo-level config from NXRM).
    // Read from the first component that has a recorded stage; fall back to ComplianceStageType.ID
    // for repos that existed before this column was added.
    String stage = components.stream()
        .map(RepositoryComponent::getLastEvaluationStage)
        .filter(s -> s != null)
        .findFirst()
        .orElse(ComplianceStageType.ID);

    RepositoryComponentEvaluationDataRequestList request =
        new RepositoryComponentEvaluationDataRequestList(RepositoryPolicyEvaluator.CONTINUOUS_MONITORING_CAUSE);
    for (RepositoryComponent component : components) {
      if (component.getHash() != null && component.getPathname() != null) {
        request.components.add(new RepositoryComponentEvaluationDataRequest(
            repoFormat,
            component.getPathname(),
            component.getHash()));
      }
    }

    if (request.components.isEmpty()) {
      log.debug("No evaluatable components (missing hash/pathname) for repository id={}, skipping", repositoryId);
      return false;
    }

    log.debug("Evaluating {} components for hosted repository id={} using stage={}",
        request.components.size(), repositoryId, stage);
    repositoryPolicyEvaluator.evaluateForMonitoring(repository, request, stage);
    stampComponentIds(repositoryId, components);
    return true;
  }

  private void stampComponentIds(final String repositoryId, final List<RepositoryComponent> components) {
    try (TransactionContext tx = repositoryPolicyViolationDAO.createTransactionContext()) {
      tx.begin();
      for (RepositoryComponent component : components) {
        if (component.getComponentId() != null && component.getPathname() != null) {
          repositoryPolicyViolationDAO.stampComponentId(tx, repositoryId, component.getPathname(),
              component.getComponentId());
        }
      }
      tx.commit();
    }
    catch (Exception e) {
      log.warn("Failed to stamp component_ids for repository id={}: {}", repositoryId, e.getMessage(), e);
    }
  }
}
