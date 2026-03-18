/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.18.0
 */
public class RepositoryReevaluationTask
    implements Runnable
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryReevaluationTask.class);

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final Repository repository;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final ExecutorService executor;

  private final int maxRepositoryEvaluationRequestSize;

  private final ClusterLockManager clusterLockManager;

  public RepositoryReevaluationTask(
      Repository repository,
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      ExecutorService executor,
      int maxRepositoryEvaluationRequestSize,
      final RepositoryComponentDAO repositoryComponentDAO,
      final ClusterLockManager clusterLockManager)
  {
    this.repository = repository;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.executor = executor;
    this.maxRepositoryEvaluationRequestSize = maxRepositoryEvaluationRequestSize;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.clusterLockManager = clusterLockManager;
  }

  @Override
  public void run() {
    try (ClusterLock clusterLock = clusterLockManager.createForRepositoryReevaluation(repository)) {
      if (clusterLock.tryLock()) {
        log.debug("Starting re-evaluation for repository {}:{} ({})", repository.getRepositoryManagerId(),
            repository.getPublicId(), repository.getId());
        List<RepositoryComponent> repositoryComponentsList =
            repositoryComponentDAO.getByRepositoryId(repository.getId());
        Iterator<RepositoryComponent> repositoryComponents = repositoryComponentsList.iterator();

        AuditData.get()
            .setData("componentCount", repositoryComponentsList.size())
            .setData("evaluationCause", RepositoryComponentEvaluationDataRequestList.REEVALUATION);

        if (!repositoryComponents.hasNext()) {
          clusterLock.unlock();
        }

        int componentCount = 0;
        final AtomicInteger activeTasks = new AtomicInteger();
        while (repositoryComponents.hasNext()) {
          RepositoryComponentEvaluationDataRequestList request = createEvaluationRequest(repositoryComponents);
          componentCount += request.components.size();

          activeTasks.incrementAndGet();

          try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.EVALUATE_REPOSITORY, true)) {
            AuditData.get()
                .setRepository(repository)
                .setData("componentCount", request.components.size())
                .setData("evaluationCause", RepositoryComponentEvaluationDataRequestList.REEVALUATION)
                .continueAsync(executor, new PolicyEvaluationTask(request, activeTasks, clusterLock));
          }
        }
        log.debug("Enqueued {} components of repository {}:{} ({}) for re-evaluation", componentCount,
            repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
      }
      else {
        log.debug("Skipping, re-evaluation for repository {}:{} ({}) is already in progress",
            repository.getRepositoryManagerId(), repository.getPublicId(), repository.getId());
        clusterLock.unlock();
      }
    }
    catch (Exception e) {
      log.error("An error occurred while re-evaluating repository {}:{} ({})", repository.getRepositoryManagerId(),
          repository.getPublicId(), repository.getId(), e);
      AuditData.get().setException(e);
    }
    finally {
      executor.shutdown();
    }
  }

  private RepositoryComponentEvaluationDataRequestList createEvaluationRequest(
      Iterator<RepositoryComponent> components)
  {
    int limit = 0;

    RepositoryComponentEvaluationDataRequestList request = new RepositoryComponentEvaluationDataRequestList(
        RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    while (components.hasNext() && limit++ < maxRepositoryEvaluationRequestSize) {
      RepositoryComponent component = components.next();
      request.components.add(new RepositoryComponentEvaluationDataRequest(repository.getFormat(), component
          .getPathname(), component.getHash()));
    }
    return request;
  }

  private class PolicyEvaluationTask
      implements Runnable
  {
    private final RepositoryComponentEvaluationDataRequestList request;

    private final AtomicInteger activeTasks;

    private final ClusterLock clusterLock;

    PolicyEvaluationTask(
        RepositoryComponentEvaluationDataRequestList request,
        AtomicInteger activeTasks,
        ClusterLock clusterLock)
    {
      this.request = request;
      this.activeTasks = activeTasks;
      this.clusterLock = clusterLock;
    }

    @Override
    public void run() {
      try {
        repositoryPolicyEvaluator.evaluate(repository, request, false /* withQuarantine */,
            // due to asynchronous handling, we can't use the original 'threadlocal' stored clientRequest
            null /* clientUserAgent */);
      }
      catch (Exception e) {
        AuditData.get().setException(e);
        log.error("An error occurred while re-evaluating repository {}:{} ({})", repository.getRepositoryManagerId(),
            repository.getPublicId(), repository.getId(), e);
      }
      finally {
        if (activeTasks.decrementAndGet() == 0) {
          log.debug("Completed re-evaluation of repository {}:{} ({})", repository.getRepositoryManagerId(),
              repository.getPublicId(), repository.getId());
          clusterLock.unlock();
        }
      }
    }
  }
}
