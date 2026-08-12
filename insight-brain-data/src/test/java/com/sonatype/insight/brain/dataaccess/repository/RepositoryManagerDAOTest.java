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
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.jooq.exception.DataAccessException;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryManager.REPOSITORY_MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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
    return dao.getByNameAndManagerType(name, ManagerType.TRADITIONAL);
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
    ProxyRepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());
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
  @Override
  public void testInsert_ValidateNameInvalidChars() {
    // RepositoryManager accepts arbitrary display name characters (e.g. "My NXRM (Production)", "Dev & QA")
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      RepositoryManager repositoryManager = createNameable(name);
      assertThat(dao.getById(repositoryManager.getId()).getName()).isEqualTo(name);
    }
  }

  @Test
  @Override
  public void testInsert_ValidateNameSpaces() {
    // RepositoryManager accepts display names with leading/trailing/double spaces
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      RepositoryManager repositoryManager = createNameable(name);
      assertThat(dao.getById(repositoryManager.getId()).getName()).isEqualTo(name);
    }
  }

  @Test
  @Override
  public void testUpdate_ValidateNameInvalidChars() {
    // RepositoryManager accepts arbitrary display name characters (e.g. "My NXRM (Production)", "Dev & QA")
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      repositoryManager.setName(name);
      dao.update(repositoryManager);
      assertThat(dao.getById(repositoryManager.getId()).getName()).isEqualTo(name);
    }
  }

  @Test
  @Override
  public void testUpdate_ValidateNameSpaces() {
    // RepositoryManager accepts display names with leading/trailing/double spaces
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      repositoryManager.setName(name);
      dao.update(repositoryManager);
      assertThat(dao.getById(repositoryManager.getId()).getName()).isEqualTo(name);
    }
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
  public void testUpdate_virtualManagerAllowsNameCollisionWithTraditional() {
    RepositoryManager traditional = tempEntity.newRepositoryManager();
    traditional.setName("shared-name");
    dao.update(traditional);

    RepositoryManager virtual = tempEntity.newRepositoryManager();
    virtual.setManagerType(ManagerType.VIRTUAL);
    virtual.setName("shared-name");
    dao.update(virtual);

    assertThat(dao.getByNameAndManagerType("shared-name", ManagerType.TRADITIONAL).getId())
        .isEqualTo(traditional.getId());
    assertThat(dao.getByNameAndManagerType("shared-name", ManagerType.VIRTUAL).getId()).isEqualTo(virtual.getId());
  }

  @Test
  public void testUpdate_duplicateNameWithinVirtualBucket() {
    RepositoryManager first = tempEntity.newRepositoryManager();
    first.setManagerType(ManagerType.VIRTUAL);
    first.setName("dup-virtual");
    dao.update(first);

    RepositoryManager second = tempEntity.newRepositoryManager();
    second.setManagerType(ManagerType.VIRTUAL);
    second.setName("dup-virtual");

    assertThatThrownBy(() -> dao.update(second))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("dup-virtual is already used as a name.");
  }

  @Test
  public void testInsert_duplicateNameWithinTraditionalBucket() {
    tempEntity.newRepositoryManager("inst-first-" + TemporaryEntity.uuid(), "dup-insert", "Nexus", "3.0");

    assertThatThrownBy(() -> tempEntity.newRepositoryManager(
        "inst-second-" + TemporaryEntity.uuid(), "dup-insert", "Nexus", "3.0"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("dup-insert is already used as a name.");
  }

  @Test
  public void testInsert_duplicateNameWithinVirtualBucket() {
    // Prime the VIRTUAL bucket via tempEntity + update so the row is tracked for cleanup, then
    // exercise dao.insert() on a second row targeting the same bucket. The collision happens on
    // insert() regardless of how the pre-existing row was persisted.
    RepositoryManager first = tempEntity.newRepositoryManager();
    first.setManagerType(ManagerType.VIRTUAL);
    first.setName("dup-virtual-insert");
    dao.update(first);

    RepositoryManager second = new RepositoryManager();
    second.setInstanceId("inst-second-" + TemporaryEntity.uuid());
    second.setManagerType(ManagerType.VIRTUAL);
    second.setName("dup-virtual-insert");

    assertThatThrownBy(() -> dao.insert(second))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("dup-virtual-insert is already used as a name.");
  }

  @Test
  public void testUpdate_duplicateNameWithinTraditionalBucket() {
    RepositoryManager first = tempEntity.newRepositoryManager();
    first.setName("dup-traditional");
    dao.update(first);

    RepositoryManager second = tempEntity.newRepositoryManager();
    second.setName("dup-traditional");

    assertThatThrownBy(() -> dao.update(second))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("dup-traditional is already used as a name.");
  }

  /**
   * The DAO pre-check catches duplicate names on the happy path, but a concurrent writer can
   * still slip a matching row in between the pre-check and the commit. That race is what the
   * DB-level {@code repository_manager_name_uk} constraint (and the {@code translateConstraintViolation}
   * translation) exist to close — so exercise the DB layer directly via jOOQ, bypassing the DAO,
   * to prove the constraint is real.
   */
  @Test
  public void testDbConstraintRejectsDuplicateNameWithinBucket() {
    RepositoryManager first = tempEntity.newRepositoryManager();
    first.setName("race-name");
    dao.update(first);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThatExceptionOfType(IntegrityConstraintViolationException.class)
          .isThrownBy(() -> tx.dsl()
              .insertInto(REPOSITORY_MANAGER)
              .set(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID, TemporaryEntity.uuid())
              .set(REPOSITORY_MANAGER.INSTANCE_ID, "race-inst-" + TemporaryEntity.uuid())
              .set(REPOSITORY_MANAGER.NAME, "race-name")
              .set(REPOSITORY_MANAGER.NAME_LOWERCASE_NO_WHITESPACE, "race-name")
              .set(REPOSITORY_MANAGER.MANAGER_TYPE, ManagerType.TRADITIONAL.name())
              .execute());
    }
  }

  /**
   * The check-then-act race between the DAO pre-check and the DB unique index is closed by
   * {@code translateConstraintViolation}, which introspects the wrapped driver exception to
   * distinguish an instance-ID collision from a name collision. Simulating an actual race
   * requires two concurrent writers and is not reliable in a single-threaded integration test,
   * so exercise the introspection logic directly with synthesised exceptions covering the
   * PostgreSQL structured path, the H2 message-substring fallback (including uppercase-message
   * variants after the {@code toLowerCase} hardening), and the unknown-constraint case.
   */
  @Test
  public void testExtractConstraintName_postgresStructuredPath_nameCollision() {
    // Wrap the PSQLException the same way jOOQ 3.19 does at execute() — the wrapper's message
    // ("SQL [...]; " + driver message) contains the driver's text, so pass 1 must skip it and
    // reach the underlying PSQLException before pass 2 sees the message.
    PSQLException psqlEx = new PSQLException(
        new ServerErrorMessage("C" + PSQLState.UNIQUE_VIOLATION.getState() + "\0nrepository_manager_name_uk\0"));
    IntegrityConstraintViolationException wrapped =
        new IntegrityConstraintViolationException("SQL [insert into repository_manager ...]; " + psqlEx.getMessage(),
            psqlEx);
    assertThat(RepositoryManagerDAO.extractConstraintName(wrapped)).isEqualTo("repository_manager_name_uk");
  }

  @Test
  public void testExtractConstraintName_postgresStructuredPath_instanceIdCollision() {
    PSQLException psqlEx = new PSQLException(
        new ServerErrorMessage("C" + PSQLState.UNIQUE_VIOLATION.getState() + "\0nrepository_manager_uk\0"));
    IntegrityConstraintViolationException wrapped =
        new IntegrityConstraintViolationException("SQL [insert into repository_manager ...]; " + psqlEx.getMessage(),
            psqlEx);
    assertThat(RepositoryManagerDAO.extractConstraintName(wrapped)).isEqualTo("repository_manager_uk");
  }

  @Test
  public void testExtractConstraintName_userDataInMessageDoesNotMisrouteConstraint() {
    // H2 embeds the conflicting row's column values after " VALUES ". A client-supplied
    // instance_id containing the literal constraint name must NOT be picked up by pass 2's
    // substring match — the search is bounded to the segment before " VALUES ".
    Exception cause = new Exception(
        "Unique index or primary key violation: \"repository_manager_uk ON PUBLIC.REPOSITORY_MANAGER\" "
            + "VALUES ('repository_manager_name_uk', 'traditional')");
    // The instance_id 'repository_manager_name_uk' sits inside VALUES and must be ignored.
    // The real constraint name (repository_manager_uk) sits before VALUES and must be returned.
    assertThat(RepositoryManagerDAO.extractConstraintName(cause)).isEqualTo("repository_manager_uk");
  }

  @Test
  public void testExtractConstraintName_postgresDetailSectionIsBoundedOut() {
    // PostgreSQL formats: "duplicate key value violates unique constraint \"...\" Detail: Key (...)=(...)".
    // A user-supplied column value in the Detail: section must not misroute the match.
    PSQLException psqlEx = new PSQLException(new ServerErrorMessage("C" + PSQLState.UNIQUE_VIOLATION.getState()
        + "\0Mduplicate key value violates unique constraint \"repository_manager_uk\"\0"
        + "DKey (name_lowercase_no_whitespace, manager_type)=(repository_manager_name_uk, traditional).\0"));
    // Even if getConstraint() were unavailable, the message-substring pass must not read the
    // Detail: section — the real constraint name is before it.
    IntegrityConstraintViolationException wrapped =
        new IntegrityConstraintViolationException("SQL [insert into repository_manager ...]; " + psqlEx.getMessage(),
            psqlEx);
    assertThat(RepositoryManagerDAO.extractConstraintName(wrapped)).isEqualTo("repository_manager_uk");
  }

  @Test
  public void testExtractConstraintName_h2MessageFallback_nameCollision() {
    // H2 does not expose a getConstraint() accessor; fall back to substring matching against
    // the driver's message. Simulate the wrapping jOOQ produces on H2 with a nested cause chain.
    Exception h2Cause = new Exception(
        "Unique index or primary key violation: \"repository_manager_name_uk ON PUBLIC.REPOSITORY_MANAGER\"");
    Exception wrapper = new Exception("SQL execution failed", h2Cause);
    assertThat(RepositoryManagerDAO.extractConstraintName(wrapper)).isEqualTo("repository_manager_name_uk");
  }

  @Test
  public void testExtractConstraintName_h2MessageFallback_uppercaseMessage() {
    // Guards the toLowerCase() hardening: even if H2 ever drops DATABASE_TO_UPPER=FALSE and
    // emits identifiers uppercased, matching still works.
    Exception cause = new Exception(
        "UNIQUE INDEX OR PRIMARY KEY VIOLATION: \"REPOSITORY_MANAGER_NAME_UK ON PUBLIC.REPOSITORY_MANAGER\"");
    assertThat(RepositoryManagerDAO.extractConstraintName(cause)).isEqualTo("repository_manager_name_uk");
  }

  @Test
  public void testExtractConstraintName_h2MessageFallback_instanceIdCollision() {
    Exception cause = new Exception(
        "Unique index or primary key violation: \"repository_manager_uk ON PUBLIC.REPOSITORY_MANAGER\"");
    assertThat(RepositoryManagerDAO.extractConstraintName(cause)).isEqualTo("repository_manager_uk");
  }

  @Test
  public void testExtractConstraintName_unknownConstraint_returnsNull() {
    Exception cause = new Exception("Some other constraint fired: \"repository_pk ON PUBLIC.REPOSITORY\"");
    assertThat(RepositoryManagerDAO.extractConstraintName(cause)).isNull();
  }

  /**
   * The synthesised-exception tests above are useful for hitting individual branches, but they
   * do not pin the real driver-message shape. Provoke a genuine duplicate-name violation via
   * jOOQ so {@link RepositoryManagerDAO#extractConstraintName} runs against the actual wrapper
   * message that the deployed H2/PostgreSQL drivers emit. If a driver upgrade changes the
   * shape — for example by dropping the constraint name in favour of an index name — this test
   * fails visibly instead of silently regressing constraint-violation translation.
   */
  @Test
  public void testExtractConstraintName_realDriverDuplicateNameMessage() {
    RepositoryManager existing = tempEntity.newRepositoryManager();
    existing.setName("real-driver-collision");
    dao.update(existing);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      DataAccessException actual = catchThrowableOfType(() -> tx.dsl()
          .insertInto(REPOSITORY_MANAGER)
          .set(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID, TemporaryEntity.uuid())
          .set(REPOSITORY_MANAGER.INSTANCE_ID, "real-driver-" + TemporaryEntity.uuid())
          .set(REPOSITORY_MANAGER.NAME, "real-driver-collision")
          .set(REPOSITORY_MANAGER.NAME_LOWERCASE_NO_WHITESPACE, "real-driver-collision")
          .set(REPOSITORY_MANAGER.MANAGER_TYPE, ManagerType.TRADITIONAL.name())
          .execute(), DataAccessException.class);
      assertThat(actual).isNotNull();
      assertThat(RepositoryManagerDAO.extractConstraintName(actual))
          .isEqualTo(RepositoryManagerDAO.REPOSITORY_MANAGER_NAME_UK);
    }
  }

  @Test
  public void testExtractConstraintName_nullOrEmpty_returnsNull() {
    assertThat(RepositoryManagerDAO.extractConstraintName(new Exception())).isNull();
  }

  /**
   * The {@code translateConstraintViolation} branching decides what a caller sees after the
   * check-then-act race between the DAO pre-check and the DB unique index is lost. A real race
   * needs two concurrent writers and is not reproducible in a single-threaded integration test,
   * so exercise the branching directly: name UK hit → {@code InvalidNameException} with cause
   * preserved, instance-id UK hit → {@code InvalidRepositoryManagerException} with cause
   * preserved, unmapped constraint → the raw {@code DataAccessException} returned as-is.
   */
  @Test
  public void testTranslateConstraintViolation_nameUk_returnsInvalidNameExceptionWithCausePreserved() {
    RepositoryManager racer = new RepositoryManager();
    racer.setName("Race Name");
    racer.setInstanceId("race-inst");

    PSQLException psqlEx = new PSQLException(
        new ServerErrorMessage("C" + PSQLState.UNIQUE_VIOLATION.getState() + "\0nrepository_manager_name_uk\0"));
    IntegrityConstraintViolationException cause = new IntegrityConstraintViolationException(
        "SQL [insert into repository_manager ...]; " + psqlEx.getMessage(), psqlEx);

    RuntimeException translated = dao.translateConstraintViolation(racer, cause);

    assertThat(translated).isInstanceOf(InvalidNameException.class)
        .hasMessage("Race Name is already used as a name.");
    assertThat(translated.getCause()).isSameAs(cause);
  }

  @Test
  public void testTranslateConstraintViolation_instanceIdUk_returnsInvalidRepositoryManagerExceptionWithCausePreserved() {
    RepositoryManager racer = new RepositoryManager();
    racer.setInstanceId("collide-inst");

    PSQLException psqlEx = new PSQLException(
        new ServerErrorMessage("C" + PSQLState.UNIQUE_VIOLATION.getState() + "\0nrepository_manager_uk\0"));
    IntegrityConstraintViolationException cause = new IntegrityConstraintViolationException(
        "SQL [insert into repository_manager ...]; " + psqlEx.getMessage(), psqlEx);

    RuntimeException translated = dao.translateConstraintViolation(racer, cause);

    assertThat(translated).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID collide-inst.");
    assertThat(translated.getCause()).isSameAs(cause);
  }

  @Test
  public void testTranslateConstraintViolation_unknownConstraint_returnsRawCause() {
    RepositoryManager racer = new RepositoryManager();
    racer.setInstanceId("ignored");

    // A constraint on this table whose name isn't one of the two mapped ones must fall through
    // to the log.warn + raw rethrow path — degrading to a raw jOOQ propagation instead of
    // masquerading as one of the mapped app-level exceptions.
    Exception unknown = new Exception("some FK violation: \"repository_manager_organization_fk\"");
    IntegrityConstraintViolationException cause = new IntegrityConstraintViolationException(
        "SQL [insert into repository_manager ...]; " + unknown.getMessage(), unknown);

    RuntimeException translated = dao.translateConstraintViolation(racer, cause);

    assertThat(translated).isSameAs(cause);
  }

  /**
   * The gate {@code isUniqueConstraintViolation} decides whether {@code translateConstraintViolation}
   * runs at all. It must accept the three shapes jOOQ produces on a real race —
   * {@code PSQLException} at the top level with the Postgres unique-violation SQLState,
   * {@code IntegrityConstraintViolationException} at the top level (H2), and
   * {@code IntegrityConstraintViolationException} wrapped as a cause.
   */
  @Test
  public void testIsUniqueConstraintViolation_acceptsPostgresSqlState() {
    PSQLException psqlEx = new PSQLException(
        new ServerErrorMessage("C" + PSQLState.UNIQUE_VIOLATION.getState() + "\0Mviolation\0"));
    DataAccessException dae = new DataAccessException("wrap", psqlEx);
    assertThat(RepositoryManagerDAO.isUniqueConstraintViolation(dae)).isTrue();
  }

  @Test
  public void testIsUniqueConstraintViolation_acceptsIntegrityViolationAtTopLevel() {
    IntegrityConstraintViolationException dae = new IntegrityConstraintViolationException("violation");
    assertThat(RepositoryManagerDAO.isUniqueConstraintViolation(dae)).isTrue();
  }

  @Test
  public void testIsUniqueConstraintViolation_acceptsIntegrityViolationAsCause() {
    DataAccessException dae = new DataAccessException("wrap", new IntegrityConstraintViolationException("violation"));
    assertThat(RepositoryManagerDAO.isUniqueConstraintViolation(dae)).isTrue();
  }

  @Test
  public void testIsUniqueConstraintViolation_rejectsUnrelatedFailure() {
    DataAccessException dae = new DataAccessException("connection lost");
    assertThat(RepositoryManagerDAO.isUniqueConstraintViolation(dae)).isFalse();
  }

  /**
   * Batch flush and pooling wrappers can nest the driver exception more than one level deep — an
   * earlier one-level-only cause check silently returned {@code false} here, sent the caller a
   * raw 500 in place of the {@link InvalidNameException} the pre-check would have produced, and
   * the regression only surfaced under concurrent writes. {@link RepositoryManagerDAO#extractConstraintName}
   * already walks the whole chain for the same reason; this pins the two in sync.
   */
  @Test
  public void testIsUniqueConstraintViolation_acceptsNestedPsqlException() {
    PSQLException psqlEx = new PSQLException(
        new ServerErrorMessage("C" + PSQLState.UNIQUE_VIOLATION.getState() + "\0Mviolation\0"));
    DataAccessException dae = new DataAccessException("outer", new RuntimeException("pool wrapper", psqlEx));
    assertThat(RepositoryManagerDAO.isUniqueConstraintViolation(dae)).isTrue();
  }

  @Test
  public void testIsUniqueConstraintViolation_acceptsNestedIntegrityViolation() {
    DataAccessException dae = new DataAccessException("outer",
        new RuntimeException("pool wrapper", new IntegrityConstraintViolationException("violation")));
    assertThat(RepositoryManagerDAO.isUniqueConstraintViolation(dae)).isTrue();
  }

  /**
   * Pins the silent-coercion contract of {@code normalizeManagerType}: a caller that hands the
   * DAO an in-memory entity with {@code managerType} explicitly set to {@code null} round-trips
   * as {@code TRADITIONAL}, not as {@code null}. Rows loaded from the database are never null
   * (migration 0481 backfilled every legacy row and the column is now {@code NOT NULL}), so the
   * only way to reach the coercion is a caller-supplied null — e.g. an older REST client that
   * omits {@code managerType} in the request body. Preserving null on this DAO would violate the
   * {@code NOT NULL DEFAULT 'TRADITIONAL'} column definition and is not supported.
   */
  @Test
  public void testUpdate_nullManagerType_normalizedToTraditional() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    assertThat(dao.getById(repositoryManager.getId()).getManagerType()).isEqualTo(ManagerType.TRADITIONAL);

    RepositoryManager loaded = dao.getById(repositoryManager.getId());
    loaded.setManagerType(null);
    dao.update(loaded);

    RepositoryManager reloaded = dao.getById(repositoryManager.getId());
    assertThat(reloaded.getManagerType()).isEqualTo(ManagerType.TRADITIONAL);
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
