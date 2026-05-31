/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for repository-scoped container image quarantine methods in PolicyViolationDAO.
 */
@Category(SlowTest.class)
public class PolicyViolationDAOContainerScopingTest
    extends AbstractDbDAOTest
{
  private PolicyViolationDAO dao;

  private OrganizationDAO organizationDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyViolationDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
  }

  @Test
  public void testGetContainerImagesInQuarantineByRepositoryIds_ReturnsOnlyMatchingRepository() {
    // proxy repo has quarantine enabled; hosted does not
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "proxyRepo",
        RepositoryType.proxy, "docker", true);
    Repository hostedRepo = tempEntity.newRepository(repositoryManager, "hostedRepo",
        RepositoryType.hosted, "docker", false);

    // Create organizations for each repository and link to repo
    Organization proxyOrg = tempEntity.newOrganization("proxyOrg");
    proxyOrg.setRelatedRepositoryId(proxyRepo.getId());
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      organizationDAO.update(tx, proxyOrg);
      tx.commit();
    }
    Organization hostedOrg = tempEntity.newOrganization("hostedOrg");
    hostedOrg.setRelatedRepositoryId(hostedRepo.getId());
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      organizationDAO.update(tx, hostedOrg);
      tx.commit();
    }

    // Create applications for each org
    var proxyApp = tempEntity.newApplication("proxyApp", "proxyAppPublicId", proxyOrg.getId());
    var hostedApp = tempEntity.newApplication("hostedApp", "hostedAppPublicId", hostedOrg.getId());

    // Create policy evaluations
    PolicyEvaluation proxyEval = tempEntity.newPolicyEvaluation(proxyApp.getId(), ProxyStageType.ID, "scan1");
    PolicyEvaluation hostedEval = tempEntity.newPolicyEvaluation(hostedApp.getId(), ProxyStageType.ID, "scan2");

    // Create policies
    Policy policy = tempEntity.newPolicy(proxyApp);

    // Create violations - only proxy should be quarantined (action=fail)
    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "constraintData");
    PolicyViolation proxyViolation = new PolicyViolation(proxyEval, policy.getId(), policy.getName(),
        5, PolicyThreatCategory.SECURITY, "hash1", componentId, List.of(constraintFact), "file1");
    proxyViolation.setActionTypeId(Action.ID_FAIL);
    dao.insert(proxyViolation);

    PolicyViolation hostedViolation = new PolicyViolation(hostedEval, policy.getId(), policy.getName(),
        5, PolicyThreatCategory.SECURITY, "hash2", componentId, List.of(constraintFact), "file2");
    hostedViolation.setActionTypeId(Action.ID_FAIL);
    dao.insert(hostedViolation);

    // Query for only the proxy repository
    Set<String> repositoryIds = Set.of(proxyRepo.getId());
    List<PolicyViolationDAO.ContainerImageInQuarantineData> results =
        dao.getContainerImagesInQuarantineByRepositoryIds(repositoryIds, 1, 10);

    // Should only return the proxy repo's container
    assertThat(results).hasSize(1);
    assertThat(results.get(0).repositoryId()).isEqualTo(proxyRepo.getId());
  }

  @Test
  public void testGetContainerImagesInQuarantineByRepositoryIds_EmptySet_ReturnsEmpty() {
    List<PolicyViolationDAO.ContainerImageInQuarantineData> results =
        dao.getContainerImagesInQuarantineByRepositoryIds(Set.of(), 1, 10);
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetContainerImagesInQuarantineByRepositoryIds_PaginationPage2_ReturnsEmpty() {
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "proxyPagRepo",
        RepositoryType.proxy, "docker", true);

    Organization proxyOrg = tempEntity.newOrganization("proxyPagOrg");
    proxyOrg.setRelatedRepositoryId(proxyRepo.getId());
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      organizationDAO.update(tx, proxyOrg);
      tx.commit();
    }

    var proxyApp = tempEntity.newApplication("proxyPagApp", "proxyPagAppPublicId", proxyOrg.getId());
    PolicyEvaluation proxyEval = tempEntity.newPolicyEvaluation(proxyApp.getId(), ProxyStageType.ID, "pagScan1");
    Policy policy = tempEntity.newPolicy(proxyApp);

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "constraintData");
    PolicyViolation violation = new PolicyViolation(proxyEval, policy.getId(), policy.getName(),
        5, PolicyThreatCategory.SECURITY, "pagHash1", componentId, List.of(constraintFact), "file1");
    violation.setActionTypeId(Action.ID_FAIL);
    dao.insert(violation);

    // Page 1 returns the single result; page 2 returns empty
    List<PolicyViolationDAO.ContainerImageInQuarantineData> page1 =
        dao.getContainerImagesInQuarantineByRepositoryIds(Set.of(proxyRepo.getId()), 1, 1);
    List<PolicyViolationDAO.ContainerImageInQuarantineData> page2 =
        dao.getContainerImagesInQuarantineByRepositoryIds(Set.of(proxyRepo.getId()), 2, 1);

    assertThat(page1).hasSize(1);
    assertThat(page2).isEmpty();
  }

  @Test
  public void testGetContainerImagesQuarantinedCountByRepositoryIds_ReturnsCorrectCount() {
    // proxy repo has quarantine enabled; hosted does not
    Repository proxyRepo = tempEntity.newRepository(repositoryManager, "proxyRepo",
        RepositoryType.proxy, "docker", true);
    Repository hostedRepo = tempEntity.newRepository(repositoryManager, "hostedRepo",
        RepositoryType.hosted, "docker", false);

    // Create organizations for each repository and link to repo
    Organization proxyOrg = tempEntity.newOrganization("proxyOrg");
    proxyOrg.setRelatedRepositoryId(proxyRepo.getId());
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      organizationDAO.update(tx, proxyOrg);
      tx.commit();
    }
    Organization hostedOrg = tempEntity.newOrganization("hostedOrg");
    hostedOrg.setRelatedRepositoryId(hostedRepo.getId());
    try (TransactionContext tx = organizationDAO.createTransactionContext()) {
      tx.begin();
      organizationDAO.update(tx, hostedOrg);
      tx.commit();
    }

    // Create applications for each org
    var proxyApp = tempEntity.newApplication("proxyApp", "proxyAppPublicId", proxyOrg.getId());
    var hostedApp = tempEntity.newApplication("hostedApp", "hostedAppPublicId", hostedOrg.getId());

    // Create policy evaluations
    PolicyEvaluation proxyEval = tempEntity.newPolicyEvaluation(proxyApp.getId(), ProxyStageType.ID, "scan1");
    PolicyEvaluation hostedEval = tempEntity.newPolicyEvaluation(hostedApp.getId(), ProxyStageType.ID, "scan2");

    // Create policies
    Policy policy = tempEntity.newPolicy(proxyApp);

    // Create violations - only proxy should be quarantined (action=fail)
    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ConstraintFact constraintFact = new ConstraintFact("constraintId", "constraintName", "constraintData");
    PolicyViolation proxyViolation = new PolicyViolation(proxyEval, policy.getId(), policy.getName(),
        5, PolicyThreatCategory.SECURITY, "hash1", componentId, List.of(constraintFact), "file1");
    proxyViolation.setActionTypeId(Action.ID_FAIL);
    dao.insert(proxyViolation);

    PolicyViolation hostedViolation = new PolicyViolation(hostedEval, policy.getId(), policy.getName(),
        5, PolicyThreatCategory.SECURITY, "hash2", componentId, List.of(constraintFact), "file2");
    hostedViolation.setActionTypeId(Action.ID_FAIL);
    dao.insert(hostedViolation);

    // Query for only the proxy repository
    Set<String> repositoryIds = Set.of(proxyRepo.getId());
    long count = dao.getContainerImagesQuarantinedCountByRepositoryIds(repositoryIds);

    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testGetContainerImagesQuarantinedCountByRepositoryIds_EmptySet_ReturnsZero() {
    long count = dao.getContainerImagesQuarantinedCountByRepositoryIds(Set.of());
    assertThat(count).isZero();
  }
}
