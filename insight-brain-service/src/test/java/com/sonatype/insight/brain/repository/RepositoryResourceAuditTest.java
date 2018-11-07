/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class RepositoryResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testDeleteRepository() throws Exception {
    Repository repository = tempEntity.newRepository();

    repositoryRequest(repository.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "repositoryManagerInstanceId",
        new RepositoryManagerDAO().getById(repository.getRepositoryManagerId()).getInstanceId());
  }

  @Test
  public void testDeleteRepository_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();

    repositoryRequest(repository.getId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testReevaluateRepository() throws Exception {
    Repository repository = repositoryWithComponents(101);

    evaluateRequest(repository.getId()).post();

    List<AuditDTO> auditDTOs = awaitLogEntries(AuditEvent.INITIATE_EVALUATE_REPOSITORY, 3);
    AuditDTO initiateEvaluateRepository = auditDTOs.stream()
        .filter(auditDTO -> auditDTO.type.equals(AuditEvent.INITIATE_EVALUATE_REPOSITORY.getType())).findFirst().get();
    AuditDTO evaluateRepository100 = findEvaluateRepositoryByComponentCount(auditDTOs, 100);
    AuditDTO evaluateRepository1 = findEvaluateRepositoryByComponentCount(auditDTOs, 1);
    assertStandardData(initiateEvaluateRepository, AuditEvent.INITIATE_EVALUATE_REPOSITORY, null);
    assertStandardData(evaluateRepository100, AuditEvent.EVALUATE_REPOSITORY, null);
    assertStandardData(evaluateRepository1, AuditEvent.EVALUATE_REPOSITORY, null);
    auditDTOs.forEach(auditDTO -> assertRepositoryData(auditDTO, repository));
    assertRepositoryEvaluationData(initiateEvaluateRepository, 101,
        RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    assertRepositoryEvaluationData(evaluateRepository100, 100,
        RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    assertRepositoryEvaluationData(evaluateRepository1, 1, RepositoryComponentEvaluationDataRequestList.REEVALUATION);
  }

  @Test
  public void testReevaluateRepository_Unauthorized() throws Exception {
    Repository repository = repositoryWithComponents(1);

    evaluateRequest(repository.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private AuditDTO findEvaluateRepositoryByComponentCount(Collection<AuditDTO> auditDTOs, int componentCount) {
    return auditDTOs.stream().filter(auditDTO -> auditDTO.type.equals(AuditEvent.EVALUATE_REPOSITORY.getType()) &&
        auditDTO.data.get("componentCount").equals(componentCount)).findFirst().get();
  }

  private HttpRequest repositoryRequest(String repositoryId) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH)
        .parameter(repositoryId);
  }

  private HttpRequest evaluateRequest(String repositoryId) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_PATH)
        .parameter(repositoryId);
  }

  private Repository repositoryWithComponents(int componentCount) {
    Repository repository = tempEntity.newRepository();
    for (int i = 0; i < componentCount; i++) {
      tempEntity.newRepositoryComponent(repository, "hash");
    }
    mockHdsResponse(componentCount);
    return repository;
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
    setHdsResponseForURI(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH, componentEvaluationDataList, 200);
  }

  private void assertRepositoryEvaluationData(AuditDTO auditDTO, int componentCount, String evaluationCause) {
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "evaluationCause", evaluationCause);
  }
}
