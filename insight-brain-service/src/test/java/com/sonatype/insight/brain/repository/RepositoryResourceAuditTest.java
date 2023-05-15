/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class RepositoryResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "hash";

  private static final String PATHNAME = "pathname";

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

    AuditDTO initiateEvaluateRepository = assertAuditLog(AuditEvent.INITIATE_EVALUATE_REPOSITORY, null);
    assertRepositoryEvaluationData(initiateEvaluateRepository, 101,
        RepositoryComponentEvaluationDataRequestList.REEVALUATION);

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.EVALUATE_REPOSITORY, 2, null);
    auditDTOs.forEach(auditDTO -> assertRepositoryData(auditDTO, repository));
    auditDTOs.sort(Comparator.comparing(dto -> (Integer) dto.data.get("componentCount")));
    assertRepositoryEvaluationData(auditDTOs.get(0), 1, RepositoryComponentEvaluationDataRequestList.REEVALUATION);
    assertRepositoryEvaluationData(auditDTOs.get(1), 100, RepositoryComponentEvaluationDataRequestList.REEVALUATION);
  }

  @Test
  public void testReevaluateRepository_Unauthorized() throws Exception {
    Repository repository = repositoryWithComponents(1);

    evaluateRequest(repository.getId()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.INITIATE_EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testReevaluateComponent() throws Exception {
    Repository repository = repositoryWithComponents(1);
    reevaluateRequest(repository.getId(), COMPONENT_HASH).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, 1, "reevaluation");
  }

  @Test
  public void testReevaluateComponent_Unauthorized() throws Exception {
    Repository repository = repositoryWithComponents(1);
    reevaluateRequest(repository.getId(), COMPONENT_HASH).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testUnquarantineComponent() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity
        .newRepositoryComponent(repository.getId(), PATHNAME, new Date(), null);
    mockHdsResponse(1);

    unquarantineRequest(repository.getId(), repositoryComponent.getPathname()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, null);
    assertRepositoryData(auditDTO, repository);
    assertUnquarantineData(auditDTO, repositoryComponent.getHash(), repositoryComponent.getPathname());
  }

  @Test
  public void testUnquarantineComponent_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();

    unquarantineRequest(repository.getId(), PATHNAME).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.RELEASE_QUARANTINE, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetPolicyViolations() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "testPath");

    policyViolationsRequest(repository.getId(), repositoryComponent.getPathname()).get();
    assertComponentData(repository, repositoryComponent.getComponentIdentifier(), repositoryComponent.getHash(),
        repositoryComponent.getPathname());
  }

  @Test
  public void testGetPolicyViolations_ComponentDoesNotExist() throws Exception {
    Repository repository = tempEntity.newRepository();
    policyViolationsRequest(repository.getId(), "non-existent/path").get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "not-found");
    assertCustomData(auditDTO, "componentPathname", "non-existent/path");
  }

  @Test
  public void testGetPolicyViolations_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    policyViolationsRequest(repository.getId(), "a/path").with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetPolicyViolation() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation repositoryPolicyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());

    policyViolationRequest(repository.getId(), repositoryPolicyViolation.getId()).get();
    assertComponentData(repository, repositoryPolicyViolation.getComponentIdentifier(),
        repositoryPolicyViolation.getHash(), repositoryPolicyViolation.getPathname());
  }

  @Test
  public void testGetPolicyViolation_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository();
    policyViolationRequest(repository.getId(), "testRepositoryPolicyViolationId").with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testConfigureRepositories_Unauthorized() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    configureRepositoriesRequest(repositoryManager.getId(), null).with(unauthorizedUser()).put();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY, "unauthorized");
    assertCustomData(auditDTO, "repositoryManagerId", repositoryManager.getId());
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
  }

  @Test
  public void testConfigureRepositories_NotExistingRepositoryManager() throws Exception {
    configureRepositoriesRequest("repositoryManagerId", Collections.singletonList(new Repository())).put();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_REPOSITORY, "not-found");
    assertCustomData(auditDTO, "repositoryManagerId", "repositoryManagerId");
  }

  @Test
  public void testConfigureRepositories_RuntimeException() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository existingRepository1 = tempEntity.newRepository(repositoryManager, "testRepoName1");
    Repository existingRepository2 = tempEntity.newRepository(repositoryManager, "testRepoName2");
    existingRepository1.setPublicId(existingRepository2.getPublicId());
    existingRepository1.setAuditEnabled(false);
    configureRepositoriesRequest(repositoryManager.getId(), Collections.singletonList(existingRepository1)).put();
    List<AuditDTO> auditDTOs = getLogEntries(AuditEvent.CONFIGURE_REPOSITORY);
    for (AuditDTO auditDTO : auditDTOs) {
      assertCustomData(auditDTO, "repositoryManagerId", repositoryManager.getId());
      assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
      if (auditDTO.data.size() > 2) {
        assertStandardData(auditDTO, AuditEvent.CONFIGURE_REPOSITORY, "Error updating repository "
            + existingRepository1.getName() + " (" + existingRepository1.getId() +
            "): There is already a repository with public ID '"
            + existingRepository1.getPublicId() + "' for the same repository manager.");
      }
    }
  }

  @Test
  public void testConfigureRepositories_ExistingRepository() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testRepoMaven", "maven");
    repository.setAuditEnabled(false);
    configureRepositoriesRequest(repositoryManager.getId(), Collections.singletonList(repository)).put();

    List<AuditDTO> auditDTOs = getLogEntries(AuditEvent.CONFIGURE_REPOSITORY);
    for (AuditDTO auditDTO : auditDTOs) {
      assertCustomData(auditDTO, "repositoryManagerId", repositoryManager.getId());
      assertCustomData(auditDTO, "repositoryManagerInstanceId", repositoryManager.getInstanceId());
      if (auditDTO.data.size() == 1) {
        repository = new RepositoryDAO().getByRepositoryManagerInstanceIdAndPublicId(repositoryManager.getInstanceId(),
            repository.getPublicId());
        assertRepositoryData(auditDTO, repository);
      }
    }
  }

  private HttpRequest repositoryRequest(String repositoryId) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.REPOSITORY_PATH)
        .parameter(repositoryId);
  }

  private HttpRequest evaluateRequest(String repositoryId) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_PATH)
        .parameter(repositoryId);
  }

  private HttpRequest reevaluateRequest(String repositoryId, String hash) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.EVALUATE_COMPONENT_PATH)
        .parameter(repositoryId, hash);
  }

  private HttpRequest unquarantineRequest(String repositoryId, String pathname) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.UNQUARANTINE_PATH)
        .parameter(repositoryId, pathname);
  }

  private HttpRequest policyViolationsRequest(String repositoryId, String pathname) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.POLICY_VIOLATIONS_PATH)
        .parameter(repositoryId, pathname);
  }

  private HttpRequest policyViolationRequest(String repositoryId, String repositoryPolicyViolationId) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.POLICY_VIOLATION_PATH)
        .parameter(repositoryId, repositoryPolicyViolationId);
  }

  private HttpRequest configureRepositoriesRequest(String repositoryManagerId, List<Repository> repositories) {
    return restRequest().path(RepositoryResource.RESOURCE_PATH, RepositoryResource.CONFIGURE_REPOSITORIES_PATH)
        .parameter(repositoryManagerId).body(repositories);
  }

  private Repository repositoryWithComponents(int componentCount) {
    Repository repository = tempEntity.newRepository();
    for (int i = 0; i < componentCount; i++) {
      tempEntity.newRepositoryComponent(repository, COMPONENT_HASH);
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
      componentEvaluationData.hash = COMPONENT_HASH;
      componentEvaluationData.matchState = MatchState.EXACT.getId();
      componentEvaluationData.declaredLicenses = Collections.emptySet();
      componentEvaluationData.observedLicenses = Collections.emptySet();
      componentEvaluationDataList.components.add(componentEvaluationData);
    }
    hdsRespondWith(componentEvaluationDataList).atUri(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH);
  }

  private void assertRepositoryEvaluationData(AuditDTO auditDTO, int componentCount, String evaluationCause) {
    assertCustomData(auditDTO, "componentCount", componentCount);
    assertCustomData(auditDTO, "evaluationCause", evaluationCause);
  }

  private void assertUnquarantineData(AuditDTO auditDTO, String componentHash, String componentPathname) {
    assertCustomData(auditDTO, "componentHash", componentHash);
    assertCustomData(auditDTO, "componentPathname", componentPathname);
  }

  protected void assertComponentData(
      Owner owner,
      ComponentIdentifier componentIdentifier,
      String hash,
      String pathname)
  {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOwnerData(auditDTO, owner);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    assertCustomData(auditDTO, "componentHash", hash);
    assertCustomData(auditDTO, "componentPathname", pathname);
  }
}
