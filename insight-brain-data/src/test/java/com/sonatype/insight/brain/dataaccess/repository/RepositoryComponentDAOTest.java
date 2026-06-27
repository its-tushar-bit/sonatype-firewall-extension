/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.common.test.SlowTest;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
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
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO.Status;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.utils.DateConverter;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Category(SlowTest.class)
public class RepositoryComponentDAOTest
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

  private RepositoryComponentDAO dao;

  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private Repository repositoryTwo;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryComponentDAO();
    quarantinedComponentAccessDAO = daoFactory.createQuarantinedComponentAccessDAO();
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
    for (int i = 0; i < PARTITION_THRESHOLD + 1; i++) {
      components.add(tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
          ComponentIdentifier.createMavenCoordinates("g", "a", "v" + i, "c", "jar")));
    }

    List<String> pathnames = components.stream()
        .map(RepositoryComponent::getPathname)
        .collect(Collectors.toList());

    List<RepositoryComponent> repositoryComponents =
        dao.getByRepositoryIdAndPathnames(repository.getId(), pathnames);

    assertThat(components).hasSize(PARTITION_THRESHOLD + 1);
    assertThat(repositoryComponents).hasSize(PARTITION_THRESHOLD + 1);
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

    dao.deleteByRepositoryId(null /* TransactionContext */, repository.getId());

    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
    assertThat(quarantinedComponentAccessDAO.getAll()).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testDeleteByRepositoryId_Postgres() {
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

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryId(tx, repository.getId());
      tx.commit();
    }

    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
    assertThat(quarantinedComponentAccessDAO.getAll()).isEmpty();
  }

  // ---- deleteByRepositoryIdAndPathnames (CLM-40943 archive-of-archives fan-out) ----

  @Test
  public void testDeleteByRepositoryIdAndPathnames_deletesMatchingPathnamesOnly() {
    String outerPath = "outer.zip";
    String innerA = outerPath + "!/a.jar";
    String innerB = outerPath + "!/b.jar";
    String unrelated = "other/unrelated.jar";
    tempEntity.newRepositoryComponent(repository.getId(), outerPath);
    tempEntity.newRepositoryComponent(repository.getId(), innerA);
    tempEntity.newRepositoryComponent(repository.getId(), innerB);
    tempEntity.newRepositoryComponent(repository.getId(), unrelated);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryIdAndPathnames(tx, repository.getId(), Arrays.asList(innerA, innerB));
      tx.commit();
    }

    List<RepositoryComponent> remaining = dao.getByRepositoryId(repository.getId());
    assertThat(remaining).extracting(RepositoryComponent::getPathname)
        .containsExactlyInAnyOrder(outerPath, unrelated);
  }

  @Test
  public void testDeleteByRepositoryIdAndPathnames_cascadesQuarantinedComponentAccess() {
    // A row that has a quarantined_component_access entry must still be deleted, and the
    // matching access row must be cleaned up first via the cascade. Without the explicit
    // pre-delete on the access table, the FK from quarantined_component_access ->
    // repository_component would block the parent delete.
    String outerPath = "outer.zip";
    String inner = outerPath + "!/lib.jar";
    tempEntity.newRepositoryComponent(repository.getId(), outerPath);
    RepositoryComponent innerRow = tempEntity.newRepositoryComponent(repository.getId(), inner);
    tempEntity.newQuarantinedComponentAccess(innerRow.getRepositoryId(), innerRow.getId());
    assertThat(quarantinedComponentAccessDAO.getAll()).hasSize(1);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryIdAndPathnames(tx, repository.getId(), Collections.singletonList(inner));
      tx.commit();
    }

    assertThat(dao.getByRepositoryIdAndPathname(repository.getId(), inner)).isNull();
    assertThat(dao.getByRepositoryIdAndPathname(repository.getId(), outerPath)).isNotNull();
    assertThat(quarantinedComponentAccessDAO.getAll()).as("access row cleaned up by cascade").isEmpty();
  }

  @Test
  public void testDeleteByRepositoryIdAndPathnames_deletesAcrossInClauseChunkBoundaries() {
    // The DAO partitions the pathname list into IN-clause chunks of size getInOperatorThreshold().
    // Build a list large enough to span at least two chunks so we exercise the loop, not just a
    // single bulk DELETE. Using threshold + a small overflow keeps the test fast while still
    // crossing the chunk boundary.
    int threshold = dao.getInOperatorThreshold();
    int totalRows = threshold + 5;
    List<String> innerPathnames = new java.util.ArrayList<>(totalRows);
    for (int i = 0; i < totalRows; i++) {
      String p = "outer.zip!/lib-" + i + ".jar";
      innerPathnames.add(p);
      tempEntity.newRepositoryComponent(repository.getId(), p);
    }
    String survivor = "outer.zip!/keeper.jar";
    tempEntity.newRepositoryComponent(repository.getId(), survivor);
    assertThat(dao.getByRepositoryId(repository.getId())).hasSize(totalRows + 1);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryIdAndPathnames(tx, repository.getId(), innerPathnames);
      tx.commit();
    }

    List<RepositoryComponent> remaining = dao.getByRepositoryId(repository.getId());
    assertThat(remaining).extracting(RepositoryComponent::getPathname).containsExactly(survivor);
  }

  @Test
  public void testDeleteByRepositoryIdAndPathnames_isolatesPerRepository() {
    // Same pathname under a sibling repo must NOT be deleted — the WHERE clause keys on
    // (repository_id, pathname) and the IN-clause is correctly bounded by the repository_id
    // predicate, not the pathname alone.
    String shared = "outer.zip!/lib.jar";
    tempEntity.newRepositoryComponent(repository.getId(), shared);
    tempEntity.newRepositoryComponent(repositoryTwo.getId(), shared);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryIdAndPathnames(tx, repository.getId(), Collections.singletonList(shared));
      tx.commit();
    }

    assertThat(dao.getByRepositoryIdAndPathname(repository.getId(), shared)).isNull();
    assertThat(dao.getByRepositoryIdAndPathname(repositoryTwo.getId(), shared))
        .as("sibling repo's row must survive")
        .isNotNull();
  }

  @Test
  public void testDeleteByRepositoryIdAndPathnames_emptyOrNullInputs_noOp() {
    tempEntity.newRepositoryComponent(repository.getId(), "outer.zip");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByRepositoryIdAndPathnames(tx, repository.getId(), Collections.emptyList());
      dao.deleteByRepositoryIdAndPathnames(tx, repository.getId(), null);
      dao.deleteByRepositoryIdAndPathnames(tx, null, Collections.singletonList("outer.zip"));
      tx.commit();
    }

    assertThat(dao.getByRepositoryId(repository.getId())).hasSize(1);
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
        dao.getAutoReleaseQuarantinedCountByRepositoryIdAndDate(repository.getId(), oneYearAgo, false);

    assertThat(results).containsExactlyInAnyOrderEntriesOf(
        ImmutableMap.of(LocalDate.now(), 2L, DateConverter.toLocalDate(oneYearAgo), 1L));
  }

  @Test
  public void testGetAutoReleaseQuarantinedCountByRepositoryIdAndDate_WithExclusiveDate() {
    Repository repository1 = tempEntity.newRepository();
    Date date2020 = new GregorianCalendar(2020, Calendar.MAY, 1).getTime();
    Date date2021 = new GregorianCalendar(2021, Calendar.MAY, 1).getTime();
    Date date2022 = new GregorianCalendar(2022, Calendar.MAY, 1).getTime();
    List<Date> dates = Arrays.asList(date2020, date2021, date2022);
    Map<LocalDate, Long> result = dao.getAutoReleaseQuarantinedCountByRepositoryIdAndDate(
        RepositoryContainer.REPOSITORY_CONTAINER_ID,
        DateConverter.toDate(LocalDate.now()),
        true);
    assertThat(result).isEmpty();
    dates.forEach(date -> {
      tempEntity.newRepositoryComponent(repository1.getId(),
          repository1.getId() + "/" + date.toString(),
          date, date, true);
    });
    // 2020 year exclusive
    result = dao.getAutoReleaseQuarantinedCountByRepositoryIdAndDate(
        repository1.getId(), date2020, true);
    assertThat(result).hasSize(2);
    // 2020 year inclusive
    result = dao.getAutoReleaseQuarantinedCountByRepositoryIdAndDate(
        repository1.getId(), DateUtils.addDays(date2020, -365), true);
    assertThat(result).hasSize(3);
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
  public void testGetTotalFirewallRepositoryComponents_filterByRepoPublicId() {
    setupMockDataForGetFirewallRepositoryComponents();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "/quarantined/test",
        june7th2020, null, june8th2020, false);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, component.getPathname(), false, FailActionType.ID,
        "policy_id", "policy", component.getComponentIdentifier());

    Repository repo2 = tempEntity.newRepository(repositoryManager);
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo2.getId(), "/quarantined/test2",
        june7th2020, null, june8th2020, false);
    tempEntity.newRepositoryPolicyViolation(repo2.getId(), 5, component2.getPathname(), false, FailActionType.ID,
        "policy_id", "policy", component2.getComponentIdentifier());

    // FILTER BY REPOSITORY PUBLIC ID
    List<FirewallFilterField> filterFields = new ArrayList<>();
    filterFields.add(new FirewallFilterField(FirewallFilterableField.REPOSITORY_PUBLIC_ID, repo2.getPublicId()));

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(1, 2,
        FirewallComponentFilterState.QUARANTINE, FirewallSortableField.REPOSITORY_PUBLIC_ID, true, filterFields);

    // EXECUTE
    Long count = dao.getTotalFirewallRepositoryComponents(filter);

    // ASSERTION - only the components with the given repository public ID should be returned
    assertThat(count).isEqualTo(1);
  }

  @Test
  public void testGetTotalFirewallRepositoryComponents_filterByQuarantineDays() {
    // Test #1 - Filter by past 1 day(s). No components should be returned.
    // Setup
    setupMockDataForGetFirewallRepositoryComponents();

    Instant past1Days = Instant.now().minus(1, ChronoUnit.DAYS);
    Instant past7Days = Instant.now().minus(7, ChronoUnit.DAYS);

    Date past7DaysQuarantineTime = Date.from(past7Days);

    Repository repository1 = tempEntity.newRepository(repositoryManager);
    Repository repository2 = tempEntity.newRepository(repositoryManager);

    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repository1.getId(), "/quarantined/test",
        past7DaysQuarantineTime, null, past7DaysQuarantineTime, false);
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repository2.getId(), "/quarantined/test2",
        past7DaysQuarantineTime, null, past7DaysQuarantineTime, false);

    tempEntity.newRepositoryPolicyViolation(repository1.getId(), 5, component1.getPathname(), false, FailActionType.ID,
        "policy_id", "policy", component1.getComponentIdentifier());
    tempEntity.newRepositoryPolicyViolation(repository2.getId(), 5, component2.getPathname(), false, FailActionType.ID,
        "policy_id", "policy", component2.getComponentIdentifier());

    String filterQuarantineTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .format(LocalDateTime.ofInstant(past1Days, ZoneId.systemDefault()));
    List<FirewallFilterField> filterFields =
        List.of(new FirewallFilterField(FirewallFilterableField.QUARANTINE_TIME, filterQuarantineTime));

    FirewallRepositoryComponentFilter filter = new FirewallRepositoryComponentFilter(1, 2,
        FirewallComponentFilterState.QUARANTINE, FirewallSortableField.QUARANTINE_TIME, true, filterFields);

    // Act
    Long count = dao.getTotalFirewallRepositoryComponents(filter);

    assertThat(count).isEqualTo(0);

    // Test #2 - Filter by past 7 day(s). Two components should be returned.
    // Setup
    filterQuarantineTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .format(LocalDateTime.ofInstant(past7Days, ZoneId.systemDefault()));
    filterFields = List.of(new FirewallFilterField(FirewallFilterableField.QUARANTINE_TIME, filterQuarantineTime));
    filter.filterFields = filterFields;

    // Act
    count = dao.getTotalFirewallRepositoryComponents(filter);

    assertThat(count).isEqualTo(2);
  }

  @Test
  public void testGetFirewallRepositoryComponents_filterByComponentName() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent1 = newQuarantinedRepositoryComponent(repository.getId(), "a1");
    newQuarantinedRepositoryComponent(repository.getId(), "a2");

    assertThat(filter(null, "a1")).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactly(repositoryComponent1);
    assertThat(filter(null, "A1")).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
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

    assertThat(filter(null, null)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(repositoryComponent1, repositoryComponent2, repositoryComponent3,
            repositoryComponent4);
    assertThat(filter(policy1.getId(), null))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(repositoryComponent1, repositoryComponent3);
    assertThat(filter(null, "a1")).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(repositoryComponent1, repositoryComponent2);
    assertThat(filter(policy1.getId(), "a1"))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
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
    // Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, null, null, true, Collections.emptyList());

    // When: executing 'getFirewallRepositoryComponents'
    // Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("firewallComponentFilterState is required and cannot be null.");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("firewallComponentFilterState is required and cannot be null.");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AuditWithQuarantineOrder() {
    // Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.AUDIT,
            FirewallSortableField.QUARANTINE_TIME, true, Collections.emptyList());

    // When: executing 'getFirewallRepositoryComponents'
    // Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AuditWithReleaseQuarantineOrder() {
    // Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.AUDIT,
            FirewallSortableField.RELEASE_QUARANTINE_TIME, true, Collections.emptyList());

    // When: executing 'getFirewallRepositoryComponents'
    // Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state AUDIT");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AllWithQuarantineOrder() {
    // Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.ALL,
            FirewallSortableField.QUARANTINE_TIME, true, Collections.emptyList());

    // When: executing 'getFirewallRepositoryComponents'
    // Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");
  }

  @Test
  public void testGetFirewallRepositoryComponents_AllWithReleaseQuarantineOrder() {
    // Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.ALL,
            FirewallSortableField.RELEASE_QUARANTINE_TIME, true, Collections.emptyList());

    // When: executing 'getFirewallRepositoryComponents'
    // Then: expect exception to be thrown
    assertThatThrownBy(() -> dao.getFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");

    assertThatThrownBy(() -> dao.getTotalFirewallRepositoryComponents(filter)).isInstanceOf(BadRequestException.class)
        .hasMessage("Sortable field cannot be specified for component state ALL");
  }

  @Test
  public void testGetFirewallRepositoryComponents_QuarantineWithReleaseQuarantineOrder() {
    // Setup: Filter with component state set to null
    FirewallRepositoryComponentFilter filter =
        new FirewallRepositoryComponentFilter(1, 2, FirewallComponentFilterState.QUARANTINE,
            FirewallSortableField.RELEASE_QUARANTINE_TIME, true, Collections.emptyList());

    // When: executing 'getFirewallRepositoryComponents'
    // Then: expect exception to be thrown
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

  @Test
  public void testGetConsolidatedQuarantinedComponentsMetricByDate() throws ParseException {
    Repository repository1 = tempEntity.newRepository();
    Date date2020 = DateUtils.parseDate("2020-05-01", "yyyy-MM-dd");
    Date date2021 = DateUtils.parseDate("2021-05-01", "yyyy-MM-dd");
    Date date2022 = DateUtils.parseDate("2022-05-01", "yyyy-MM-dd");
    List<Date> dates = Arrays.asList(date2020, date2021, date2022);
    Map<LocalDate, Long> result = dao.getConsolidatedQuarantinedComponentsMetricByDate(
        DateConverter.toDate(LocalDate.now()));
    assertThat(result).isEmpty();
    dates.forEach(date -> {
      tempEntity.newRepositoryComponent(repository1.getId(),
          repository1.getId() + "/" + date.toString(),
          date, null);
    });
    // 2020 year exclusive
    result = dao.getConsolidatedQuarantinedComponentsMetricByDate(date2020);
    assertThat(result).hasSize(2);
    // 2020 year inclusive
    result = dao.getConsolidatedQuarantinedComponentsMetricByDate(DateUtils.addDays(date2020, -365));
    assertThat(result).hasSize(3);
  }

  @Test
  public void testGetRepositoryToComponentsByHash() {
    // Setup test data
    String targetHash = "test_hash_123";
    String otherHash = "other_hash_456";
    Date now = new Date();

    // Create repositories
    Repository repo1 = tempEntity.newRepository();
    Repository repo2 = tempEntity.newRepository();
    Repository repo3 = tempEntity.newRepository();

    // Create components with target hash in repo1 and repo2
    RepositoryComponent component1InRepo1 = tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "/path1", targetHash,
        ComponentIdentifier.createMavenCoordinates("group1", "artifact1", "1.0.0"), now, now);

    RepositoryComponent component2InRepo1 = tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "/path2", targetHash,
        ComponentIdentifier.createMavenCoordinates("group2", "artifact2", "1.0.0"), now, now);

    RepositoryComponent componentInRepo2 = tempEntity.newRepositoryComponent(repo2.getId(),
        MatchState.EXACT, "/path3", targetHash,
        ComponentIdentifier.createNpmCoordinates("package1", "2.0.0"), now, now);

    // Create component with different hash (should not be returned)
    tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "/path4", otherHash,
        ComponentIdentifier.createMavenCoordinates("group3", "artifact3", "1.0.0"), now, now);

    // Create component in repo3 with different hash (should not be returned)
    tempEntity.newRepositoryComponent(repo3.getId(),
        MatchState.EXACT, "/path5", otherHash,
        ComponentIdentifier.createMavenCoordinates("group4", "artifact4", "1.0.0"), now, now);

    // Execute test with transaction context
    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<Repository, List<RepositoryComponent>> result = dao.getRepositoryToComponentsByHash(tx, targetHash);

      // Assertions
      assertThat(result).hasSize(2); // Only repo1 and repo2 should be returned
      assertThat(result.keySet()).extracting(Repository::getId).containsExactlyInAnyOrder(repo1.getId(), repo2.getId());

      // Verify repo1 has 2 components
      Repository foundRepo1 = result.keySet()
          .stream()
          .filter(r -> r.getId().equals(repo1.getId()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Repository 1 not found in results"));
      List<RepositoryComponent> repo1Components = result.get(foundRepo1);
      assertThat(repo1Components).hasSize(2);
      assertThat(repo1Components).extracting(RepositoryComponent::getId)
          .containsExactlyInAnyOrder(component1InRepo1.getId(), component2InRepo1.getId());
      assertThat(repo1Components).allMatch(c -> c.getHash().equals(targetHash));

      // Verify repo2 has 1 component
      Repository foundRepo2 = result.keySet()
          .stream()
          .filter(r -> r.getId().equals(repo2.getId()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Repository 2 not found in results"));
      List<RepositoryComponent> repo2Components = result.get(foundRepo2);
      assertThat(repo2Components).hasSize(1);
      assertThat(repo2Components.get(0).getId()).isEqualTo(componentInRepo2.getId());
      assertThat(repo2Components.get(0).getHash()).isEqualTo(targetHash);

      // Verify repo3 is not in results (no matching hash)
      assertThat(result.keySet()).extracting(Repository::getId).doesNotContain(repo3.getId());
    }
  }

  @Test
  public void testGetRepositoryToComponentsByHash_NoComponentsFound() {
    String nonExistentHash = "non_existent_hash";

    // Create some repositories with components but with different hashes
    Repository repo1 = tempEntity.newRepository();
    tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT, "/path1", "different_hash",
        ComponentIdentifier.createMavenCoordinates("group1", "artifact1", "1.0.0"), new Date(), new Date());

    // Execute test
    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<Repository, List<RepositoryComponent>> result = dao.getRepositoryToComponentsByHash(tx, nonExistentHash);

      // Assertions
      assertThat(result).isEmpty();
    }
  }

  @Test
  public void testGetRepositoryToComponentsByHash_SingleRepositoryMultipleComponents() {
    String targetHash = "single_repo_hash";
    Date now = new Date();
    Repository repo = tempEntity.newRepository();

    // Create multiple components with same hash in single repository
    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo.getId(),
        MatchState.EXACT, "/path1", targetHash,
        ComponentIdentifier.createMavenCoordinates("group1", "artifact1", "1.0.0"), now, now);

    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo.getId(),
        MatchState.EXACT, "/path2", targetHash,
        ComponentIdentifier.createNpmCoordinates("package1", "2.0.0"), now, now);

    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repo.getId(),
        MatchState.EXACT, "/path3", targetHash,
        ComponentIdentifier.createMavenCoordinates("group2", "artifact2", "3.0.0"), now, now);

    // Execute test
    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<Repository, List<RepositoryComponent>> result = dao.getRepositoryToComponentsByHash(tx, targetHash);

      // Assertions
      assertThat(result).hasSize(1);
      assertThat(result.keySet()).extracting(Repository::getId).containsExactly(repo.getId());

      List<RepositoryComponent> components = result.values().iterator().next();
      assertThat(components).hasSize(3);
      assertThat(components).extracting(RepositoryComponent::getId)
          .containsExactlyInAnyOrder(component1.getId(), component2.getId(), component3.getId());
      assertThat(components).allMatch(c -> c.getHash().equals(targetHash));
    }
  }

  @Test
  public void testGetRepositoryToComponentsByHash_QuarantinedAndNonQuarantined() {
    String targetHash = "quarantine_test_hash";
    Date now = new Date();
    Date quarantineTime = new Date();
    Repository repo = tempEntity.newRepository();

    // Create quarantined and non-quarantined components with same hash
    RepositoryComponent quarantinedComponent = tempEntity.newRepositoryComponent(repo.getId(),
        MatchState.EXACT, "/quarantined", targetHash,
        ComponentIdentifier.createMavenCoordinates("group1", "artifact1", "1.0.0"), now, quarantineTime);

    RepositoryComponent nonQuarantinedComponent = tempEntity.newRepositoryComponent(repo.getId(),
        MatchState.EXACT, "/not-quarantined", targetHash,
        ComponentIdentifier.createMavenCoordinates("group2", "artifact2", "1.0.0"), now, now);
    // Explicitly set non-quarantined (null quarantine time)
    nonQuarantinedComponent.setQuarantineTime(null);
    dao.update(nonQuarantinedComponent);

    // Execute test
    try (TransactionContext tx = dao.createTransactionContext()) {
      Map<Repository, List<RepositoryComponent>> result = dao.getRepositoryToComponentsByHash(tx, targetHash);

      // Assertions
      assertThat(result).hasSize(1);
      List<RepositoryComponent> components = result.values().iterator().next();
      assertThat(components).hasSize(2);

      // Verify both quarantined and non-quarantined components are returned
      assertThat(components).extracting(RepositoryComponent::getId)
          .containsExactlyInAnyOrder(quarantinedComponent.getId(), nonQuarantinedComponent.getId());

      // Verify quarantine status is preserved
      RepositoryComponent foundQuarantined = components.stream()
          .filter(c -> c.getId().equals(quarantinedComponent.getId()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Quarantined component not found"));
      assertThat(foundQuarantined.getQuarantineTime()).isNotNull();

      RepositoryComponent foundNonQuarantined = components.stream()
          .filter(c -> c.getId().equals(nonQuarantinedComponent.getId()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Non-quarantined component not found"));
      assertThat(foundNonQuarantined.getQuarantineTime()).isNull();
    }
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

  @Test
  public void testGetOtherVersionRepositoryComponentsByPathnameFilter_ExcludesPreCachedVersionsWithViolations() {
    Date now = new Date();
    String repositoryId = repository.getId();
    ComponentIdentifier v1 = ComponentIdentifier.createMavenCoordinates("com.example", "library", "1.0.0", null, "jar");
    ComponentIdentifier v2 = ComponentIdentifier.createMavenCoordinates("com.example", "library", "2.0.0", null, "jar");
    ComponentIdentifier v3 = ComponentIdentifier.createMavenCoordinates("com.example", "library", "3.0.0", null, "jar");
    ComponentIdentifier v4 = ComponentIdentifier.createMavenCoordinates("com.example", "library", "4.0.0", null, "jar");

    RepositoryComponent quarantined = tempEntity.newRepositoryComponent(
        repositoryId,
        MatchState.EXACT,
        "com/example/library/1.0.0/library-1.0.0.jar",
        "hash1",
        v1,
        now,
        now,
        null);

    // Pre-cached with active violations - should be excluded
    RepositoryComponent preCachedWithViolation = tempEntity.newRepositoryComponent(
        repositoryId,
        MatchState.EXACT,
        "com/example/library/2.0.0/library-2.0.0.jar",
        "hash2",
        v2,
        now,
        null,
        null);

    tempEntity.newRepositoryPolicyViolation(
        repositoryId,
        8,
        preCachedWithViolation.getPathname(),
        false,
        "fail",
        "policy-id",
        "Security Policy",
        v2,
        now);

    RepositoryComponent safeComponent = tempEntity.newRepositoryComponent(
        repositoryId,
        MatchState.EXACT,
        "com/example/library/3.0.0/library-3.0.0.jar",
        "hash3",
        v3,
        now,
        null,
        null);

    RepositoryComponent unquarantined = tempEntity.newRepositoryComponent(
        repositoryId,
        MatchState.EXACT,
        "com/example/library/4.0.0/library-4.0.0.jar",
        "hash4",
        v4,
        now,
        DateUtils.addDays(now, -1),
        now);

    List<RepositoryComponent> result = dao.getOtherVersionRepositoryComponentsByPathnameFilter(
        repositoryId,
        "com/example/library/",
        quarantined.getPathname());

    assertThat(result).hasSize(2);
    assertThat(result).extracting(RepositoryComponent::getId)
        .containsExactlyInAnyOrder(safeComponent.getId(), unquarantined.getId());
    assertThat(result).extracting(RepositoryComponent::getId)
        .doesNotContain(preCachedWithViolation.getId(), quarantined.getId());
  }

  @Test
  public void testGetOtherVersionRepositoryComponentsByPathnameFilter_IncludesPreCachedVersionsWithWaivedViolations() {
    Date now = new Date();
    String repositoryId = repository.getId();
    ComponentIdentifier v1 = ComponentIdentifier.createMavenCoordinates("com.example", "lib", "1.0.0", null, "jar");
    ComponentIdentifier v2 = ComponentIdentifier.createMavenCoordinates("com.example", "lib", "2.0.0", null, "jar");

    RepositoryComponent quarantined = tempEntity.newRepositoryComponent(
        repositoryId,
        MatchState.EXACT,
        "com/example/lib/1.0.0/lib-1.0.0.jar",
        "hash1",
        v1,
        now,
        now,
        null);

    // Pre-cached with waived violations - should be included
    RepositoryComponent preCachedWithWaivedViolation = tempEntity.newRepositoryComponent(
        repositoryId,
        MatchState.EXACT,
        "com/example/lib/2.0.0/lib-2.0.0.jar",
        "hash2",
        v2,
        now,
        null,
        null);

    tempEntity.newRepositoryPolicyViolation(
        repositoryId,
        8,
        preCachedWithWaivedViolation.getPathname(),
        true,
        "fail",
        "policy-id",
        "Security Policy",
        v2,
        now);

    List<RepositoryComponent> result = dao.getOtherVersionRepositoryComponentsByPathnameFilter(
        repositoryId,
        "com/example/lib/",
        quarantined.getPathname());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(preCachedWithWaivedViolation.getId());
  }

  @Test
  public void testGetLastScanTimesByRepositoryIds_ReturnsMaxEvaluationTimePerRepository() {
    Date earlier = Date.from(Instant.now().minusSeconds(3600));
    Date later = new Date();

    Repository repo1 = tempEntity.newRepository();
    Repository repo2 = tempEntity.newRepository();

    tempEntity.newRepositoryComponent(repo1.getId(), earlier);
    tempEntity.newRepositoryComponent(repo1.getId(), later);
    tempEntity.newRepositoryComponent(repo2.getId(), earlier);

    Map<String, Date> result = dao.getLastScanTimesByRepositoryIds(List.of(repo1.getId(), repo2.getId()));

    assertThat(result).containsKey(repo1.getId());
    assertThat(result).containsKey(repo2.getId());
    assertThat(result.get(repo1.getId())).isEqualTo(later);
    assertThat(result.get(repo2.getId())).isEqualTo(earlier);
  }

  @Test
  public void testGetLastScanTimesByRepositoryIds_ReturnsEmptyMapForEmptyInput() {
    assertThat(dao.getLastScanTimesByRepositoryIds(Collections.emptyList())).isEmpty();
    assertThat(dao.getLastScanTimesByRepositoryIds(null)).isEmpty();
  }

  @Test
  public void testGetLastScanTimesByRepositoryIds_ExcludesRepositoriesWithNoComponents() {
    Repository repo = tempEntity.newRepository();
    // Repository with no components — should not appear in the result map

    Map<String, Date> result = dao.getLastScanTimesByRepositoryIds(List.of(repo.getId()));

    assertThat(result).doesNotContainKey(repo.getId());
  }

  @Test
  public void testGetRepositoryIdsWithQueuedScans_ReturnsPendingAndInProgressRepositories() {
    Repository repo1 = tempEntity.newRepository();
    Repository repo2 = tempEntity.newRepository();
    Repository repo3 = tempEntity.newRepository();

    RepositoryComponent component1 = tempEntity.newRepositoryComponent(repo1.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"));
    RepositoryComponent component2 = tempEntity.newRepositoryComponent(repo2.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "2.0"));
    RepositoryComponent component3 = tempEntity.newRepositoryComponent(repo3.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "3.0"));

    tempEntity.newHostedComponentScanQueue(component1.getId(), repo1.getId(), Status.PENDING.name());
    tempEntity.newHostedComponentScanQueue(component2.getId(), repo2.getId(), Status.IN_PROGRESS.name());
    tempEntity.newHostedComponentScanQueue(component3.getId(), repo3.getId(), Status.COMPLETED.name());

    Set<String> result = dao.getRepositoryIdsWithQueuedScans(
        List.of(repo1.getId(), repo2.getId(), repo3.getId()));

    assertThat(result).containsExactlyInAnyOrder(repo1.getId(), repo2.getId());
    assertThat(result).doesNotContain(repo3.getId());
  }

  @Test
  public void testGetRepositoryIdsWithQueuedScans_ReturnsEmptySetForEmptyInput() {
    assertThat(dao.getRepositoryIdsWithQueuedScans(Collections.emptyList())).isEmpty();
    assertThat(dao.getRepositoryIdsWithQueuedScans(null)).isEmpty();
  }

  @Test
  public void testStampComponentId_SetsComponentIdOnMatchingComponent() {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"));
    assertThat(component.getComponentId()).isNull();

    String componentId = "test-component-id-123";
    try (TransactionContext tx = dao.createTransactionContext()) {
      dao.stampComponentId(tx, repository.getId(), component.getPathname(), componentId);
    }

    RepositoryComponent updated = dao.getById(component.getId());
    assertThat(updated.getComponentId()).isEqualTo(componentId);
  }

  @Test
  public void testStampComponentId_DoesNotAffectOtherComponents() {
    RepositoryComponent target = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "1.0"));
    RepositoryComponent other = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "b", "1.0"));

    try (TransactionContext tx = dao.createTransactionContext()) {
      dao.stampComponentId(tx, repository.getId(), target.getPathname(), "stamped-id");
    }

    assertThat(dao.getById(other.getId()).getComponentId()).isNull();
  }

  @Test
  public void testGetByRepositoryIdPaged_ReturnsPaginatedResults() {
    for (int i = 0; i < 5; i++) {
      tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
          ComponentIdentifier.createMavenCoordinates("g", "a", "v" + i));
    }

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page1 = dao.getByRepositoryId(tx, repository.getId(), 3, 0);
      List<RepositoryComponent> page2 = dao.getByRepositoryId(tx, repository.getId(), 3, 3);

      assertThat(page1).hasSize(3);
      assertThat(page2).hasSize(2);

      List<String> allIds = new ArrayList<>();
      page1.forEach(c -> allIds.add(c.getId()));
      page2.forEach(c -> allIds.add(c.getId()));
      assertThat(allIds).doesNotHaveDuplicates();
    }
  }

  @Test
  public void testGetByRepositoryIdPaged_ReturnsEmptyForOutOfBoundsOffset() {
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v1"));

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> result = dao.getByRepositoryId(tx, repository.getId(), 10, 100);
      assertThat(result).isEmpty();
    }
  }

  // ----------------------------------------------------------------------
  // CLM-40039 §6.1 — getMonitoringEligiblePage: eligibility filter + dedup
  // by (repository_id, hash) + globally newest-first emission.
  // Covers AT-001 / AT-002 / AT-003 plus the dedup + newest-first behavior
  // introduced by the audit-finding fix to RepositoryComponentDAO.
  // ----------------------------------------------------------------------

  /**
   * AT-002 — components with {@code last_evaluation_time >= cycleStart} are excluded so the
   * cycle does not re-evaluate work it just did.
   */
  @Test
  public void getMonitoringEligiblePage_excludesComponentsEvaluatedAtOrAfterCycleStart() {
    Repository hostedRepo = tempEntity.newHostedRepository(repositoryManager, uuid("repo"), "maven2", false);
    Instant cycle = Instant.now();
    Date cycleStart = Date.from(cycle);
    Date afterCycle = Date.from(cycle.plusSeconds(60));
    Date beforeCycle = Date.from(cycle.minusSeconds(60));

    RepositoryComponent stale = newComponentWithEvalTime(hostedRepo.getId(), "/path/old.jar", "hash-old", beforeCycle);
    newComponentWithEvalTime(hostedRepo.getId(), "/path/fresh.jar", "hash-fresh", afterCycle);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page = dao.getMonitoringEligiblePage(tx, cycleStart, 100, (EligibilityCursor) null);
      assertThat(page).extracting(RepositoryComponent::getId).containsExactly(stale.getId());
    }
  }

  /**
   * AT-003 — only repositories with {@code repository_type='hosted'} AND
   * {@code monitoring_enabled=TRUE} contribute candidates.
   */
  @Test
  public void getMonitoringEligiblePage_excludesNonHostedAndMonitoringDisabledRepositories() {
    Instant cycle = Instant.now();
    Date cycleStart = Date.from(cycle);
    Date past = Date.from(cycle.minusSeconds(60));

    // Hosted, monitoring enabled — eligible.
    Repository eligible = tempEntity.newHostedRepository(repositoryManager, uuid("eligible"), "maven2", false);
    RepositoryComponent eligibleRc = newComponentWithEvalTime(eligible.getId(), "/p/eligible.jar", "h-elig", past);

    // Hosted, monitoring DISABLED — excluded.
    Repository disabled = tempEntity.newHostedRepository(repositoryManager, uuid("disabled"), "maven2", false);
    disabled.setMonitoringEnabled(false);
    daoFactory.createRepositoryDAO().update(disabled);
    newComponentWithEvalTime(disabled.getId(), "/p/disabled.jar", "h-disab", past);

    // Proxy repo (default newRepository is proxy) — excluded.
    newComponentWithEvalTime(repository.getId(), "/p/proxy.jar", "h-proxy", past);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page = dao.getMonitoringEligiblePage(tx, cycleStart, 100, (EligibilityCursor) null);
      assertThat(page).extracting(RepositoryComponent::getId).containsExactly(eligibleRc.getId());
    }
  }

  /**
   * Dedup behavior — multiple {@code repository_component} rows sharing the same
   * {@code (repository_id, hash)} pair (e.g. same jar at different pathnames) collapse to a
   * single representative. Without this, parent queue rows would orphan against the
   * satellite UNIQUE constraint on consume.
   */
  @Test
  public void getMonitoringEligiblePage_dedupsByRepositoryIdAndHash() {
    Repository hostedRepo = tempEntity.newHostedRepository(repositoryManager, uuid("repo"), "maven2", false);
    Date cycleStart = new Date();
    Date past = new Date(cycleStart.getTime() - 60_000L);

    // Three rows in the same repo with the same hash but different pathnames.
    newComponentWithEvalTime(hostedRepo.getId(), "/a/lib.jar", "shared-hash", past);
    newComponentWithEvalTime(hostedRepo.getId(), "/b/lib.jar", "shared-hash", past);
    newComponentWithEvalTime(hostedRepo.getId(), "/c/lib.jar", "shared-hash", past);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page = dao.getMonitoringEligiblePage(tx, cycleStart, 100, (EligibilityCursor) null);
      assertThat(page).hasSize(1);
      assertThat(page).extracting(RepositoryComponent::getRepositoryId, RepositoryComponent::getHash)
          .containsExactly(tuple(hostedRepo.getId(), "shared-hash"));
    }
  }

  /**
   * Within each {@code (repository_id, hash)} group, the row with the most recent
   * {@code repository_component.time} wins as the representative — the design's "newest-first"
   * intent operating at the dedup-group level.
   */
  @Test
  public void getMonitoringEligiblePage_picksMostRecentRowAsRepresentativePerGroup() {
    Repository hostedRepo = tempEntity.newHostedRepository(repositoryManager, uuid("repo"), "maven2", false);
    Instant cycle = Instant.now();
    Date cycleStart = Date.from(cycle);
    Date oldest = Date.from(cycle.minusSeconds(600));
    Date middle = Date.from(cycle.minusSeconds(400));
    Date newest = Date.from(cycle.minusSeconds(200));

    RepositoryComponent oldestRow = newComponentWithTimeAndHash(hostedRepo.getId(), "/a", "h", oldest);
    RepositoryComponent middleRow = newComponentWithTimeAndHash(hostedRepo.getId(), "/b", "h", middle);
    RepositoryComponent newestRow = newComponentWithTimeAndHash(hostedRepo.getId(), "/c", "h", newest);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page = dao.getMonitoringEligiblePage(tx, cycleStart, 100, (EligibilityCursor) null);
      assertThat(page).hasSize(1);
      assertThat(page.get(0).getId()).isEqualTo(newestRow.getId());
      // Sanity: the older rows do exist in the table; they were filtered, not deleted.
      assertThat(dao.getById(oldestRow.getId())).isNotNull();
      assertThat(dao.getById(middleRow.getId())).isNotNull();
    }
  }

  /**
   * Two rows sharing an exact-millisecond TIME within the same (repository_id, hash) group must
   * resolve via the secondary {@code repository_component_id DESC} tiebreaker — the higher id
   * wins (CLM-41005). This protects both dialect paths from a future "simplification" that
   * drops the secondary term from the row-value comparison: the Postgres path's NOT EXISTS
   * anti-join uses {@code (time, id) > (rc.time, rc.id)} and the H2 path's third-pass uses
   * {@code MAX(repository_component_id)} on rows tied at MAX(TIME).
   */
  @Test
  public void getMonitoringEligiblePage_picksHigherIdOnExactTimeCollision() {
    Repository hostedRepo = tempEntity.newHostedRepository(repositoryManager, uuid("repo"), "maven2", false);
    Instant cycle = Instant.now();
    Date cycleStart = Date.from(cycle);
    // Both rows: same repo + same hash + same time. Only repository_component_id differs (the PK
    // is assigned sequentially by TemporaryEntity, so the second insert gets a lexicographically
    // greater id — the test asserts on that property, not on numeric ordering, to stay robust to
    // any id-generation scheme.)
    Date sharedTime = Date.from(cycle.minusSeconds(100));
    RepositoryComponent firstInsert = newComponentWithTimeAndHash(hostedRepo.getId(), "/a", "h", sharedTime);
    RepositoryComponent secondInsert = newComponentWithTimeAndHash(hostedRepo.getId(), "/b", "h", sharedTime);

    String winnerId = firstInsert.getId().compareTo(secondInsert.getId()) > 0
        ? firstInsert.getId()
        : secondInsert.getId();

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page = dao.getMonitoringEligiblePage(tx, cycleStart, 100, (EligibilityCursor) null);
      assertThat(page).hasSize(1);
      assertThat(page.get(0).getId()).isEqualTo(winnerId);
    }
  }

  /**
   * Across repositories, the deduped result is emitted globally newest-first by
   * {@code rc.time DESC} so a freshly-uploaded artifact in any repo is picked up before
   * older ones (design doc §6.1, refined for hosted repo).
   */
  @Test
  public void getMonitoringEligiblePage_emitsGloballyNewestFirstAcrossRepositories() {
    Instant cycle = Instant.now();
    Date cycleStart = Date.from(cycle);
    // Three repos whose newest rc.time differs. Naming chosen so alphabetical
    // ordering by repository_id would NOT match the expected newest-first emission.
    Repository repoA = tempEntity.newHostedRepository(repositoryManager, "aaa-" + uuid("r"), "maven2", false);
    Repository repoB = tempEntity.newHostedRepository(repositoryManager, "bbb-" + uuid("r"), "maven2", false);
    Repository repoC = tempEntity.newHostedRepository(repositoryManager, "ccc-" + uuid("r"), "maven2", false);

    Date oldTime = Date.from(cycle.minusSeconds(900));
    Date midTime = Date.from(cycle.minusSeconds(600));
    Date newTime = Date.from(cycle.minusSeconds(300));

    RepositoryComponent inA = newComponentWithTimeAndHash(repoA.getId(), "/a", "h-a", oldTime);
    RepositoryComponent inB = newComponentWithTimeAndHash(repoB.getId(), "/b", "h-b", newTime);
    RepositoryComponent inC = newComponentWithTimeAndHash(repoC.getId(), "/c", "h-c", midTime);

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page = dao.getMonitoringEligiblePage(tx, cycleStart, 100, (EligibilityCursor) null);
      assertThat(page).extracting(RepositoryComponent::getId)
          .containsExactly(inB.getId(), inC.getId(), inA.getId());
    }
  }

  /**
   * AT-001 (CLM-41005) — eligibility query is page-aware via keyset cursor: limit + cursor on
   * (time DESC, repository_component_id DESC) slice the deduped, ordered set without skipping
   * or duplicating rows under concurrent writes.
   */
  @Test
  public void getMonitoringEligiblePage_paginatesDedupedNewestFirstResult() {
    Instant cycle = Instant.now();
    Date cycleStart = Date.from(cycle);
    Repository hostedRepo = tempEntity.newHostedRepository(repositoryManager, uuid("repo"), "maven2", false);

    // 4 distinct (repo, hash) pairs, ordered newest-first by time.
    RepositoryComponent first = newComponentWithTimeAndHash(hostedRepo.getId(), "/p1", "h1",
        Date.from(cycle.minusSeconds(100)));
    RepositoryComponent second = newComponentWithTimeAndHash(hostedRepo.getId(), "/p2", "h2",
        Date.from(cycle.minusSeconds(200)));
    RepositoryComponent third = newComponentWithTimeAndHash(hostedRepo.getId(), "/p3", "h3",
        Date.from(cycle.minusSeconds(300)));
    RepositoryComponent fourth = newComponentWithTimeAndHash(hostedRepo.getId(), "/p4", "h4",
        Date.from(cycle.minusSeconds(400)));

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page1 = dao.getMonitoringEligiblePage(tx, cycleStart, 2, null);
      assertThat(page1).extracting(RepositoryComponent::getId).containsExactly(first.getId(), second.getId());

      EligibilityCursor cursor = new EligibilityCursor(second.getTime(), second.getId());
      List<RepositoryComponent> page2 = dao.getMonitoringEligiblePage(tx, cycleStart, 2, cursor);
      assertThat(page2).extracting(RepositoryComponent::getId).containsExactly(third.getId(), fourth.getId());
    }
  }

  /**
   * AT-002 (CLM-41005) — the primary correctness benefit of keyset over OFFSET: a row inserted
   * between the cursor and the eligibility tail does NOT shift pagination and does NOT cause a
   * skip. With OFFSET, an insert between pages would push later rows past the offset window and
   * leak them out of the cycle. With keyset on {@code (time, repository_component_id)} the
   * predicate is independent of position, so the inserted row joins the result set in its proper
   * order and no eligible row is lost.
   */
  @Test
  public void getMonitoringEligiblePage_keysetSkipsNoRowsUnderConcurrentInsert() {
    Instant cycle = Instant.now();
    Date cycleStart = Date.from(cycle);
    Repository hostedRepo = tempEntity.newHostedRepository(repositoryManager, uuid("repo"), "maven2", false);

    // 4 rows ordered newest-first by time.
    RepositoryComponent r4 = newComponentWithTimeAndHash(hostedRepo.getId(), "/p4", "h4",
        Date.from(cycle.minusSeconds(100)));
    RepositoryComponent r3 = newComponentWithTimeAndHash(hostedRepo.getId(), "/p3", "h3",
        Date.from(cycle.minusSeconds(200)));
    RepositoryComponent r2 = newComponentWithTimeAndHash(hostedRepo.getId(), "/p2", "h2",
        Date.from(cycle.minusSeconds(300)));
    RepositoryComponent r1 = newComponentWithTimeAndHash(hostedRepo.getId(), "/p1", "h1",
        Date.from(cycle.minusSeconds(400)));

    try (TransactionContext tx = dao.createTransactionContext()) {
      List<RepositoryComponent> page1 = dao.getMonitoringEligiblePage(tx, cycleStart, 2, null);
      assertThat(page1).extracting(RepositoryComponent::getId).containsExactly(r4.getId(), r3.getId());

      // Simulate a concurrent insert at time=cycle-250s — strictly between r3 and r2 in the DESC
      // order. Under OFFSET pagination this would push r1 outside the second page's window; under
      // keyset the cursor predicate is on the (time, id) tuple, not on row position.
      RepositoryComponent inserted = newComponentWithTimeAndHash(hostedRepo.getId(), "/p-concurrent",
          "h-concurrent", Date.from(cycle.minusSeconds(250)));

      EligibilityCursor cursor = new EligibilityCursor(r3.getTime(), r3.getId());
      List<RepositoryComponent> page2 = dao.getMonitoringEligiblePage(tx, cycleStart, 10, cursor);
      // The concurrently inserted row joins the result set in DESC (time, id) order; r2 and r1
      // remain — no row skipped, no row duplicated.
      assertThat(page2).extracting(RepositoryComponent::getId)
          .containsExactly(inserted.getId(), r2.getId(), r1.getId());
    }
  }

  // -- helpers ---------------------------------------------------------------

  private RepositoryComponent newComponentWithEvalTime(
      String repositoryId,
      String pathname,
      String hash,
      Date evalTime)
  {
    return newComponentWith(repositoryId, pathname, hash, evalTime, evalTime);
  }

  private RepositoryComponent newComponentWithTimeAndHash(
      String repositoryId,
      String pathname,
      String hash,
      Date rcTime)
  {
    // last_evaluation_time stays in the past so the row is eligible; rc.time controls emission order.
    Date pastEval = new Date(rcTime.getTime() - 3600_000L);
    return newComponentWith(repositoryId, pathname, hash, rcTime, pastEval);
  }

  private RepositoryComponent newComponentWith(
      String repositoryId,
      String pathname,
      String hash,
      Date rcTime,
      Date lastEvalTime)
  {
    RepositoryComponent rc = new RepositoryComponent(
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
