/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Async task for processing cascade re-evaluation across multiple repositories. Creates progress tracking entries and
 * delegates component re-evaluation to existing logic.
 *
 * @since 1.196
 */
public class CascadeReevaluationTask
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(CascadeReevaluationTask.class);

  private final String cascadeRequestId;

  private final String componentHash;

  private final ReevaluateCascadeProgressDAO cascadeProgressDAO;

  private final ReevaluateCascadeRequestDAO cascadeRequestDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  public CascadeReevaluationTask(
      final String cascadeRequestId,
      final String componentHash,
      final ReevaluateCascadeProgressDAO cascadeProgressDAO,
      final ReevaluateCascadeRequestDAO cascadeRequestDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyEvaluator repositoryPolicyEvaluator)
  {
    this.cascadeRequestId = cascadeRequestId;
    this.componentHash = componentHash;
    this.cascadeProgressDAO = cascadeProgressDAO;
    this.cascadeRequestDAO = cascadeRequestDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
  }

  @Override
  public void run() {
    log.info("Starting cascade re-evaluation for component {}. Request ID: {}",
        componentHash, cascadeRequestId);

    AuditData.get()
        .setData("cascadeRequestId", cascadeRequestId)
        .setData("componentHash", componentHash)
        .setData("evaluationCause", RepositoryComponentEvaluationDataRequestList.REEVALUATION);

    updateRequestStatus(ReevaluateCascadeRequestStatus.IN_PROGRESS);

    try {
      Map<Repository, List<RepositoryComponent>> repositoryToComponents =
          getRepositoryToComponentsByHash(componentHash);

      AuditData.get().setData("repositoryCount", repositoryToComponents.size());

      if (repositoryToComponents.isEmpty()) {
        updateRequestStatus(ReevaluateCascadeRequestStatus.NO_COMPONENTS_FOUND);
        log.info("Cascade re-evaluation request {} completed with no components found for hash: {}", cascadeRequestId,
            componentHash);
        return;
      }

      log.info("Found {} repositories containing component {} for cascade re-evaluation",
          repositoryToComponents.size(), componentHash);

      int totalComponents = 0;
      int successfulEvaluations = 0;
      int failedEvaluations = 0;

      // Process each repository with its pre-loaded components
      for (Map.Entry<Repository, List<RepositoryComponent>> entry : repositoryToComponents.entrySet()) {
        Repository repository = entry.getKey();
        List<RepositoryComponent> components = entry.getValue();

        try {
          processRepositoryWithComponents(repository, components);
          totalComponents += components.size();
          successfulEvaluations += components.size();
        }
        catch (Exception e) {
          failedEvaluations++;
          log.error("Failed to process repository {} for cascade re-evaluation request {} of component {}: {}",
              repository.getId(), cascadeRequestId, componentHash, e.getMessage(), e);
        }
      }

      // Mark as completed
      updateRequestStatus(ReevaluateCascadeRequestStatus.COMPLETED);
      log.info(
          "Completed cascade re-evaluation request {} for component {}. Processed {} components in {} repositories. " +
              "Successes: {}, Failures: {}",
          cascadeRequestId, componentHash, totalComponents,
          repositoryToComponents.size(), successfulEvaluations, failedEvaluations);

      AuditData.get()
          .setData("totalComponents", totalComponents)
          .setData("successfulEvaluations", successfulEvaluations)
          .setData("failedEvaluations", failedEvaluations);
    }
    catch (Exception e) {
      updateRequestStatus(ReevaluateCascadeRequestStatus.FAILED);
      log.error("Cascade re-evaluation failed for component {} with request ID {}: {}",
          componentHash, cascadeRequestId, e.getMessage(), e);
      throw e;
    }
  }

  private Map<Repository, List<RepositoryComponent>> getRepositoryToComponentsByHash(final String componentHash) {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      return repositoryComponentDAO.getRepositoryToComponentsByHash(tx, componentHash);
    }
  }

  private void processRepositoryWithComponents(
      final Repository repository,
      final List<RepositoryComponent> components)
  {
    log.debug("Processing repository {} with {} pre-loaded components", repository.getId(), components.size());
    Map<String, ReevaluateCascadeProgress> progressByComponentId = new HashMap<>();

    try (TransactionContext tx = cascadeProgressDAO.createTransactionContext()) {
      tx.begin();

      for (RepositoryComponent component : components) {
        ReevaluateCascadeProgress progress = new ReevaluateCascadeProgress(
            cascadeRequestId, repository.getId(), component.getId(), ReevaluateCascadeProgressStatus.PENDING);

        cascadeProgressDAO.insert(tx, progress);
        progressByComponentId.put(component.getId(), progress);
      }

      tx.commit();
    }

    processComponentsBatch(repository, components, progressByComponentId);
  }

  private void processComponentsBatch(
      final Repository repository,
      final List<RepositoryComponent> components,
      final Map<String, ReevaluateCascadeProgress> progressByComponentId)
  {
    log.debug("Batch processing {} components for cascade re-evaluation in repository {}",
        components.size(), repository.getId());

    try {
      RepositoryComponentEvaluationDataRequestList request = new RepositoryComponentEvaluationDataRequestList(
          RepositoryComponentEvaluationDataRequestList.REEVALUATION);

      for (RepositoryComponent component : components) {
        RepositoryComponentEvaluationDataRequest componentRequest = new RepositoryComponentEvaluationDataRequest(
            repository.getFormat(), component.getPathname(), component.getHash());
        request.components.add(componentRequest);
      }

      log.debug("Performing batch policy evaluation for {} components in repository {}",
          components.size(), repository.getId());

      RepositoryComponentEvaluationDataList evaluationResults = repositoryPolicyEvaluator.evaluate(repository, request,
          false /* withQuarantine */, null /* clientUserAgent */);

      log.debug("Successfully batch re-evaluated {} components in repository {}",
          components.size(), repository.getId());

      // Update quarantine values based on evaluation results
      if (CollectionUtils.isNotEmpty(evaluationResults.componentEvalResults)) {
        for (RepositoryComponentEvaluationData evaluationData : evaluationResults.componentEvalResults) {
          boolean newQuarantineStatus = evaluationData.quarantine;
          RepositoryComponent component = components.get(evaluationData.requestIndex);
          ReevaluateCascadeProgress progress = progressByComponentId.get(component.getId());
          progress.setQuarantined(newQuarantineStatus);
          updateProgressCompleted(progress);
          log.debug("Updated quarantine status for component {} in repository {} to: {}",
              component.getPathname(), repository.getId(), newQuarantineStatus);
        }
      }
    }
    catch (Exception e) {
      log.error(
          "Failed to re-evaluate {} components in repository {} in cascade re-evaluation request {} with hash {}: {}",
          components.size(), repository.getId(), cascadeRequestId, componentHash, e.getMessage(), e);

      for (RepositoryComponent component : components) {
        updateProgressFailed(progressByComponentId.get(component.getId()));
      }
    }
  }

  private void updateProgressCompleted(final ReevaluateCascadeProgress progress) {
    progress.markCompleted();
    cascadeProgressDAO.update(progress);
  }

  private void updateProgressFailed(final ReevaluateCascadeProgress progress) {
    progress.markFailed();
    cascadeProgressDAO.update(progress);
  }

  private void updateRequestStatus(final ReevaluateCascadeRequestStatus status) {
    try (TransactionContext tx = cascadeRequestDAO.createTransactionContext()) {
      tx.begin();
      ReevaluateCascadeRequest request = cascadeRequestDAO.getById(tx, cascadeRequestId);
      if (request != null) {
        request.setStatus(status);
        cascadeRequestDAO.update(tx, request);
        tx.commit();
        log.debug("Updated cascade request {} status to {}", cascadeRequestId, status);
      }
      else {
        log.warn("Could not find cascade request {} to update status to {}", cascadeRequestId, status);
      }
    }
    catch (Exception e) {
      log.error("Failed to update cascade request {} status to {}: {}",
          cascadeRequestId, status, e.getMessage(), e);
    }
  }
}
