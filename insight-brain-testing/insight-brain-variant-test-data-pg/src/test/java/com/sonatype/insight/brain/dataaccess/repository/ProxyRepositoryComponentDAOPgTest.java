/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link ProxyRepositoryComponentDAOTest} (CLM-45228).
 */
@PostgresTest
public class ProxyRepositoryComponentDAOPgTest
    extends AbstractDbDAOTest
{
  public static final int PARTITION_THRESHOLD = 2;

  private final Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june3rd2020 = Date.from(LocalDateTime.of(2020, 6, 3, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june4th2020 = Date.from(LocalDateTime.of(2020, 6, 4, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june5th2020 = Date.from(LocalDateTime.of(2020, 6, 5, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june6th2020 = Date.from(LocalDateTime.of(2020, 6, 6, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june7th2020 = Date.from(LocalDateTime.of(2020, 6, 7, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june8th2020 = Date.from(LocalDateTime.of(2020, 6, 8, 1, 0).toInstant(ZoneOffset.UTC));

  private ProxyRepositoryComponentDAO dao;

  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private Repository repositoryTwo;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryComponentDAO();
    quarantinedComponentAccessDAO = daoFactory.createQuarantinedComponentAccessDAO();
    repositoryTwo = tempEntity.newRepository();
  }

  private void assertIsContainedIn(ProxyRepositoryComponent expected, List<ProxyRepositoryComponent> in) {
    Optional<ProxyRepositoryComponent> optionalRepositoryComponent = in.stream()
        .filter(component -> component.getPathname().equals(expected.getPathname()))
        .findFirst();
    assertThat(optionalRepositoryComponent.isPresent()).isTrue();
    assertRepositoryComponent(optionalRepositoryComponent.get(), expected);
  }

  private void assertRepositoryComponent(ProxyRepositoryComponent expected, ProxyRepositoryComponent actual) {
    assertRepositoryComponent(expected.getRepositoryId(), expected.getPathname(), expected.getTime(),
        expected.getHash(), expected.getComponentIdentifier(), expected.getMatchStateId(),
        expected.getIdentificationSourceId(), expected.getLastEvaluationTime(), actual,
        expected.getAnalyzerFeaturesJson());
  }

  private void assertRepositoryComponent(
      String repositoryId,
      String pathname,
      Date time,
      String hash,
      ComponentIdentifier componentIdentifier,
      String matchStateId,
      String identificationSourceId,
      Date lastEvaluationTime,
      ProxyRepositoryComponent actual,
      String analyzerFeatures)
  {
    assertThat(actual.getRepositoryId()).isEqualTo(repositoryId);
    assertThat(actual.getPathname()).isEqualTo(pathname);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getTime()).isEqualTo(time);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getDisplayName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(actual.getMatchStateId()).isEqualTo(matchStateId);
    assertThat(actual.getIdentificationSourceId()).isEqualTo(identificationSourceId);
    assertThat(actual.getLastEvaluationTime()).isEqualTo(lastEvaluationTime);
    assertThat(actual.getAnalyzerFeaturesJson()).isEqualTo(analyzerFeatures);
  }

  @Test
  public void testDeleteByRepositoryId_Postgres() {
    assertThat(dao.isDatabaseEmbedded()).isFalse();

    repository = tempEntity.newRepository();
    ProxyRepositoryComponent repositoryComponent1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    ProxyRepositoryComponent repositoryComponent2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newQuarantinedComponentAccess(repositoryComponent1.getRepositoryId(), repositoryComponent1.getId());
    tempEntity.newQuarantinedComponentAccess(repositoryComponent2.getRepositoryId(), repositoryComponent2.getId());
    assertThat(dao.getByRepositoryId(repository.getId())).hasSize(3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }

    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
    assertThat(quarantinedComponentAccessDAO.getAll()).isEmpty();
  }

  @Test
  public void testGetWithActiveViolationsByRepositoryIdAndPathname_Postgres() {
    // H2 doesn't support FULL OUTER JOIN, so the merged read is emulated as a LEFT JOIN UNION ALL a
    // reverse LEFT JOIN WHERE unmatched (CLM-42134). Exercise both halves of that emulation against
    // real Postgres, which does support FULL OUTER JOIN natively, to guard against the two dialects
    // diverging on this query.
    assertThat(dao.isDatabaseEmbedded()).isFalse();
    repository = tempEntity.newRepository();

    String withComponent = "path";
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), withComponent);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, withComponent, null);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 3, withComponent, null);

    ProxyRepositoryComponentDAO.ComponentWithActiveViolations withComponentResult =
        getWithActiveViolations(repository.getId(), withComponent);
    assertThat(withComponentResult.component()).isNotNull();
    assertThat(withComponentResult.component().getId()).isEqualTo(component.getId());
    assertThat(withComponentResult.activeViolations()).extracting(ProxyRepositoryPolicyViolation::getThreatLevel)
        .containsExactly(3, 1);

    // CLM-40943 archive-of-archives orphan case: active violation with no matching component row.
    String orphanPathname = "outer.zip!/inner.jar";
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, orphanPathname, null);

    ProxyRepositoryComponentDAO.ComponentWithActiveViolations orphanResult =
        getWithActiveViolations(repository.getId(), orphanPathname);
    assertThat(orphanResult.component()).isNull();
    assertThat(orphanResult.activeViolations()).extracting(ProxyRepositoryPolicyViolation::getThreatLevel)
        .containsExactly(5);
  }

  private ProxyRepositoryComponentDAO.ComponentWithActiveViolations getWithActiveViolations(
      String repositoryId,
      String pathname)
  {
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      ProxyRepositoryComponentDAO.ComponentWithActiveViolations result =
          dao.getWithActiveViolationsByRepositoryIdAndPathname(tx, repositoryId, pathname);
      tx.commit();
      return result;
    }
  }

  public ProxyRepositoryComponent newQuarantinedRepositoryComponent(String repositoryId, String artifactName) {
    return tempEntity.newRepositoryComponent(repositoryId, MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", artifactName, "v", "c", "e"), true);
  }

  public void newQuarantinedRepositoryComponentPolicyViolation(
      Policy policy,
      ProxyRepositoryComponent proxyRepositoryComponent)
  {
    tempEntity.newRepositoryPolicyViolation(proxyRepositoryComponent.getRepositoryId(), 5,
        proxyRepositoryComponent.getPathname(),
        false, FailActionType.ID, policy.getId(), policy.getName(), proxyRepositoryComponent.getComponentIdentifier());
  }

  public List<ProxyRepositoryComponent> filter(String policyId, String componentName) {
    List<FirewallFilterField> firewallFilterFields = new ArrayList<>();
    if (policyId != null) {
      firewallFilterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID, policyId));
    }
    if (componentName != null) {
      firewallFilterFields.add(new FirewallFilterField(FirewallFilterableField.COMPONENT_NAME, componentName));
    }
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 1000, FirewallComponentFilterState.QUARANTINE, null, true,
            firewallFilterFields);
    return dao.getFirewallRepositoryComponents(filter);
  }

  // Guards the merge-function fix: multiple quarantines on the same calendar day (different hours) must
  // aggregate into a single map entry with the counts summed. Runs against Postgres because the failure
  // mode reported by the customer is Postgres-specific, though we could not reproduce their exact throw
  // in test environments.
  @Test
  public void testGetConsolidatedQuarantinedComponentsMetricByDate_MultipleQuarantinesSameDay_Postgres() {
    Repository repo = tempEntity.newRepository();
    Date day1At08 = Date.from(LocalDateTime.of(2026, 7, 14, 8, 0).toInstant(ZoneOffset.UTC));
    Date day1At15 = Date.from(LocalDateTime.of(2026, 7, 14, 15, 0).toInstant(ZoneOffset.UTC));
    Date day2At10 = Date.from(LocalDateTime.of(2026, 7, 15, 10, 0).toInstant(ZoneOffset.UTC));
    tempEntity.newRepositoryComponent(repo.getId(), "path-day1-08", day1At08, null);
    tempEntity.newRepositoryComponent(repo.getId(), "path-day1-15", day1At15, null);
    tempEntity.newRepositoryComponent(repo.getId(), "path-day2-10", day2At10, null);

    Date floor = Date.from(LocalDateTime.of(2026, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
    Map<LocalDate, Long> result = dao.getConsolidatedQuarantinedComponentsMetricByDate(floor);

    assertThat(result)
        .containsEntry(LocalDate.of(2026, 7, 14), 2L)
        .containsEntry(LocalDate.of(2026, 7, 15), 1L);
  }

  private void setupMockDataForGetFirewallRepositoryComponents() {
    // ADD COMPONENT
    tempEntity
        .newRepositoryComponent(repository.getId(), "/autoreleased1", june1st2020, june2nd2020, june8th2020, true);
    final ProxyRepositoryComponent component2 =
        tempEntity
            .newRepositoryComponent(repository.getId(), "/autoreleased2", june2nd2020, june3rd2020, june7th2020, true);
    final ProxyRepositoryComponent component3 =
        tempEntity
            .newRepositoryComponent(repository.getId(), "/autoreleased3", june3rd2020, june4th2020, june6th2020, true);
    tempEntity
        .newRepositoryComponent(repository.getId(), "/autoreleased4", june4th2020, june5th2020, june5th2020, true);
    final ProxyRepositoryComponent component5 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june5th2020, null, june4th2020, false);
    final ProxyRepositoryComponent component6 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", june6th2020, null, june3rd2020, false);
    tempEntity.newRepositoryComponent(repository.getId(), "/manualreleased1", june7th2020, june8th2020, june2nd2020,
        false);
    tempEntity.newRepositoryComponent(repository.getId(), "/audit1", null, null, june1st2020, false);

    // CREATE POLICY VIOLATION
    tempEntity
        .newRepositoryPolicyViolation(repository.getId(), 5, "/autoreleased2", false, "policy_id_2", "policy_2",
            component2.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "/autoreleased3", false, "policy_id_2", "policy_2",
        component3.getComponentIdentifier());

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "/quarantined1", false, FailActionType.ID,
        "policy_id_2", "policy_2", component5.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "/quarantined2", false, WarnActionType.ID,
        "policy_id_2", "policy_2", component6.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, "/quarantined2", true, FailActionType.ID,
        "policy_id_2", "policy_2", component6.getComponentIdentifier());
  }

  private void assertComponentForFirewall(
      final ProxyRepositoryComponent component,
      final String pathName,
      final Date quarantineTime,
      final Date unquarantineTime,
      final Boolean autoUnquarantined)
  {
    assertThat(component.getPathname()).isEqualTo(pathName);
    assertThat(component.getQuarantineTime()).isEqualTo(quarantineTime);
    assertThat(component.getUnquarantineTime()).isEqualTo(unquarantineTime);
    assertThat(component.getAutoUnquarantined()).isEqualTo(autoUnquarantined);
  }

  private ProxyRepositoryComponent newComponentWithEvalTime(
      String repositoryId,
      String pathname,
      String hash,
      Date evalTime)
  {
    return newComponentWith(repositoryId, pathname, hash, evalTime, evalTime);
  }

  private ProxyRepositoryComponent newComponentWithTimeAndHash(
      String repositoryId,
      String pathname,
      String hash,
      Date rcTime)
  {
    // last_evaluation_time stays in the past so the row is eligible; rc.time controls emission order.
    Date pastEval = new Date(rcTime.getTime() - 3600_000L);
    return newComponentWith(repositoryId, pathname, hash, rcTime, pastEval);
  }

  private ProxyRepositoryComponent newComponentWith(
      String repositoryId,
      String pathname,
      String hash,
      Date rcTime,
      Date lastEvalTime)
  {
    ProxyRepositoryComponent rc = new ProxyRepositoryComponent(
        repositoryId, pathname, rcTime, hash,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), lastEvalTime);
    dao.insert(rc);
    return rc;
  }

  private static String uuid(String prefix) {
    return prefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
  }
}
