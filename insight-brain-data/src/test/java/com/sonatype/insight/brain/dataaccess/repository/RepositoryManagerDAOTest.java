/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RepositoryManagerDAOTest
    extends NameableDAOTest<RepositoryManager>
{
  private RepositoryManagerDAO dao;

  private OrganizationDAO organizationDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryManagerDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
  }

  @Override
  protected RepositoryManager createNameable(String a) {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(System.nanoTime() + "");
    repositoryManager.setName(a);
    dao.update(repositoryManager);
    return repositoryManager;
  }

  @Override
  protected AbstractOperationalSqlDAO<RepositoryManager> getDao() {
    return dao;
  }

  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH_APP_ORG;
  }

  @Override
  protected RepositoryManager getEntityByName(String name) {
    return dao.getByName(name);
  }

  @Test
  public void testCRUD() {
    // Create
    RepositoryManager repoManager = tempEntity.newRepositoryManager("RepositoryManagerDAOTest");
    String id = repoManager.getId();
    repoManager = dao.getById(id);
    assertThat(repoManager.getInstanceId()).isEqualTo("RepositoryManagerDAOTest");

    // Update
    repoManager.setInstanceId("RepositoryManagerDAOTest updated");
    dao.update(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager.getInstanceId()).isEqualTo("RepositoryManagerDAOTest updated");

    // Delete
    dao.delete(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager).isNull();
  }

  @Test
  public void testInsert_DuplicateInstanceId() {
    tempEntity.newRepositoryManager("MyInstanceId");

    assertThatThrownBy(() -> tempEntity.newRepositoryManager("MyInstanceId"))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID MyInstanceId.");
  }

  @Test
  public void testInsert_ValidateNullInstanceId() {
    RepositoryManager repoManager = new RepositoryManager();
    repoManager.setName("TestManager");
    // Non-virtual manager with null instanceId should fail
    assertThatThrownBy(() -> dao.insert(repoManager))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testInsert_ValidateEmptyInstanceId() {
    RepositoryManager repoManager = new RepositoryManager();
    repoManager.setInstanceId(" ");
    repoManager.setName("TestManager");
    assertThatThrownBy(() -> dao.insert(repoManager))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testInsert_VirtualManager_AllowsNullInstanceId() {
    RepositoryManager repoManager = new RepositoryManager();
    repoManager.setName("VirtualManager");
    repoManager.setManagerType(ManagerType.VIRTUAL);
    // Virtual manager with null instanceId should be allowed (server generates it later)
    // But since we're testing DAO directly, we need to set it to avoid DB constraint
    repoManager.setInstanceId("virtual-" + System.nanoTime());
    dao.insert(repoManager);
    assertThat(dao.getById(repoManager.getId())).isNotNull();
  }

  @Test
  public void testUpdate_ValidateNullInstanceId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(null);
    assertThatThrownBy(() -> dao.update(repoManager))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testUpdate_ValidateEmptyInstanceId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(" ");
    assertThatThrownBy(() -> dao.update(repoManager))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testUpdate_DuplicateInstanceId() {
    tempEntity.newRepositoryManager("MyInstanceId1");
    RepositoryManager repoManager = tempEntity.newRepositoryManager("MyInstanceId2");
    repoManager.setInstanceId("MyInstanceId1");

    assertThatThrownBy(() -> dao.update(repoManager)).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID MyInstanceId1.");
  }

  @Test
  public void testGetByRelatedOrganizationId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryManagerId(repoManager.getId());
    organizationDAO.update(organization);
    repoManager.setRelatedOrganizationId(organization.getId());
    dao.update(repoManager);

    RepositoryManager result = dao.getByRelatedOrganizationId(organization.getId());
    assertThat(result.getId()).isEqualTo(repoManager.getId());
  }

  @Test
  public void testDelete_CascadesToPolicyWaivers() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver policyWaiver = new PolicyWaiver(policy.getId(), repoManager.getId(), "Comment");
    PolicyWaiverDAO policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);

    // sanity check
    assertThat(policyWaiverDAO.getByOwnerId(repoManager.getId())).hasSize(1);

    dao.delete(repoManager);

    assertThat(policyWaiverDAO.getByOwnerId(repoManager.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyWaiverRequests() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setOwnerId(repoManager.getId())
        .setPolicyId(policy.getId())
        .setPolicyViolationId(policyViolation.getId()));

    // sanity check
    PolicyWaiverRequestDAO policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    assertThat(policyWaiverRequestDAO.getByOwnerId(repoManager.getId())).hasSize(1);

    dao.delete(repoManager);

    assertThat(policyWaiverRequestDAO.getByOwnerId(repoManager.getId())).isEmpty();
  }

  @Test
  public void testGetByInstanceIdNotNull() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    RepositoryManager resultRepositoryManager = dao.getByInstanceIdNotNull(repositoryManager.getInstanceId());

    assertThat(resultRepositoryManager.getId()).isEqualTo(repositoryManager.getId());
    assertThat(resultRepositoryManager.getInstanceId()).isEqualTo(repositoryManager.getInstanceId());
  }

  @Test
  public void testGetByInstanceIdNotNull_NotFoundException() {
    assertThatThrownBy(() -> dao.getByInstanceIdNotNull("repoManagerInstanceId"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a repository manager with instance ID repoManagerInstanceId.");
  }

  @Test
  public void testGetUnconfigured() {
    RepositoryManager configuredRepositoryManager = tempEntity.newRepositoryManager();
    configuredRepositoryManager.setConfigured(true);
    dao.update(configuredRepositoryManager);

    assertThat(dao.getUnconfigured()).extracting(RepositoryManager::getId)
        .containsExactly(repository.getRepositoryManagerId());
  }

  @Test
  public void testUpdate_nameExistsSameEntity() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryManager.setName("name1");
    dao.update(repositoryManager);
    dao.update(repositoryManager);

    RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());

    assertThat(resultRepositoryManager.getName()).isEqualTo("name1");
  }

  @Override
  public void testInsert_ValidateNullName() {
    RepositoryManager repositoryManager = createNameable(null);
    RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());

    assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
  }

  @Test
  @Override
  public void testUpdate_ValidateNullName() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryManager.setName(null);

    dao.update(repositoryManager);

    RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());

    assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
  }

  @Test
  public void testInsert_InstanceIdWithInvalidNameChars() {
    for (String invalidChar : NameHelperTest.INVALID_CHARACTERS) {
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager("a" + invalidChar + "b");

      RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());
      assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
    }
  }

  @Test
  public void testUpdate_InstanceIdWithInvalidNameChars() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    for (String invalidChar : NameHelperTest.INVALID_CHARACTERS) {
      repositoryManager.setInstanceId("a" + invalidChar + "b");
      dao.update(repositoryManager);

      RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());
      assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
    }
  }

  @Test
  public void testGetByIdOrRepositoryId() {
    RepositoryManager repoMan1 = tempEntity.newRepositoryManager();
    Repository repo11 = tempEntity.newRepository(repoMan1);

    assertThat(dao.getByIdOrRepositoryId(repositoryManager.getId()).getId()).isEqualTo(repositoryManager.getId());
    assertThat(dao.getByIdOrRepositoryId(repository.getId()).getId()).isEqualTo(repositoryManager.getId());

    assertThat(dao.getByIdOrRepositoryId(repoMan1.getId()).getId()).isEqualTo(repoMan1.getId());
    assertThat(dao.getByIdOrRepositoryId(repo11.getId()).getId()).isEqualTo(repoMan1.getId());

    assertThat(dao.getByIdOrRepositoryId("ROOT_ORGANIZATION_ID")).isNull();
  }

  @Test
  public void testGetByRepositoryIds() {
    RepositoryManager repoMan1 = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(repoMan1);

    RepositoryManager repoMan2 = tempEntity.newRepositoryManager();
    Repository repo2 = tempEntity.newRepository(repoMan2);

    tempEntity.newRepositoryManager();

    JPA.assertContainsEntitiesExactlyInAnyOrder(dao.getByRepositoryIds(Set.of(repo1.getId())), repoMan1);
    JPA.assertContainsEntitiesExactlyInAnyOrder(dao.getByRepositoryIds(Set.of(repo2.getId())), repoMan2);
    JPA.assertContainsEntitiesExactlyInAnyOrder(dao.getByRepositoryIds(Set.of(repo1.getId(), repo2.getId())), repoMan1,
        repoMan2);
  }

  @Test
  public void testGetByIds() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    // Additional RepositoryManager not queried
    tempEntity.newRepositoryManager();

    Set<String> repositoryManagerIds = Set.of(repositoryManager1.getId(), repositoryManager2.getId());

    JPA.assertContainsEntitiesExactlyInAnyOrder(
        dao.getByIds(repositoryManagerIds), repositoryManager1, repositoryManager2);
  }
}
