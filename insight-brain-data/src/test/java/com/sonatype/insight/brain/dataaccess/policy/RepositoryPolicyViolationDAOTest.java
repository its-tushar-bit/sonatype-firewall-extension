/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetails;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryPolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryPolicyViolationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryPolicyViolationDAO();
  }

  @Test
  public void testCRUD() {
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    // Create
    Date now = new Date();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repository.getId(), "path", now,
        policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier,
        "constraint data");
    assertThat(policyViolation.getId()).isNull();
    dao.insert(policyViolation);
    assertThat(policyViolation.getId()).isNotNull();

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNotNull();
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, now, null /* actionTypeId */,
        policyViolation);

    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNotNull();
    assertPolicyViolation(repository.getId(), "path", policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, now, Action.ID_FAIL, policyViolation);

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation).isNull();
  }

  private void assertPolicyViolation(String repositoryId,
                                     String pathname,
                                     String policyId,
                                     String policyName,
                                     int threatLevel,
                                     PolicyThreatCategory threatCategory,
                                     String hash,
                                     ComponentIdentifier componentIdentifier,
                                     Date time,
                                     String actionTypeId,
                                     RepositoryPolicyViolation actual)
  {
    assertThat(actual.getRepositoryId()).isEqualTo(repositoryId);
    assertThat(actual.getPathname()).isEqualTo(pathname);
    assertThat(actual.getPolicyId()).isEqualTo(policyId);
    assertThat(actual.getPolicyName()).isEqualTo(policyName);
    assertThat(actual.getThreatLevel()).isEqualTo(threatLevel);
    assertThat(actual.getThreatCategory()).isEqualTo(threatCategory);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getTime()).isEqualTo(time);
    assertThat(actual.getActionTypeId()).isEqualTo(actionTypeId);
  }

  @Test
  public void testGetActiveByRepositoryIdAndPathname_OrderByThreatLevelDesc_PolicyId() {
    final String pathname = "pathname";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, pathname, null);

    final String policyIdSecond = "policyId2";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, false, policyIdSecond,
        "policyName2", null);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, pathname, null);

    final List<RepositoryPolicyViolation> violations = dao.getActiveByRepositoryIdAndPathname(repository.getId(),
        pathname);

    int i = 0;
    final RepositoryPolicyViolation firstViolation = violations.get(i++);
    assertThat(firstViolation.getThreatLevel()).isEqualTo(3);
    assertThat(firstViolation.getPolicyId()).isEqualTo("policyId");

    final RepositoryPolicyViolation secondViolation = violations.get(i++);
    assertThat(secondViolation.getThreatLevel()).isEqualTo(3);
    assertThat(secondViolation.getPolicyId()).isEqualTo(policyIdSecond);

    assertThat(violations.get(i++).getThreatLevel()).isEqualTo(2);
    assertThat(violations.get(i).getThreatLevel()).isEqualTo(1);
  }

  @Test
  public void testGetCount() {
    final String pathname = "pathname";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, pathname, null);

    final String policyIdSecond = "policyId2";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, false, policyIdSecond,
        "policyName2", null);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, pathname, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, pathname, null);

    assertThat(dao.getCount()).isEqualTo(4);
  }

  @Test
  public void testDeleteByRepositoryId_H2() {
    assertThat(dao.isDatabaseEmbedded()).isTrue();

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname1", null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname2", null);

    dao.deleteByRepositoryId(null /* TransactionContext */, repository.getId());

    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  @PostgresTest
  public void testDeleteByRepositoryId_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();

    repository = tempEntity.newRepository();

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname1", null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, "pathname2", null);
    assertThat(dao.getByRepositoryId(repository.getId())).hasSize(2);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }

    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  public void testGetRepositoryResultsDetailsNonAggregate_H2() {
    testGetRepositoryResultsDetailsNonAggregate();
  }

  @Test
  @PostgresTest
  public void testGetRepositoryResultsDetailsNotAggregate_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetailsNonAggregate();
  }

  private void testGetRepositoryResultsDetailsNonAggregate() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 1);
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);
    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
            p2.getId(), p2.getName(), c1.getComponentIdentifier());
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, false);
    RepositoryPolicyViolation c2v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c2.getPathname(), false,
            p1.getId(), p1.getName(), c2.getComponentIdentifier());
    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = Collections.emptyMap();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    repositoryResultsDetailsFilter.aggregate = false;

    List<RepositoryResultsDetails> repositoryResultsDetails =
        dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);

    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(c1, c1v1),
        toRepositoryResultsDetails(c1, c1v2),
        toRepositoryResultsDetails(c2, c2v1)
    );
  }

  @Test
  public void testGetRepositoryResultsDetailsAggregate_H2() {
    testGetRepositoryResultsDetailsAggregate();
  }

  @Test
  @PostgresTest
  public void testGetRepositoryResultsDetailsAggregate_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetailsAggregate();
  }

  private void testGetRepositoryResultsDetailsAggregate() {
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 1);
    Repository repository = tempEntity.newRepository();
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);
    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
            p2.getId(), p2.getName(), c1.getComponentIdentifier());
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, false);
    RepositoryPolicyViolation c2v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c2.getPathname(), false,
            p1.getId(), p1.getName(), c2.getComponentIdentifier());
    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = Collections.emptyMap();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    repositoryResultsDetailsFilter.aggregate = true;

    List<RepositoryResultsDetails> repositoryResultsDetails =
        dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);

    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetailsWithoutWaived(c1, c1v1),
        toRepositoryResultsDetailsWithoutWaived(c2, c2v1)
    );
  }

  @Test
  public void testGetRepositoryResultsDetails_FilterQuarantineTime_H2() {
    testGetRepositoryResultsDetails_FilterQuarantineTime();
  }

  @Test
  @PostgresTest
  public void testGetRepositoryResultsDetails_FilterQuarantineTime_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetails_FilterQuarantineTime();
  }

  private void testGetRepositoryResultsDetails_FilterQuarantineTime() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "my-repo");
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p", 10);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3", "c3", "e3");

    LocalDate localDate1 = LocalDate.of(2023, 10, 19);
    LocalDate localDate2 = LocalDate.of(2023, 10, 18);
    ZoneId defaultZoneId = ZoneId.systemDefault();
    Date date1 = Date.from(localDate1.atStartOfDay(defaultZoneId).toInstant());
    Date date2 = Date.from(localDate2.atStartOfDay(defaultZoneId).toInstant());

    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, date1, date1);
    RepositoryComponent c2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g2/a2/v2/test-v2-c2.e2", "hash2",
            componentIdentifier2, date2, date2);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g3/a3/v3/test-v3-c3.e3", "hash3",
        componentIdentifier3, false);

    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getThreatLevel(), c1.getPathname(), false,
            policy.getId(), policy.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c2v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), policy.getThreatLevel(), c2.getPathname(), false,
            policy.getId(), policy.getName(), c2.getComponentIdentifier());

    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = new HashMap<>();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    List<RepositoryResultsDetails> repositoryResultsDetails;

    repositoryResultsDetails = dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(c1, c1v1),
        toRepositoryResultsDetails(c2, c2v1)
    );

    repositoryResultsDetailsFilter.searchFilters.put("QUARANTINE_TIME", "19");
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactly(
        toRepositoryResultsDetails(c1, c1v1)
    );

    repositoryResultsDetailsFilter.searchFilters.put("QUARANTINE_TIME", "18");
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactly(
        toRepositoryResultsDetails(c2, c2v1)
    );
  }

  @Test
  public void testGetRepositoryResultsDetails_FilterThreatLevel_H2() {
    testGetRepositoryResultsDetails_FilterThreatLevel();
  }

  @Test
  @PostgresTest
  public void testGetRepositoryResultsDetails_FilterThreatLevel_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    testGetRepositoryResultsDetails_FilterThreatLevel();
  }

  private void testGetRepositoryResultsDetails_FilterThreatLevel() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "my-repo");
    Policy p1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p1", 10);
    Policy p2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p2", 5);
    Policy p3 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p3", 1);
    Policy p4 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "p4", 0);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");

    RepositoryComponent c1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "g1/a1/v1/test-v1-c1.e1", "hash1",
            componentIdentifier1, false);

    RepositoryPolicyViolation c1v1 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p1.getThreatLevel(), c1.getPathname(), false,
            p1.getId(), p1.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v2 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p2.getThreatLevel(), c1.getPathname(), false,
            p2.getId(), p2.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v3 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p3.getThreatLevel(), c1.getPathname(), false,
            p3.getId(), p3.getName(), c1.getComponentIdentifier());
    RepositoryPolicyViolation c1v4 =
        tempEntity.newRepositoryPolicyViolation(repository.getId(), p4.getThreatLevel(), c1.getPathname(), false,
            p4.getId(), p4.getName(), c1.getComponentIdentifier());

    RepositoryResultsDetailsFilter repositoryResultsDetailsFilter = new RepositoryResultsDetailsFilter();
    repositoryResultsDetailsFilter.page = 1;
    repositoryResultsDetailsFilter.pageSize = 12;
    repositoryResultsDetailsFilter.violationStateFilters = new HashSet<>();
    repositoryResultsDetailsFilter.searchFilters = new HashMap<>();
    repositoryResultsDetailsFilter.matchStateFilter = "";
    List<RepositoryResultsDetails> repositoryResultsDetails;

    repositoryResultsDetails = dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(c1, c1v1),
        toRepositoryResultsDetails(c1, c1v2),
        toRepositoryResultsDetails(c1, c1v3),
        toRepositoryResultsDetails(c1, c1v4)
    );

    repositoryResultsDetailsFilter.threatLevelFilters = Arrays.asList(5, 5);
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(c1, c1v2)
    );

    repositoryResultsDetailsFilter.threatLevelFilters = Arrays.asList(5, 10);
    repositoryResultsDetails = dao.getRepositoryResultsDetails(repository.getId(), repositoryResultsDetailsFilter);
    assertThat(repositoryResultsDetails).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        toRepositoryResultsDetails(c1, c1v1),
        toRepositoryResultsDetails(c1, c1v2)
    );
  }

  private RepositoryResultsDetails toRepositoryResultsDetailsWithoutWaived(
      RepositoryComponent repositoryComponent,
      RepositoryPolicyViolation repositoryPolicyViolation)
  {
    RepositoryResultsDetails result = toRepositoryResultsDetails(repositoryComponent, repositoryPolicyViolation);
    result.waived = null;
    return result;
  }

  private RepositoryResultsDetails toRepositoryResultsDetails(
      RepositoryComponent repositoryComponent,
      RepositoryPolicyViolation repositoryPolicyViolation)
  {
    return new RepositoryResultsDetails(
        repositoryPolicyViolation.getThreatLevel(),
        repositoryPolicyViolation.getPolicyName(),
        repositoryComponent.getComponentIdentifier().getFormat(),
        repositoryComponent.getPathname(),
        ComponentIdentifierAdapter.toJson(repositoryComponent.getComponentIdentifier().getCoordinates()),
        repositoryComponent.getDisplayName(),
        repositoryComponent.getHash(),
        repositoryComponent.getMatchStateId(),
        (repositoryComponent.getQuarantineTime() != null &&
            repositoryComponent.getUnquarantineTime() == null) ? repositoryComponent.getQuarantineTime() : null,
        repositoryPolicyViolation.isWaived()
    );
  }
}
