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
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Test;

public class RepositoryResourceAuditTest
    extends AbstractRepositoryResourceAuditTest
{
  @Override
  protected String getEnablePath() {
    return RepositoryResource.ENABLE_PATH;
  }

  @Override
  protected String getResourcePath() {
    return RepositoryResource.RESOURCE_PATH;
  }

  @Override
  protected String getEvaluateComponentsPath() {
    return RepositoryResource.EVALUATE_COMPONENTS_PATH;
  }

  @Override
  protected String getQuarantinePath() {
    return RepositoryResource.QUARANTINE_PATH;
  }

  @Override
  protected String getComponentsPath() {
    return RepositoryResource.COMPONENTS_PATH;
  }

  @Override
  protected String getEvaluateComponentWithQuarantinePath() {
    return RepositoryResource.EVALUATE_COMPONENT_WITH_QUARANTINE_PATH;
  }

  @Test
  public void testEvaluateComponentsAdhoc_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    evaluateAdhocRequest(new RepositoryComponentEvaluationDataRequestList()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testEvaluateComponentsAdhoc_OneComponent() throws Exception {
    testEvaluateComponentsAdhoc(1);
  }

  @Test
  public void testEvaluateComponentsAdhoc_TwoComponents() throws Exception {
    testEvaluateComponentsAdhoc(2);
  }

  @Test
  public void testEvaluateComponentsAdhoc_NoComponents() throws Exception {
    testEvaluateComponentsAdhoc(0);
  }

  private HttpRequest evaluateAdhocRequest(
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    return restRequest()
        .path(getResourcePath())
        .path(RepositoryResource.EVALUATE_COMPONENTS_ADHOC_PATH)
        .parameter(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID)
        .body(componentEvaluationDataRequestList);
  }

  private void testEvaluateComponentsAdhoc(int count) throws Exception {
    Repository repository = tempEntity.newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequestList repoComponentEvalList = repoComponentEvalList(count);
    repoComponentEvalList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;

    evaluateAdhocRequest(repoComponentEvalList).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, count, RepositoryComponentEvaluationDataRequestList.ADHOC);
  }
}
