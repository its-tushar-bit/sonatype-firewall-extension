/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.utils.DateConverter;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.postgres.PostgresServer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RepositoryComponentDAOTest
    extends AbstractDbDAOTest
{
  private final Date june1st2020 = Date.from(LocalDateTime.of(2020, 6, 1, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june2nd2020 = Date.from(LocalDateTime.of(2020, 6, 2, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june3rd2020 = Date.from(LocalDateTime.of(2020, 6, 3, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june4th2020 = Date.from(LocalDateTime.of(2020, 6, 4, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june5th2020 = Date.from(LocalDateTime.of(2020, 6, 5, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june6th2020 = Date.from(LocalDateTime.of(2020, 6, 6, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june7th2020 = Date.from(LocalDateTime.of(2020, 6, 7, 1, 0).toInstant(ZoneOffset.UTC));

  private final Date june8th2020 = Date.from(LocalDateTime.of(2020, 6, 8, 1, 0).toInstant(ZoneOffset.UTC));

  private final RepositoryComponentDAO dao = new RepositoryComponentDAO();

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO = new QuarantinedComponentAccessDAO();

  private Repository repositoryTwo;

  @Before
  public void before() {
    repositoryTwo = tempEntity.newRepository();
  }

  @Test
  public void testCRUD() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId",
        "version");

    // Create
    Date createTime = new Date();
    RepositoryComponent repositoryComponent = new RepositoryComponent(repository.getId(), "path", createTime, "hash",
        componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), createTime);
    String analyzerFeatures =
        JsonUtils.format(new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, "client", null));
    repositoryComponent.setAnalyzerFeaturesJson(analyzerFeatures);
    dao.insert(repositoryComponent);
    assertThat(repositoryComponent.getId()).isNotNull();

    // Get
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent).isNotNull();
    assertRepositoryComponent(repository.getId(), "path", createTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), createTime, repositoryComponent,
        analyzerFeatures);

    // Update
    Date updateTime = new Date();
    repositoryComponent.setLastEvaluationTime(updateTime);
    analyzerFeatures =
        JsonUtils.format(new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.HASH, "client", null));
    repositoryComponent.setAnalyzerFeaturesJson(analyzerFeatures);
    dao.update(repositoryComponent);
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent).isNotNull();
    assertRepositoryComponent(repository.getId(), "path", createTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), updateTime, repositoryComponent,
        analyzerFeatures);

    // Delete
    tempEntity.newQuarantinedComponentAccess(repositoryComponent.getRepositoryId(), repositoryComponent.getId());
    dao.delete(repositoryComponent);

    // Get
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent).isNull();
    assertThat(quarantinedComponentAccessDAO.getAll()).isEmpty();
  }

  @Test
  public void testGetComponentCountByRepositoryId() {
    assertThat(dao.getComponentCountByRepositoryId(repository.getId())).isEqualTo(0);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    // Component in another repository
    tempEntity.newRepositoryComponent(repositoryTwo.getId());

    assertThat(dao.getComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
  }

  @Test
  public void testGetKnownComponentCountByRepositoryId() {
    // Component in another repository
    tempEntity.newRepositoryComponent(repositoryTwo.getId());

    assertThat(dao.getKnownComponentCountByRepositoryId(repository.getId())).isEqualTo(0);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN,
        ComponentIdentifier.createMavenCoordinates("unknown", "component", "1"));
    assertThat(dao.getKnownComponentCountByRepositoryId(repository.getId())).isEqualTo(0);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "jar"));
    assertThat(dao.getKnownComponentCountByRepositoryId(repository.getId())).isEqualTo(1);
  }

  @Test
  public void testGetByRepositoryIdAndPathnames_GetsSingleRepositoryComponent() {
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "jar"));

    ArrayList<String> pathnames = new ArrayList<>();
    pathnames.add(repositoryComponent.getPathname());

    List<RepositoryComponent> repositoryComponents =
        dao.getByRepositoryIdAndPathnames(repository.getId(), pathnames);

    assertThat(repositoryComponents).hasSize(1);
    assertRepositoryComponent(repositoryComponent.getRepositoryId(), repositoryComponent.getPathname(),
        repositoryComponent.getTime(),
        repositoryComponent.getHash(), repositoryComponent.getComponentIdentifier(),
        repositoryComponent.getMatchStateId(), repositoryComponent.getIdentificationSourceId(),
        repositoryComponent.getLastEvaluationTime(), repositoryComponents.get(0), null);
  }

  @Test
  public void testGetByRepositoryIdAndPathnames_GetsRepositoryComponentInBatches() {
    List<RepositoryComponent> components = new ArrayList<>();
    for (int i = 0; i < TestRepositoryComponentDAO.PARTITION_THRESHOLD + 1; i++) {
      components.add(tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
          ComponentIdentifier.createMavenCoordinates("g", "a", "v" + i, "c", "jar")));
    }

    List<String> pathnames = components.stream()
        .map(RepositoryComponent::getPathname)
        .collect(Collectors.toList());

    List<RepositoryComponent> repositoryComponents =
        dao.getByRepositoryIdAndPathnames(repository.getId(), pathnames);

    assertThat(components).hasSize(TestRepositoryComponentDAO.PARTITION_THRESHOLD + 1);
    assertThat(repositoryComponents).hasSize(TestRepositoryComponentDAO.PARTITION_THRESHOLD + 1);
    for (RepositoryComponent expected : components) {
      assertIsContainedIn(expected, repositoryComponents);
    }
  }

  private void assertIsContainedIn(RepositoryComponent expected, List<RepositoryComponent> in) {
    Optional<RepositoryComponent> optionalRepositoryComponent = in.stream()
        .filter(component -> component.getPathname().equals(expected.getPathname()))
        .findFirst();
    assertThat(optionalRepositoryComponent.isPresent()).isTrue();
    assertRepositoryComponent(optionalRepositoryComponent.get(), expected);
  }

  private void assertRepositoryComponent(RepositoryComponent expected, RepositoryComponent actual) {
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
      RepositoryComponent actual,
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
  public void testGetQuarantinedComponentCountByRepositoryId() {
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(0);
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", new Date(), null);
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(1);
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", new Date(), null);
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
    // unquarantined component, so shouldn't add to to total
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined3", new Date(), new Date());
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
    // not a quarantined item, shouldn't add to count
    tempEntity.newRepositoryComponent(repository.getId(), "/notquarantined", null, null);
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
  }

  @Test
  public void testGetOldestComponentEvaluationTimeByRepositoryId() {
    Date oldest = new Date();
    tempEntity.newRepositoryComponent(repository.getId(), new Date(oldest.getTime() + 1000));
    tempEntity.newRepositoryComponent(repository.getId(), oldest);
    tempEntity.newRepositoryComponent(repository.getId(), new Date(oldest.getTime() + 2000));

    assertThat(dao.getOldestComponentEvaluationTimeByRepositoryId(repository.getId())).isEqualTo(oldest);
  }

  @Test
  public void testGetCount() {
    // Component in another repository
    tempEntity.newRepositoryComponent(repositoryTwo.getId());
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    assertThat(dao.getCount()).isEqualTo(3);
  }

  @Test
  public void testDeleteByRepositoryId_H2() {
    assertThat(dao.isDatabaseEmbedded()).isTrue();

    RepositoryComponent repositoryComponent1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    RepositoryComponent repositoryComponent2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newQuarantinedComponentAccess(repositoryComponent1.getRepositoryId(), repositoryComponent1.getId());
    tempEntity.newQuarantinedComponentAccess(repositoryComponent2.getRepositoryId(), repositoryComponent2.getId());
    ClusterLock.createForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()).close();
    ClusterLock.createForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()).close();
    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isTrue();
    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isTrue();

    dao.deleteByRepositoryId(null /* TransactionContext */, repository.getId());

    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isFalse();
    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isFalse();
    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
    assertThat(quarantinedComponentAccessDAO.getAll()).isEmpty();
  }

  @Test
  public void testDeleteByRepositoryId_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();

    try (PostgresServer postgres = new PostgresServer()) {
      // Create a postgres ODS database
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);

      assertThat(dao.isDatabaseEmbedded()).isFalse();

      repository = tempEntity.newRepository();
      RepositoryComponent repositoryComponent1 =
          tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
      RepositoryComponent repositoryComponent2 =
          tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
      tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
      tempEntity.newQuarantinedComponentAccess(repositoryComponent1.getRepositoryId(), repositoryComponent1.getId());
      tempEntity.newQuarantinedComponentAccess(repositoryComponent2.getRepositoryId(), repositoryComponent2.getId());
      assertThat(dao.getByRepositoryId(repository.getId())).hasSize(3);
      ClusterLock.createForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()).close();
      ClusterLock.createForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()).close();
      assertThat(ClusterLock.lockExists(ClusterLock
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isTrue();
      assertThat(ClusterLock.lockExists(ClusterLock
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isTrue();

      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.deleteByRepositoryId(tx, repository.getId());
        tx.commit();
      }

      assertThat(ClusterLock.lockExists(ClusterLock
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isFalse();
      assertThat(ClusterLock.lockExists(ClusterLock
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isFalse();
      assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
      assertThat(quarantinedComponentAccessDAO.getAll()).isEmpty();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testGetQuarantinedByRepositoryIdAndDate() {
    Repository repository = tempEntity.newRepository();
    Date oldQuarantinedDate = Date.from(Instant.now().minusMillis(1000));
    Date quarantinedDateToQuery = new Date();
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "old-quarantined-path", "hash1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), new Date(), oldQuarantinedDate);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "not-quarantined", "hash3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), new Date(), null /* quarantine time */);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "un-quarantined", "hash4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"), new Date(), quarantinedDateToQuery, new Date());
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "quarantined-path", "hash2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), new Date(), quarantinedDateToQuery);

    List<RepositoryComponent> results =
        dao.getQuarantinedByRepositoryIdAndDate(repository.getId(), quarantinedDateToQuery);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getPathname()).isEqualTo("quarantined-path");
  }

  @Test
  public void testGetQuarantinedCountByRepositoryIdAndDate() {
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date moreThanOneYearAgo = DateUtils.addDays(oneYearAgo, -1);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "now-quarantined-path", "hash1",
        ComponentIdentifier.createNpmCoordinates("p1", "v1"), now, now);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "not-quarantined-path", "hash2",
        ComponentIdentifier.createNpmCoordinates("p2", "v2"), now, null /* quarantine time */);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "1-year-ago-quarantined-path-1", "hash3",
        ComponentIdentifier.createNpmCoordinates("p3", "v3"), oneYearAgo, oneYearAgo);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "1-year-ago-quarantined-path-2", "hash4",
        ComponentIdentifier.createNpmCoordinates("p4", "v4"), oneYearAgo, oneYearAgo);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "+1-year-ago-quarantined-path", "hash5",
        ComponentIdentifier.createNpmCoordinates("p5", "v5"), moreThanOneYearAgo, moreThanOneYearAgo);

    Map<LocalDate, Long> results = dao.getQuarantinedCountByRepositoryIdAndDate(repository.getId(), oneYearAgo);

    assertThat(results)
        .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(toLocalDate(now), 1L, toLocalDate(oneYearAgo), 2L));
  }

  @Test
  public void testGetAutoReleaseQuarantinedCountByDate() {
    final Date oneYearAgo = Date.from(
        (LocalDate.now().minusYears(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    final Date startOfCurMonth =
        Date.from((LocalDate.now().withDayOfMonth(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", new Date(), oneYearAgo, true);

    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", new Date(), startOfCurMonth, true);

    // not a quarantined item, shouldn't add to count
    tempEntity.newRepositoryComponent(repository.getId(), "/notquarantined", null, null);

    assertThat(dao.getAutoReleaseQuarantinedCountByDate(startOfCurMonth)).isOne();
  }

  @Test
  public void testGetAutoReleaseQuarantinedCountByRepositoryIdAndDate() {
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date moreThanOneYearAgo = DateUtils.addDays(oneYearAgo, -1);

    tempEntity.newRepositoryComponent(repository.getId(), "now-auto-released-path-1", now, now, true);
    tempEntity.newRepositoryComponent(repository.getId(), "now-auto-released-path-2", now, now, true);
    tempEntity.newRepositoryComponent(repository.getId(), "not-auto-released-path", now, now, false);
    tempEntity.newRepositoryComponent(repository.getId(), "1-year-ago-auto-released-path-2", oneYearAgo, oneYearAgo,
        true);
    tempEntity.newRepositoryComponent(repository.getId(), "+1-year-ago-auto-released-path-2", moreThanOneYearAgo,
        moreThanOneYearAgo, true);

    Map<LocalDate, Long> results =
        dao.getAutoReleaseQuarantinedCountByRepositoryIdAndDate(repository.getId(), oneYearAgo);

    assertThat(results).containsExactlyInAnyOrderEntriesOf(
        ImmutableMap.of(LocalDate.now(), 2L, DateConverter.toLocalDate(oneYearAgo), 1L));
  }

  @Test
  public void testGetQuarantinedComponentCount() {
    Date quarantineDate = new Date();
    Date unquarantineDate = new Date();
    Repository repo1 = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true);
    tempEntity.newRepositoryComponent(repo1.getId(), "repo1/not-quarantined", null, null);
    tempEntity.newRepositoryComponent(repo1.getId(), "repo1/quarantined", quarantineDate, null);
    tempEntity.newRepositoryComponent(repo1.getId(), "repo1/unquarantined", quarantineDate, unquarantineDate);
    Repository repo2 = tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo2", true, true);
    tempEntity.newRepositoryComponent(repo2.getId(), "repo2/not-quarantined", null, null);
    tempEntity.newRepositoryComponent(repo2.getId(), "repo2/quarantined", quarantineDate, null);
    tempEntity.newRepositoryComponent(repo2.getId(), "repo2/unquarantined", quarantineDate, unquarantineDate);

    assertThat(dao.getQuarantinedComponentCount()).isEqualTo(2);
  }

  @Test
  public void testGetFirewallRepositoryComponents_AutoUnquarantineOnly() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SETUP FILTER
    final FirewallSortableField sortField = FirewallSortableField.QUARANTINE_TIME;
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.UNQUARANTINE_AUTO, sortField, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantined).isNotEmpty();
    assertThat(autoUnquarantined.size()).isEqualTo(4);
    assertComponentForFirewall(autoUnquarantined.get(0), "/autoreleased1", june1st2020, june2nd2020, true);
    assertComponentForFirewall(autoUnquarantined.get(1), "/autoreleased2", june2nd2020, june3rd2020, true);
    assertComponentForFirewall(autoUnquarantined.get(2), "/autoreleased3", june3rd2020, june4th2020, true);
    assertComponentForFirewall(autoUnquarantined.get(3), "/autoreleased4", june4th2020, june5th2020, true);
  }

  @Test
  public void testGetFirewallRepositoryComponents_QuarantinedOnly() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SETUP FILTER
    final FirewallSortableField sortField = FirewallSortableField.QUARANTINE_TIME;
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.QUARANTINE, sortField, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION - Both components should be returned, regardless of whether a failed violation is present
    assertThat(autoUnquarantined).isNotEmpty();
    assertThat(autoUnquarantined.size()).isEqualTo(2);
    assertComponentForFirewall(autoUnquarantined.get(0), "/quarantined1", june5th2020, null, null);
    assertComponentForFirewall(autoUnquarantined.get(1), "/quarantined2", june6th2020, null, null);
  }

  @Test
  public void testGetFirewallRepositoryComponents_ManualUnquarantinedOnly() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SETUP FILTER
    final FirewallSortableField sortField = FirewallSortableField.QUARANTINE_TIME;
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.UNQUARANTINE_MANUAL, sortField, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantined).isNotEmpty();
    assertThat(autoUnquarantined.size()).isEqualTo(1);
    assertComponentForFirewall(autoUnquarantined.get(0), "/manualreleased1", june7th2020, june8th2020, false);
  }

  @Test
  public void testGetFirewallRepositoryComponents_AuditOnly() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SETUP FILTER
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.AUDIT, null, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantined).isNotEmpty();
    assertThat(autoUnquarantined.size()).isEqualTo(1);
    assertComponentForFirewall(autoUnquarantined.get(0), "/audit1", null, null, null);
  }

  @Test
  public void testGetFirewallRepositoryComponents_StateUnquarantinedAll() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SETUP FILTER
    final FirewallSortableField sortField = FirewallSortableField.QUARANTINE_TIME;
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.UNQUARANTINE_ALL, sortField, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantined).isNotEmpty();
    assertThat(autoUnquarantined.size()).isEqualTo(5);
    assertComponentForFirewall(autoUnquarantined.get(0), "/autoreleased1", june1st2020, june2nd2020, true);
    assertComponentForFirewall(autoUnquarantined.get(1), "/autoreleased2", june2nd2020, june3rd2020, true);
    assertComponentForFirewall(autoUnquarantined.get(2), "/autoreleased3", june3rd2020, june4th2020, true);
    assertComponentForFirewall(autoUnquarantined.get(3), "/autoreleased4", june4th2020, june5th2020, true);
    assertComponentForFirewall(autoUnquarantined.get(4), "/manualreleased1", june7th2020, june8th2020, false);
  }

  @Test
  public void testGetFirewallRepositoryComponents_AllStates() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SETUP FILTER
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 10, FirewallComponentFilterState.ALL, null, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> all = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(all).isNotEmpty();
    assertThat(all.size()).isEqualTo(8);
    assertComponentForFirewall(all.get(0), "/audit1", null, null, null);
    assertComponentForFirewall(all.get(1), "/manualreleased1", june7th2020, june8th2020, false);
    assertComponentForFirewall(all.get(2), "/quarantined2", june6th2020, null, null);
    assertComponentForFirewall(all.get(3), "/quarantined1", june5th2020, null, null);
    assertComponentForFirewall(all.get(4), "/autoreleased4", june4th2020, june5th2020, true);
    assertComponentForFirewall(all.get(5), "/autoreleased3", june3rd2020, june4th2020, true);
    assertComponentForFirewall(all.get(6), "/autoreleased2", june2nd2020, june3rd2020, true);
    assertComponentForFirewall(all.get(7), "/autoreleased1", june1st2020, june2nd2020, true);
  }

  @Test
  public void testGetFirewallRepositoryComponents_sortDesc() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SORT DESC
    final FirewallSortableField sortField = FirewallSortableField.QUARANTINE_TIME;
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, sortField, false,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantinedDesc = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantinedDesc).isNotEmpty();
    assertThat(dao.getTotalFirewallRepositoryComponents(filter)).isEqualTo(4);
    assertThat(autoUnquarantinedDesc.size()).isEqualTo(2);
    assertComponentForFirewall(autoUnquarantinedDesc.get(0), "/autoreleased4", june4th2020, june5th2020, true);
    assertComponentForFirewall(autoUnquarantinedDesc.get(1), "/autoreleased3", june3rd2020, june4th2020, true);
  }

  @Test
  public void testGetFirewallRepositoryComponents_defaultSort() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SORT DESC
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, false,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantinedDesc = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantinedDesc).isNotEmpty();
    assertThat(dao.getTotalFirewallRepositoryComponents(filter)).isEqualTo(4);
    assertThat(autoUnquarantinedDesc.size()).isEqualTo(2);
    // Default sort will always return entries in the same order as inserted
    assertComponentForFirewall(autoUnquarantinedDesc.get(0), "/autoreleased1", june1st2020, june2nd2020, true);
    assertComponentForFirewall(autoUnquarantinedDesc.get(1), "/autoreleased2", june2nd2020, june3rd2020, true);
  }

  @Test
  public void testGetFirewallRepositoryComponents_filterByPolicyId() {
    setupMockDataForGetFirewallRepositoryComponents();

    // FILTER BY POLICY NAME
    final ArrayList<FirewallFilterField> filterFields = new ArrayList<>();
    filterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID, "policy_id_2"));
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.QUARANTINE, null, true,
            filterFields);

    // EXECUTE
    final List<RepositoryComponent> quarantinedFiltered = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION - only the component with failed policy violation should be returned
    // Warn action type or waived fail action type should not be returned
    assertThat(quarantinedFiltered).isNotEmpty();
    assertThat(dao.getTotalFirewallRepositoryComponents(filter)).isEqualTo(1);
    assertThat(quarantinedFiltered.size()).isEqualTo(1);
    assertComponentForFirewall(quarantinedFiltered.get(0), "/quarantined1", june5th2020, null, null);
  }

  @Test
  public void testGetFirewallRepositoryComponents_filterByMultiplePolicyIds() {
    setupMockDataForGetFirewallRepositoryComponents();

    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "/quarantined/multiple/test",
        june7th2020, null, june8th2020, false);

    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, component.getPathname(), false, FailActionType.ID,
        "policy_id_multiple", "policy_multiple", component.getComponentIdentifier());

    // FILTER BY POLICY NAME
    List<FirewallFilterField> filterFields = new ArrayList<>();
    filterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID,
        Sets.newHashSet("policy_id_2", "policy_id_multiple")));

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(1, 2,
        FirewallComponentFilterState.QUARANTINE, FirewallSortableField.QUARANTINE_TIME, true, filterFields);

    // EXECUTE
    List<RepositoryComponent> quarantinedFiltered = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION - only the components with the given policies IDs should be returned
    assertThat(quarantinedFiltered).isNotEmpty();
    assertThat(dao.getTotalFirewallRepositoryComponents(filter)).isEqualTo(2);
    assertThat(quarantinedFiltered).hasSize(2);
    assertComponentForFirewall(quarantinedFiltered.get(0), "/quarantined1", june5th2020, null, null);
    assertComponentForFirewall(quarantinedFiltered.get(1), "/quarantined/multiple/test", june7th2020, null, null);
  }

  @Test
  public void testGetFirewallRepositoryComponents_filterByComponentName() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent1 = newQuarantinedRepositoryComponent(repository.getId(), "a1");
    newQuarantinedRepositoryComponent(repository.getId(), "a2");

    assertThat(filter(null, "a1")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(repositoryComponent1);
    assertThat(filter(null, "A1")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(repositoryComponent1);
  }

  @Test
  public void testGetFirewallRepositoryComponents_filterByPolicyIdAndComponentName() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent1 = newQuarantinedRepositoryComponent(repository.getId(), "a11");
    RepositoryComponent repositoryComponent2 = newQuarantinedRepositoryComponent(repository.getId(), "a12");
    RepositoryComponent repositoryComponent3 = newQuarantinedRepositoryComponent(repository.getId(), "a21");
    RepositoryComponent repositoryComponent4 = newQuarantinedRepositoryComponent(repository.getId(), "a22");
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    Policy policy2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    newQuarantinedRepositoryComponentPolicyViolation(policy1, repositoryComponent1);
    newQuarantinedRepositoryComponentPolicyViolation(policy2, repositoryComponent2);
    newQuarantinedRepositoryComponentPolicyViolation(policy1, repositoryComponent3);
    newQuarantinedRepositoryComponentPolicyViolation(policy2, repositoryComponent4);

    assertThat(filter(null, null)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(repositoryComponent1, repositoryComponent2, repositoryComponent3,
            repositoryComponent4);
    assertThat(filter(policy1.getId(), null)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(repositoryComponent1, repositoryComponent3);
    assertThat(filter(null, "a1")).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(repositoryComponent1, repositoryComponent2);
    assertThat(filter(policy1.getId(), "a1")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(repositoryComponent1);
  }

  public RepositoryComponent newQuarantinedRepositoryComponent(String repositoryId, String artifactName) {
    return tempEntity.newRepositoryComponent(repositoryId, MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", artifactName, "v", "c", "e"), true);
  }

  public void newQuarantinedRepositoryComponentPolicyViolation(Policy policy, RepositoryComponent repositoryComponent) {
    tempEntity.newRepositoryPolicyViolation(repositoryComponent.getRepositoryId(), 5, repositoryComponent.getPathname(),
        false, FailActionType.ID, policy.getId(), policy.getName(), repositoryComponent.getComponentIdentifier());
  }

  public List<RepositoryComponent> filter(String policyId, String componentName) {
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

  @Test
  public void testGetFirewallRepositoryComponents_pageSizeThree() {
    setupMockDataForGetFirewallRepositoryComponents();

    // INCREASE PAGE SIZE
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 3, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined3Items = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantined3Items).isNotEmpty();
    assertThat(dao.getTotalFirewallRepositoryComponents(filter)).isEqualTo(4);
    assertThat(autoUnquarantined3Items.size()).isEqualTo(3);
    assertComponentForFirewall(autoUnquarantined3Items.get(0), "/autoreleased4", june4th2020, june5th2020, true);
    assertComponentForFirewall(autoUnquarantined3Items.get(1), "/autoreleased3", june3rd2020, june4th2020, true);
    assertComponentForFirewall(autoUnquarantined3Items.get(2), "/autoreleased2", june2nd2020, june3rd2020, true);
  }

  @Test
  public void testGetFirewallRepositoryComponents_2ndPage() {
    setupMockDataForGetFirewallRepositoryComponents();

    // CHANGE PAGE NUMBER
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(2, 3, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined2ndPage = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantined2ndPage).isNotEmpty();
    assertThat(dao.getTotalFirewallRepositoryComponents(filter)).isEqualTo(4);
    assertThat(autoUnquarantined2ndPage.size()).isEqualTo(1);
    assertComponentForFirewall(autoUnquarantined2ndPage.get(0), "/autoreleased1", june1st2020, june2nd2020, true);
  }

  @Test
  public void testGetFirewallRepositoryComponents_pageDoesNotExist() {
    setupMockDataForGetFirewallRepositoryComponents();

    // SETUP FILTER
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(3, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantined = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantined).isEmpty();
  }

  @Test
  public void testGetFirewallRepositoryComponents_PolicyNotViolated() {
    setupMockDataForGetFirewallRepositoryComponents();

    // FILTER BY UNQUARANTINE_AUTO COMPONENT
    final ArrayList<FirewallFilterField> filterFieldsInvalid = new ArrayList<>();
    filterFieldsInvalid.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID, "policy_5"));
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            filterFieldsInvalid);

    // EXECUTE
    final List<RepositoryComponent> autoUnquarantinedInvalidPolicy = dao.getFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantinedInvalidPolicy).isEmpty();
    assertThat(dao.getTotalFirewallRepositoryComponents(filter)).isZero();
  }

  @Test
  public void testGetFirewallRepositoryComponents_AllExcluded() {
    //Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, null, null, true, Collections.emptyList());

    //When: executing 'getFirewallRepositoryComponents'
    //Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("firewallComponentFilterState is required and cannot be null.");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("firewallComponentFilterState is required and cannot be null.");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AuditWithQuarantineOrder() {
    //Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.AUDIT,
            FirewallSortableField.QUARANTINE_TIME, true, Collections.emptyList());

    //When: executing 'getFirewallRepositoryComponents'
    //Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AuditWithReleaseQuarantineOrder() {
    //Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.AUDIT,
            FirewallSortableField.RELEASE_QUARANTINE_TIME, true, Collections.emptyList());

    //When: executing 'getFirewallRepositoryComponents'
    //Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AllWithQuarantineOrder() {
    //Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.ALL,
            FirewallSortableField.QUARANTINE_TIME, true, Collections.emptyList());

    //When: executing 'getFirewallRepositoryComponents'
    //Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AllWithReleaseQuarantineOrder() {
    //Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.ALL,
            FirewallSortableField.RELEASE_QUARANTINE_TIME, true, Collections.emptyList());

    //When: executing 'getFirewallRepositoryComponents'
    //Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");
  }

  @Test
  public void testGetFirewallRepositoryComponents_QuarantineWithReleaseQuarantineOrder() {
    //Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.RELEASE_QUARANTINE_TIME, true, Collections.emptyList());

    //When: executing 'getFirewallRepositoryComponents'
    //Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field releaseQuarantineTime is not applicable to component state QUARANTINE");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field releaseQuarantineTime is not applicable to component state QUARANTINE");
  }

  @Test
  public void testGetTotalFirewallRepositoryComponents_MultiplePolicyViolations() {
    setupMockDataForGetFirewallRepositoryComponents();

    // FILTER
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.UNQUARANTINE_AUTO, null, true,
            Collections.emptyList());

    // EXECUTE
    final long autoUnquarantinedComponentsCount = dao.getTotalFirewallRepositoryComponents(filter);

    // ASSERTION
    assertThat(autoUnquarantinedComponentsCount).isEqualTo(4);
  }

  private void setupMockDataForGetFirewallRepositoryComponents() {
    // ADD COMPONENT
    tempEntity
        .newRepositoryComponent(repository.getId(), "/autoreleased1", june1st2020, june2nd2020, june8th2020, true);
    final RepositoryComponent component2 =
        tempEntity
            .newRepositoryComponent(repository.getId(), "/autoreleased2", june2nd2020, june3rd2020, june7th2020, true);
    final RepositoryComponent component3 =
        tempEntity
            .newRepositoryComponent(repository.getId(), "/autoreleased3", june3rd2020, june4th2020, june6th2020, true);
    tempEntity
        .newRepositoryComponent(repository.getId(), "/autoreleased4", june4th2020, june5th2020, june5th2020, true);
    final RepositoryComponent component5 =
        tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", june5th2020, null, june4th2020, false);
    final RepositoryComponent component6 =
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
      final RepositoryComponent component,
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

  private static class TestRepositoryComponentDAO
      extends RepositoryComponentDAO
  {
    public static final int PARTITION_THRESHOLD = 2;

    @Override
    public int getInOperatorThreshold() {
      return PARTITION_THRESHOLD;
    }
  }
}
