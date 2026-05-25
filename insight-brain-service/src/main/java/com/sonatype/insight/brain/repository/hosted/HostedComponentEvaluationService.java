/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedDeploymentBlockDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.policy.stages.HostedStageType;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlockViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class HostedComponentEvaluationService
{
  private static final Logger log = LoggerFactory.getLogger(HostedComponentEvaluationService.class);

  private final HostedComponentScanStorageService hostedComponentScanStorageService;

  private final HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  private final HostedComponentScanQueueConsumer hostedComponentScanQueueConsumer;

  private final ScanPersistenceService scanPersistenceService;

  // CLM-39870: synchronous enforcement collaborators — injected via Provider to match the
  // tenant-scoped lifecycle pattern used by the existing HostedComponentScanQueueConsumer.
  private final Provider<ScanUploader> scanUploaderProvider;

  private final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider;

  private final HostedDeploymentBlockDAO hostedDeploymentBlockDAO;

  private final HostedEvaluationResultMapper resultMapper;

  private final HostedEvaluationUrlBuilder urlBuilder;

  @Inject
  public HostedComponentEvaluationService(
      final HostedComponentScanStorageService hostedComponentScanStorageService,
      final HostedComponentScanQueueDAO hostedComponentScanQueueDAO,
      final HostedComponentScanQueueConsumer hostedComponentScanQueueConsumer,
      final ScanPersistenceService scanPersistenceService,
      final Provider<ScanUploader> scanUploaderProvider,
      final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider,
      final HostedDeploymentBlockDAO hostedDeploymentBlockDAO,
      final HostedEvaluationResultMapper resultMapper,
      final HostedEvaluationUrlBuilder urlBuilder)
  {
    this.hostedComponentScanStorageService = hostedComponentScanStorageService;
    this.hostedComponentScanQueueDAO = hostedComponentScanQueueDAO;
    this.hostedComponentScanQueueConsumer = hostedComponentScanQueueConsumer;
    this.scanPersistenceService = scanPersistenceService;
    this.scanUploaderProvider = scanUploaderProvider;
    this.repositoryPolicyEvaluatorProvider = repositoryPolicyEvaluatorProvider;
    this.hostedDeploymentBlockDAO = hostedDeploymentBlockDAO;
    this.resultMapper = resultMapper;
    this.urlBuilder = urlBuilder;
  }

  public String queueScan(
      final String repositoryId,
      final String componentId,
      final String purl,
      final String policyEvaluationStage,
      final File scanFile) throws IOException
  {
    log.debug("Queueing scan for repositoryId={}, componentId={}, purl={}", repositoryId, componentId, purl);

    ScanEntity scanEntity = hostedComponentScanStorageService.storeScanFile(repositoryId, scanFile);
    String scanFileId = scanEntity.getName();

    HostedComponentScanQueue hostedComponentScanQueueEntity = new HostedComponentScanQueue(
        componentId,
        scanFileId,
        HostedComponentScanQueueDAO.Status.PENDING.name(),
        HostedComponentScanQueue.DEFAULT_PRIORITY,
        repositoryId);
    hostedComponentScanQueueEntity.setPurl(purl);
    hostedComponentScanQueueEntity.setPolicyEvaluationStage(policyEvaluationStage);

    try {
      hostedComponentScanQueueDAO.insert(hostedComponentScanQueueEntity);
    }
    catch (RuntimeException e) {
      try {
        scanPersistenceService.deleteScan(scanEntity);
      }
      catch (IOException deleteEx) {
        log.warn("Failed to clean up scan file scanFileId={} after DB insert failure", scanFileId, deleteEx);
      }
      throw e;
    }

    String jobId = hostedComponentScanQueueEntity.getId();
    log.debug("Enqueued scan job id={} for componentId={}, scanFileId={}, repositoryId={}, purl={}",
        jobId, componentId, scanFileId, repositoryId, purl);

    hostedComponentScanQueueConsumer.triggerProcessing();
    return jobId;
  }

  /**
   * Evaluate a hosted-repository upload synchronously on the HTTP servlet thread (CLM-39870).
   * <p>
   * The entire flow runs inline — no queue, no background worker. Steps:
   * <ol>
   * <li>Store the scan file to local staging (reuses existing storage service).</li>
   * <li>Parse {@code scan.xml} to recover the component coordinates + hash.</li>
   * <li>Upload the scan to HDS via {@link ScanUploader}.</li>
   * <li>Call {@link RepositoryPolicyEvaluator#evaluateForHostedEnforcement} with
   * {@link HostedStageType#ID}.</li>
   * <li>Map the evaluator output to a {@link HostedEvaluationResult}.</li>
   * <li>If blocked, persist a {@link HostedDeploymentBlock} row (and child violations) so a
   * future UI can render the details. Allowed-path persistence is handled by the
   * evaluator's normal flow.</li>
   * <li>Build the {@code evaluationUrl} for the response regardless of outcome.</li>
   * <li>Delete the staged scan file in a {@code finally} block.</li>
   * </ol>
   * <p>
   * Blocked deployments do NOT write to {@code repository_component} or
   * {@code repository_policy_violation} — because the artifact never entered the repository,
   * writing to those tables would create phantom components that continuous monitoring would
   * later re-scan. Instead, block details live in the dedicated
   * {@code hosted_deployment_block*} tables, which are garbage-collected by the cleanup task.
   *
   * @param repository the resolved target hosted repository (the resource layer already looked
   *          this up by repositoryManagerInstanceId + publicId; passing it through avoids a
   *          second DB round-trip on the synchronous hot path)
   * @param componentId the IQ-side component identifier (same field async path uses)
   * @param purl the package URL; may be null if unknown
   * @param policyEvaluationStage unused today — kept for API parity with {@link #queueScan};
   *          enforcement always evaluates at {@link HostedStageType#ID}
   * @param scanFile the scan.xml.gz uploaded by NXRM
   * @param correlationId the per-deploy UUID NXRM generated; echoed back
   * @param requestedBy the authenticated NXRM user-agent principal; audited
   * @param clientUserAgent the client tool identifier
   * @return the enforcement verdict for NXRM to relay to the developer
   * @throws IOException if scan storage, parsing, or HDS upload fails
   */
  public HostedEvaluationResult evaluateSynchronously(
      final Repository repository,
      final String componentId,
      final String purl,
      final String policyEvaluationStage,
      final File scanFile,
      final String correlationId,
      final String requestedBy,
      final String clientUserAgent) throws IOException
  {
    if (repository == null) {
      throw new IllegalArgumentException("repository must not be null");
    }
    String repositoryId = repository.getId();
    log.debug(
        "Synchronous enforcement evaluation: repositoryId={}, componentId={}, purl={}, correlationId={}",
        repositoryId, componentId, purl, correlationId);

    ScanEntity scanEntity = hostedComponentScanStorageService.storeScanFile(repositoryId, scanFile);
    try {
      ScanComponentInfo componentInfo = ScanXmlParser.extractComponentInfo(scanEntity);
      if (componentInfo == null) {
        // The insight-scanner couldn't produce a fingerprint for this artifact (typical for
        // sources jars, javadoc jars, signature/checksum files). This is an input-shape
        // problem, not an IQ failure — surface it as a 422 so callers can distinguish it
        // from genuine internal errors and decide to skip enforcement gracefully.
        throw new UnscannableArtifactException(
            "Could not extract component info from scan file for sync evaluation: componentId=" + componentId);
      }

      // Upload to HDS at the hosted stage so downstream correlation in HDS/IQ is consistent.
      ScanReceipt scanReceipt = scanUploaderProvider.get()
          .uploadForRepository(scanEntity, repositoryId, HostedStageType.ID, clientUserAgent, false);
      log.debug("HDS upload completed for sync enforcement: scanId={}, correlationId={}",
          scanReceipt != null ? scanReceipt.getScanId() : null, correlationId);

      RepositoryComponentEvaluationDataRequestList request =
          new RepositoryComponentEvaluationDataRequestList(
              RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);
      request.components.add(new RepositoryComponentEvaluationDataRequest(
          componentInfo.format() != null ? componentInfo.format() : repository.getFormat(),
          componentInfo.pathname(),
          componentInfo.hash()));

      // Step 1: evaluate WITHOUT persisting. We decide whether to persist once we know the verdict.
      // This prevents blocked deployments from writing phantom rows to repository_component.
      RepositoryComponentEvaluationDataList evaluation =
          repositoryPolicyEvaluatorProvider.get()
              .evaluateForHostedEnforcement(repository, request, false /* persistEvaluationResults */,
                  clientUserAgent, HostedStageType.ID);

      String evaluationUrl = urlBuilder.build(repository);
      HostedEvaluationResult verdict = resultMapper.map(evaluation, evaluationUrl, correlationId, componentId);

      if (verdict.blocked()) {
        // Block: persist to the dedicated tables so the future "view blocked deployment" UI has a record.
        // Persistence is an audit concern, not an enforcement concern — if the DB write fails
        // (Aurora failover, pool exhausted, transient network blip), the verdict is still correct
        // and must be returned to NXRM. Letting the exception escape would surface as HTTP 500,
        // which NXRM may treat as "IQ unavailable → fail-open" and silently allow the deployment
        // we just classified as blocked. Mirrors the deleteScan pattern in the finally block.
        try {
          persistBlock(repository, componentInfo, componentId, purl, verdict, requestedBy);
        }
        catch (RuntimeException persistEx) {
          log.error(
              "Failed to persist hosted deployment block (audit only); enforcement verdict still returned: "
                  + "repositoryId={}, correlationId={}",
              repository.getId(), correlationId, persistEx);
        }
      }
      else {
        // Allow: re-run the evaluation with persistence so repository_component and
        // repository_policy_violation are updated just like the async path does.
        // NOTE: this is a second HDS round-trip; acceptable because HDS caches identical scans and
        // the marginal latency is small compared to the initial upload. Revisit if perf data shows
        // this as a hotspot.
        // Wrap the persistence step the same way the block path does — the verdict is already
        // computed and must reach NXRM. If this re-evaluation fails (HDS blip, DB hiccup), CM
        // will pick up the component on its next scheduled scan and recompute violations; the
        // worst case is a delayed continuous-monitoring update, which is recoverable. Letting
        // the exception escape would surface as HTTP 500 and risk NXRM rejecting an
        // already-allowed deployment.
        try {
          repositoryPolicyEvaluatorProvider.get()
              .evaluateForHostedEnforcement(repository, request, true /* persistEvaluationResults */,
                  clientUserAgent, HostedStageType.ID);
        }
        catch (RuntimeException persistEx) {
          log.error(
              "Failed to persist allow-path evaluation results (audit only); enforcement verdict still returned: "
                  + "repositoryId={}, correlationId={}",
              repository.getId(), correlationId, persistEx);
        }
      }

      return verdict;
    }
    finally {
      try {
        scanPersistenceService.deleteScan(scanEntity);
      }
      catch (IOException deleteEx) {
        log.warn("Failed to clean up scan file after sync evaluation: componentId={}, correlationId={}",
            componentId, correlationId, deleteEx);
      }
    }
  }

  private void persistBlock(
      final Repository repository,
      final ScanComponentInfo componentInfo,
      final String componentId,
      final String purl,
      final HostedEvaluationResult verdict,
      final String requestedBy)
  {
    HostedDeploymentBlock block = new HostedDeploymentBlock();
    block.setId(UUID.randomUUID().toString());
    block.setRepositoryId(repository.getId());
    block.setPathname(componentInfo.pathname());
    block.setHash(componentInfo.hash());
    block.setComponentIdFormat(componentInfo.format() != null ? componentInfo.format() : repository.getFormat());
    // Purl is the single best display string available to us here; store it as the display name
    // so the future UI has something meaningful to render without re-computing coordinates.
    block.setDisplayName(purl);
    block.setPolicyAction(verdict.policyAction());
    block.setHighestThreatLevel(verdict.highestThreatLevel());
    block.setEvaluationUrl(verdict.evaluationUrl());
    block.setCorrelationId(verdict.correlationId());
    block.setRequestedBy(requestedBy);
    block.setBlockedTime(new Date());

    List<HostedDeploymentBlockViolation> violations = new ArrayList<>();
    for (HostedBlockingViolation bv : verdict.blockingViolations()) {
      HostedDeploymentBlockViolation violation = new HostedDeploymentBlockViolation();
      violation.setId(UUID.randomUUID().toString());
      violation.setPolicyName(bv.policyName());
      violation.setConstraintName(bv.constraintName());
      violation.setReason(bv.reason());
      violation.setComponentIdentifier(bv.componentIdentifier());
      violations.add(violation);
    }

    try (TransactionContext tx = hostedDeploymentBlockDAO.createTransactionContext()) {
      tx.begin();
      hostedDeploymentBlockDAO.insertWithViolations(tx, block, violations);
      tx.commit();
    }
    log.info("Persisted hosted deployment block: id={}, repositoryId={}, pathname={}, correlationId={}",
        block.getId(), repository.getId(), componentInfo.pathname(), verdict.correlationId());
  }
}
