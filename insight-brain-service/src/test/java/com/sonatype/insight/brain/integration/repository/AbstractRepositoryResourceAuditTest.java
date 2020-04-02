/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRepositoryResourceAuditTest
    extends AbstractAuditTest
{
  protected static final String REPOSITORY_MANAGER_INSTANCE_ID = "repoManInsId";

  protected static final String REPOSITORY_PUBLIC_ID = "repoPubId";

  protected abstract String getEnablePath();

  protected abstract String getResourcePath();

  protected abstract String getEvaluateComponentsPath();

  protected abstract String getQuarantinePath();

  protected abstract String getComponentsPath();

  protected abstract String getEvaluateComponentWithQuarantinePath();

  @Test
  public void testSetEnabled_Connect() throws Exception {
    tempEntity.newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);

    enableRequest(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true).post();

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

    enableRequest(repositoryManager.getInstanceId(), repository.getPublicId(), false).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DISCONNECT_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  public void testSetEnabled_Unauthorized() throws Exception {
    enableRequest(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONNECT_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, new Repository(null, REPOSITORY_PUBLIC_ID));
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

  @Test
  public void testEvaluateComponents_InitialAudit() throws Exception {
    testEvaluateComponents(false, 0, RepositoryComponentEvaluationDataRequestList.INITIAL_AUDIT);
  }

  @Test
  public void testEvaluateComponents_NewComponent() throws Exception {
    testEvaluateComponents(false, 1, RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);
  }

  @Test
  public void testEvaluateComponents_Reevaluation() throws Exception {
    testEvaluateComponents(false, 2, RepositoryComponentEvaluationDataRequestList.REEVALUATION);
  }

  @Test
  public void testEvaluateComponents_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testEvaluateComponents_NullComponentsAndCause() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    RepositoryComponentEvaluationDataRequestList evalList = new RepositoryComponentEvaluationDataRequestList();
    evalList.components = null;
    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), evalList).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, 0, null);
  }

  @Test
  public void testEvaluateComponentWithQuarantine() throws Exception {
    testEvaluateComponents(true, 2, RepositoryComponentEvaluationDataRequestList.NEW_COMPONENT);
  }

  @Test
  public void testEvaluateComponentWithQuarantine_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testEvaluateComponentsWithQuarantine_RetainSubEvent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    tempEntity.newPolicy(failProxyOnExactMatch());
    RepositoryComponentEvaluationDataRequestList repoComponentEvalList = repoComponentEvalList(1);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RETAIN_QUARANTINE, null, SYSTEM_USER);
    assertRepositoryData(auditDTO, repository);
    assertComponentData(auditDTO, repoComponentEvalList.components.get(0).hash,
        repoComponentEvalList.components.get(0).pathname);
  }

  private Policy failProxyOnExactMatch() {
    Policy policy = new Policy();
    policy.setName("policy");
    policy.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    Constraint constraint = new Constraint(null, "constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(MatchStateConditionType.ID, "is", MatchState.EXACT.getId()));
    policy.addConstraint(constraint);
    policy.getActions().put(Stage.ID_PROXY, "fail");
    return policy;
  }

  @Test
  public void testEvaluateComponents_QuarantinedComponent_ResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), null);
    repositoryComponent.setHash("differentHash");
    new RepositoryComponentDAO().update(repositoryComponent);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList(1))
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentData(auditDTO, repositoryComponent.getHash(), repositoryComponent.getPathname());
  }

  @Test
  public void testEvaluateComponents_NeverQuarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", null, null);
    repositoryComponent.setHash("differentHash");
    new RepositoryComponentDAO().update(repositoryComponent);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList(1))
        .post();

    assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  @Test
  public void testEvaluateComponents_UnquarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), new Date());
    repositoryComponent.setHash("differentHash");
    new RepositoryComponentDAO().update(repositoryComponent);

    evaluateRequest(false, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList(1))
        .post();

    assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  private void testEvaluateComponents(boolean withQuarantine, int count, String cause) throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequestList repoComponentEvalList = repoComponentEvalList(count);
    repoComponentEvalList.cause = cause;

    evaluateRequest(withQuarantine, repositoryManager.getInstanceId(), repository.getPublicId(), repoComponentEvalList)
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, count, cause.replace('_', '-'));
  }

  @Test
  public void testSetQuarantine_Enabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    quarantineRequest(repositoryManager.getInstanceId(), repository.getPublicId(), true).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "quarantine", "enabled");
  }

  @Test
  public void testSetQuarantine_Disabled() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    quarantineRequest(repositoryManager.getInstanceId(), repository.getPublicId(), false).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "quarantine", "disabled");
  }

  @Test
  public void testSetQuarantine_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    quarantineRequest(repositoryManager.getInstanceId(), repository.getPublicId(), true).with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testRemoveComponent_QuarantinedComponent_ResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), null);

    componentRequest(repositoryManager.getInstanceId(), repository.getPublicId(), repositoryComponent.getPathname())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RESET_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentData(auditDTO, repositoryComponent.getHash(), repositoryComponent.getPathname());
  }

  @Test
  public void testRemoveComponent_NeverQuarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", null, null);

    assertResponseStatus(204,
        componentRequest(repositoryManager.getInstanceId(), repository.getPublicId(), repositoryComponent.getPathname())
            .delete());

    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  @Test
  public void testRemoveComponent_UnquarantinedComponent_NoResetQuarantineSubEvent() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date(), new Date());

    assertResponseStatus(204,
        componentRequest(repositoryManager.getInstanceId(), repository.getPublicId(), repositoryComponent.getPathname())
            .delete());

    assertThat(awaitLogEntries(AuditEvent.RESET_QUARANTINE, 0)).isEmpty();
  }

  @Test
  public void testEvaluateComponentWithQuarantine_ImplicitlyEnableQuarantine() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, false);

    evaluateRequest(true, repositoryManager.getInstanceId(), repository.getPublicId(),
        new RepositoryComponentEvaluationDataRequestList()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "quarantine", "enabled");
  }

  private HttpRequest enableRequest(String repositoryManagerInstanceId, String repositoryPublicId, boolean enabled) {
    return restRequest().path(getResourcePath(), getEnablePath())
        .parameter(repositoryManagerInstanceId, repositoryPublicId, enabled);
  }

  private HttpRequest evaluateRequest(boolean withQuarantine,
                                      String repositoryManagerInstanceId,
                                      String repositoryPublicId,
                                      RepositoryComponentEvaluationDataRequestList repoComponentEvalList)
  {
    return restRequest().path(getResourcePath(),
        withQuarantine ? getEvaluateComponentWithQuarantinePath() : getEvaluateComponentsPath())
        .parameter(repositoryManagerInstanceId, repositoryPublicId).body(repoComponentEvalList);
  }

  private HttpRequest quarantineRequest(String repositoryManagerInstanceId,
                                        String repositoryPublicId,
                                        boolean enabled)
  {
    return restRequest().path(getResourcePath(), getQuarantinePath())
        .parameter(repositoryManagerInstanceId, repositoryPublicId, enabled);
  }

  private HttpRequest componentRequest(String repositoryManagerInstanceId, String repositoryPublicId, String pathname) {
    return restRequest().path(getResourcePath(), getComponentsPath())
        .parameter(repositoryManagerInstanceId, repositoryPublicId, pathname);
  }

  protected void assertRepositoryEvaluationData(AuditDTO auditDTO, int componentCount, String evaluationCause) {
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "evaluationCause", evaluationCause);
  }

  protected RepositoryComponentEvaluationDataRequestList repoComponentEvalList(int componentCount) {
    RepositoryComponentEvaluationDataRequestList evalList = new RepositoryComponentEvaluationDataRequestList();
    for (int i = 0; i < componentCount; i++) {
      RepositoryComponentEvaluationDataRequest evalRequest = new RepositoryComponentEvaluationDataRequest();
      evalRequest.format = "format";
      evalRequest.pathname = "pathname";
      evalRequest.hash = "hash";
      evalList.components.add(evalRequest);
    }
    mockHdsResponse(componentCount);
    return evalList;
  }

  private void mockHdsResponse(int componentCount) {
    ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
    componentEvaluationDataList.components = new ArrayList<>();
    for (int i = 0; i < componentCount; i++) {
      ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
      componentEvaluationData.requestIndex = i;
      componentEvaluationData.hash = "hash";
      componentEvaluationData.matchState = MatchState.EXACT.getId();
      componentEvaluationData.declaredLicenses = Collections.emptySet();
      componentEvaluationData.observedLicenses = Collections.emptySet();
      componentEvaluationDataList.components.add(componentEvaluationData);
    }
    hdsRespondWith(componentEvaluationDataList).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }

  private void assertComponentData(AuditDTO auditDTO, String componentHash, String componentPathname) {
    assertCustomData(auditDTO, "componentHash", componentHash);
    assertCustomData(auditDTO, "componentPathname", componentPathname);
  }
}
