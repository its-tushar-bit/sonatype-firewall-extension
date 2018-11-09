/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class RepositoryResourceAuditTest
    extends AbstractAuditTest
{
  private static final String REPOSITORY_MANAGER_INSTANCE_ID = "repoManInsId";

  private static final String REPOSITORY_PUBLIC_ID = "repoPubId";

  @Test
  public void testSetEnabled_Connect() throws Exception {
    tempEntity.newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);

    restRequest(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, null);
    Repository repository = new RepositoryDAO()
        .getByRepositoryManagerInstanceIdAndPublicId(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", REPOSITORY_MANAGER_INSTANCE_ID);
  }

  @Test
  public void testSetEnabled_Disconnect() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    restRequest(repositoryManager.getInstanceId(), repository.getPublicId(), false).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DISCONNECT_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  public void testSetEnabled_Unauthorized() throws Exception {
    restRequest(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, new Repository(null, REPOSITORY_PUBLIC_ID));
  }

  private HttpRequest restRequest(String repositoryManagerInstanceId, String repositoryPublicId, boolean enabled) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.ENABLE_PATH)
        .parameter(repositoryManagerInstanceId, repositoryPublicId, enabled);
  }

  @Test
  public void testEvaluateComponents_ImplicitlyEnableAudit() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  public void testEvaluateComponentsWithQuarantine_ImplicitlyEnableAudit() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  private HttpRequest evaluateRequest(boolean withQuarantine,
                                      String repositoryManagerInstanceId,
                                      String repositoryPublicId,
                                      RepositoryComponentEvaluationDataRequestList repoComponentEvalList)
  {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, withQuarantine ?
        RepositoryResource.EVALUATE_COMPONENT_WITH_QUARANTINE_PATH : RepositoryResource.EVALUATE_COMPONENTS_PATH)
        .parameter(repositoryManagerInstanceId, repositoryPublicId).body(repoComponentEvalList);
  }
}
