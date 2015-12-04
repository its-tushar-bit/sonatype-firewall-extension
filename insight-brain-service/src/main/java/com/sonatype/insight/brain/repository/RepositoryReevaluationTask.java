/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
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

  private RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  private final Repository repository;

  private final RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  private final Executor executor;

  private final Map<String, AtomicInteger> reevaluations;

  public RepositoryReevaluationTask(Repository repository, RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      Executor executor, Map<String, AtomicInteger> reevaluations)
  {
    this.repository = repository;
    this.repositoryPolicyEvaluator = repositoryPolicyEvaluator;
    this.executor = executor;
    this.reevaluations = reevaluations;
  }

  @Override
  public void run() {
    try {
      Iterator<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(repository.getId())
          .iterator();

      final AtomicInteger activeTasks = reevaluations.get(repository.getId());
      while (repositoryComponents.hasNext()) {
        RepositoryComponentEvaluationDataRequestList request = createEvaluationRequest(repositoryComponents);

        activeTasks.incrementAndGet();

        executor.execute(new PolicyEvaluationTask(request, activeTasks));
      }
    }
    catch (Exception e) {
      log.error("An error occured while re-evaluating repository {}", repository.getId(), e);
    }
  }

  private RepositoryComponentEvaluationDataRequestList createEvaluationRequest(Iterator<RepositoryComponent> components) {
    int limit = 0;

    RepositoryComponentEvaluationDataRequestList request = new RepositoryComponentEvaluationDataRequestList();
    while (components.hasNext() && limit++ < 100) {
      RepositoryComponent component = components.next();
      request.components.add(new RepositoryComponentEvaluationDataRequest(repository.getFormat(),
          component.getPathname(), component.getHash()));
    }
    return request;
  }

  private class PolicyEvaluationTask
      implements Runnable
  {

    private final RepositoryComponentEvaluationDataRequestList request;

    private final AtomicInteger activeTasks;

    PolicyEvaluationTask(RepositoryComponentEvaluationDataRequestList request, AtomicInteger activeTasks) {
      this.request = request;
      this.activeTasks = activeTasks;
    }

    @Override
    public void run() {
      try {
        repositoryPolicyEvaluator.evaluate(repository, request, false);
      }
      catch (Exception e) {
        log.error("An error occured while re-evaluating repository {}", repository.getId(), e);
      }
      finally {
        if (activeTasks.decrementAndGet() == 0) {
          synchronized (reevaluations) {
            reevaluations.remove(repository.getId());
          }
        }
      }
    }
  }
}
