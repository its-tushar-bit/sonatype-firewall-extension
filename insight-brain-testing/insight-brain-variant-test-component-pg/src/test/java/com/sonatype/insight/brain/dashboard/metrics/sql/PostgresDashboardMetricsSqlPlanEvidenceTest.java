/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;
import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.APPLICATIONS;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.ORGANIZATIONS;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.POLICIES;
import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.Metric.VIOLATIONS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual PostgreSQL plan evidence for the exact dashboard metric DAO query shapes.
 *
 * <p>
 * Run one method at a time with {@code -Ddashboard.sql.planEvidence=true}. Normal builds skip all methods.
 * </p>
 */
@ComponentPgTest
public class PostgresDashboardMetricsSqlPlanEvidenceTest
    extends AbstractComponentPgTest
{
  private static final String OPT_IN_PROPERTY = "dashboard.sql.planEvidence";

  private static final int ESTATE_SIZE = 10_000;

  private static final List<Integer> GRANT_SIZES = List.of(10, 500, 5_000);

  private static final int RUNTIME_CHUNK_SIZE = AbstractSqlDAO.POSTGRES_IN_OPERATOR_THRESHOLD - 100;

  private static final int FIXED_VIOLATION_COUNT = 30_000;

  private static final String PREFIX = "task9-plan-";

  private static final long ADVISORY_LOCK_KEY = 0x44415348504C414EL;

  private static final Pattern INDEX_PATTERN =
      Pattern.compile("(?:Index(?: Only)? Scan using|Bitmap Index Scan on) ([^ ]+)");

  @Inject
  private OperationalDataStore operationalDataStore;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  public void requireExplicitOptIn() {
    Assumptions.assumeTrue(
        Boolean.getBoolean(OPT_IN_PROPERTY),
        "Manual EXPLAIN evidence requires -D" + OPT_IN_PROPERTY + "=true");
  }

  private void purgeEvidenceFixture() throws Exception {
    execute("DELETE FROM " + table("policy_violation") + " WHERE policy_violation_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("policy") + " WHERE policy_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("application") + " WHERE application_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("organization") + " WHERE organization_id LIKE '" + PREFIX + "%'");
  }

  @Test
  public void explainOrganizationsGlobalAndRestricted() throws Exception {
    withEvidenceLock(() -> {
      seedOrganizations();
      writeFixture("organizations", "organization", "organization_id");

      CapturedDaoCall<Long> global = capture(() -> organizationDAO.selectCountByOrganizationIds(null));
      assertThat(global.result()).isGreaterThanOrEqualTo(ESTATE_SIZE);
      explainCaptured(ORGANIZATIONS, "organizations-global", global.statements(), false, 0);
      for (int grantSize : GRANT_SIZES) {
        Set<String> ids = ids("org-", grantSize);
        CapturedDaoCall<Long> restricted = capture(() -> organizationDAO.selectCountByOrganizationIds(ids));
        assertThat(restricted.result()).isEqualTo(grantSize);
        explainCaptured(
            ORGANIZATIONS, "organizations-grants-" + grantSize, restricted.statements(), true, grantSize);
      }
    });
  }

  @Test
  public void explainApplicationsAtGrantSizes() throws Exception {
    withEvidenceLock(() -> {
      seedApplications();
      writeFixture("applications", "application", "application_id");

      CapturedDaoCall<Long> global = capture(() -> applicationDAO.selectCountByApplicationIds(null));
      assertThat(global.result()).isGreaterThanOrEqualTo(ESTATE_SIZE);
      explainCaptured(APPLICATIONS, "applications-global", global.statements(), false, 0);
      for (int grantSize : GRANT_SIZES) {
        Set<String> ids = ids("app-", grantSize);
        CapturedDaoCall<Long> restricted = capture(() -> applicationDAO.selectCountByApplicationIds(ids));
        assertThat(restricted.result()).isEqualTo(grantSize);
        explainCaptured(APPLICATIONS, "applications-grants-" + grantSize, restricted.statements(), true, grantSize);
      }
    });
  }

  @Test
  public void explainPoliciesAtGrantSizes() throws Exception {
    withEvidenceLock(() -> {
      seedPolicies();
      writeFixture("policies", "policy", "policy_id");

      CapturedDaoCall<Long> global = capture(() -> policyDAO.selectCountByOwnerIds(null));
      assertThat(global.result()).isGreaterThanOrEqualTo(ESTATE_SIZE);
      explainCaptured(POLICIES, "policies-global", global.statements(), false, 0);
      for (int grantSize : GRANT_SIZES) {
        Set<String> ownerIds = ids("org-", grantSize);
        CapturedDaoCall<Long> restricted = capture(() -> policyDAO.selectCountByOwnerIds(ownerIds));
        assertThat(restricted.result()).isEqualTo(grantSize);
        explainCaptured(POLICIES, "policies-grants-" + grantSize, restricted.statements(), true, grantSize);
      }
    });
  }

  @Test
  public void explainViolationsGlobalAndAtGrantSizes() throws Exception {
    withEvidenceLock(() -> {
      assertExistingViolationIndex();
      seedViolations();
      writeViolationFixture();

      CapturedDaoCall<List<PolicyViolationDAO.RawThreatLevelCount>> global =
          capture(() -> policyViolationDAO.countUnfixedByThreatLevel(null, null));
      assertThat(total(global.result())).isEqualTo(ESTATE_SIZE);
      explainCaptured(VIOLATIONS, "violations-global", global.statements(), false, 0);
      for (int grantSize : GRANT_SIZES) {
        Set<String> applicationIds = ids("app-", grantSize);
        CapturedDaoCall<List<PolicyViolationDAO.RawThreatLevelCount>> restricted =
            capture(() -> policyViolationDAO.countUnfixedByThreatLevel(applicationIds, null));
        assertThat(total(restricted.result())).isEqualTo(grantSize);
        explainCaptured(
            VIOLATIONS, "violations-grants-" + grantSize, restricted.statements(), true, grantSize);
      }
    });
  }

  private <T> CapturedDaoCall<T> capture(final Callable<T> call) throws Exception {
    try (DashboardMetricsSqlPlanEvidenceSupport.RuntimeJdbcCapture capture =
        new DashboardMetricsSqlPlanEvidenceSupport.RuntimeJdbcCapture(operationalDataStore))
    {
      T result = call.call();
      return new CapturedDaoCall<>(result, capture.statements());
    }
  }

  private void explainCaptured(
      final DashboardMetricsSqlPlanEvidenceSupport.Metric metric,
      final String label,
      final List<DashboardMetricsSqlPlanEvidenceSupport.CapturedStatement> statements,
      final boolean restricted,
      final int expectedBindCount) throws Exception
  {
    assertThat(statements).as(label + " runtime statements").isNotEmpty();
    assertThat(statements.stream().mapToInt(statement -> statement.bindValues().size()).sum())
        .as(label + " captured bind count")
        .isEqualTo(expectedBindCount);
    for (int statementIndex = 0; statementIndex < statements.size(); statementIndex++) {
      var statement = statements.get(statementIndex);
      DashboardMetricsSqlPlanEvidenceSupport.requireRuntimeCapture(statement);
      assertRuntimePredicate(metric, statement, restricted);
      explain(metric, label + "-chunk-" + (statementIndex + 1), statement, statements.size());
    }
  }

  private static void assertRuntimePredicate(
      final DashboardMetricsSqlPlanEvidenceSupport.Metric metric,
      final DashboardMetricsSqlPlanEvidenceSupport.CapturedStatement statement,
      final boolean restricted)
  {
    String sql = statement.sql().toLowerCase();
    String tableName;
    String restrictedField;
    switch (metric) {
      case ORGANIZATIONS:
        tableName = "organization";
        restrictedField = "organization_id";
        break;
      case APPLICATIONS:
        tableName = "application";
        restrictedField = "application_id";
        break;
      case POLICIES:
        tableName = "policy";
        restrictedField = "owner_id";
        break;
      case VIOLATIONS:
        tableName = "policy_violation";
        restrictedField = "application_id";
        assertThat(sql).contains("fix_time", "is null", "group by", "threat_level");
        break;
      default:
        throw new IllegalStateException("Unhandled dashboard metric " + metric);
    }
    assertThat(sql).contains("select", tableName);
    if (restricted) {
      assertThat(sql).contains(restrictedField, " in ");
    }
    else {
      assertThat(sql).doesNotContain(restrictedField + "\" in ");
    }
  }

  private void explain(
      final DashboardMetricsSqlPlanEvidenceSupport.Metric metric,
      final String label,
      final DashboardMetricsSqlPlanEvidenceSupport.CapturedStatement captured,
      final int capturedChunkCount) throws Exception
  {
    List<String> plan = new ArrayList<>();
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement =
            connection.prepareStatement("EXPLAIN (ANALYZE, BUFFERS) " + captured.sql()))
    {
      List<Object> bindValues = captured.bindValues();
      for (int bindIndex = 0; bindIndex < bindValues.size(); bindIndex++) {
        statement.setObject(bindIndex + 1, bindValues.get(bindIndex));
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          plan.add(resultSet.getString(1));
        }
      }
    }

    DashboardMetricsSqlPlanEvidenceSupport.PlanAssessment assessment =
        DashboardMetricsSqlPlanEvidenceSupport.assess(metric, plan);
    List<String> evidence = new ArrayList<>();
    evidence.add("label=" + label);
    evidence.add("capturedAt=" + Instant.now());
    evidence.add("metric=" + metric);
    evidence.add("executionTargetMs=" + metric.executionTargetMillis());
    evidence.add("fixtureEstateSize=" + ESTATE_SIZE);
    evidence.add("sqlCaptureSource=" + captured.source());
    evidence.add("runtimeCapturedChunkCount=" + capturedChunkCount);
    evidence.add("bindCount=" + captured.bindValues().size());
    evidence.add("runtimeSql=" + captured.sql());
    evidence.add("runtimeBindValues=" + captured.bindValues());
    evidence.add("classification=" + assessment.classification());
    evidence.add("classificationReason=" + assessment.reason());
    evidence.add("selectedIndexes=" + String.join(",", selectedIndexes(plan)));
    evidence.add("heapFetchEvidence=" + DashboardMetricsSqlPlanEvidenceSupport.heapFetchEvidence(plan));
    evidence.addAll(plan);
    writeEvidence(label + ".txt", evidence);

    assertThat(assessment.classification())
        .withFailMessage("RED plan for %s: %s", label, assessment.reason())
        .isEqualTo("GREEN");
  }

  private static Set<String> selectedIndexes(final List<String> plan) {
    Set<String> indexes = new LinkedHashSet<>();
    for (String line : plan) {
      Matcher matcher = INDEX_PATTERN.matcher(line);
      while (matcher.find()) {
        indexes.add(matcher.group(1).replace("\"", ""));
      }
    }
    return indexes;
  }

  private void seedOrganizations() throws Exception {
    execute("INSERT INTO " + table("organization")
        + " (organization_id, parent_organization_id, name, name_lowercase_no_whitespace) "
        + "SELECT '" + PREFIX + "org-' || lpad(i::text, 5, '0'), 'ROOT_ORGANIZATION_ID', "
        + "'Task 9 plan org ' || i, 'task9planorg' || i FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    analyze("organization");
  }

  private void seedApplications() throws Exception {
    execute("INSERT INTO " + table("application")
        + " (application_id, public_id, public_id_lowercase, name, name_lowercase_no_whitespace, organization_id) "
        + "SELECT '" + PREFIX + "app-' || lpad(i::text, 5, '0'), 'task9-plan-app-' || i, "
        + "'task9-plan-app-' || i, 'Task 9 plan app ' || i, 'task9planapp' || i, 'ROOT_ORGANIZATION_ID' "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    analyze("application");
  }

  private void seedPolicies() throws Exception {
    seedOrganizations();
    execute("INSERT INTO " + table("policy")
        + " (policy_id, owner_id, name, name_lowercase_no_whitespace, threat_level, "
        + "legacy_violation_allowed, content, drools_code) "
        + "SELECT '" + PREFIX + "policy-' || lpad(i::text, 5, '0'), "
        + "'" + PREFIX + "org-' || lpad(i::text, 5, '0'), 'Task 9 policy ' || i, "
        + "'task9policy' || i, (i % 10)::smallint, false, '{}', '' "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    analyze("policy");
  }

  private void seedViolations() throws Exception {
    seedApplications();
    execute("INSERT INTO " + table("policy_violation")
        + " (policy_violation_id, application_id, stage_type_id, policy_id, policy_name, threat_level, "
        + "threat_category, constraint_facts_json, open_time, seen_by_primary_evaluation, "
        + "seen_by_monitoring_evaluation) "
        + "SELECT '" + PREFIX + "violation-' || lpad(i::text, 5, '0'), "
        + "'" + PREFIX + "app-' || lpad(i::text, 5, '0'), 'build', 'task9-policy', 'Task 9 policy', "
        + "(i % 10)::smallint, 'SECURITY', '[]', timestamp '2026-01-01 00:00:00' + i * interval '1 second', "
        + "true, false FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("policy_violation")
        + " (policy_violation_id, application_id, stage_type_id, policy_id, policy_name, threat_level, "
        + "threat_category, constraint_facts_json, open_time, fix_time, seen_by_primary_evaluation, "
        + "seen_by_monitoring_evaluation) "
        + "SELECT '" + PREFIX + "fixed-' || lpad(i::text, 5, '0'), "
        + "'" + PREFIX + "app-' || lpad((((i - 1) % " + ESTATE_SIZE + ") + 1)::text, 5, '0'), "
        + "'build', 'task9-policy', 'Task 9 policy', (i % 10)::smallint, 'SECURITY', '[]', "
        + "timestamp '2025-01-01 00:00:00' + i * interval '1 second', "
        + "timestamp '2025-06-01 00:00:00' + i * interval '1 second', true, false "
        + "FROM generate_series(1, " + FIXED_VIOLATION_COUNT + ") AS i");
    analyze("policy_violation");
  }

  private void writeFixture(final String metric, final String tableName, final String idColumn) throws Exception {
    long seeded = count("SELECT count(*) FROM " + table(tableName) + " WHERE " + idColumn + " LIKE '" + PREFIX + "%'");
    long total = count("SELECT count(*) FROM " + table(tableName));
    writeEvidence(
        "fixture-" + metric + ".txt",
        List.of(
            "metric=" + metric,
            "deterministicPrefix=" + PREFIX,
            "seededRows=" + seeded,
            "tableRows=" + total,
            "grantSizes=" + GRANT_SIZES,
            "configuredRuntimeChunkSize=" + RUNTIME_CHUNK_SIZE,
            "actualChunkCounts=recorded from runtime JDBC capture in each plan file"));
    assertThat(seeded).isEqualTo(ESTATE_SIZE);
  }

  private void writeViolationFixture() throws Exception {
    String fixturePredicate = "policy_violation_id LIKE '" + PREFIX + "%'";
    long qualifying = count("SELECT count(*) FROM " + table("policy_violation") + " WHERE "
        + fixturePredicate + " AND fix_time IS NULL");
    long fixed = count("SELECT count(*) FROM " + table("policy_violation") + " WHERE "
        + fixturePredicate + " AND fix_time IS NOT NULL");
    long fixtureTotal = count("SELECT count(*) FROM " + table("policy_violation") + " WHERE " + fixturePredicate);
    long tableTotal = count("SELECT count(*) FROM " + table("policy_violation"));
    writeEvidence(
        "fixture-violations.txt",
        List.of(
            "metric=violations",
            "deterministicPrefix=" + PREFIX,
            "qualifyingOpenUnfixedRows=" + qualifying,
            "fixedBackgroundRows=" + fixed,
            "fixtureTotalRows=" + fixtureTotal,
            "tableRows=" + tableTotal,
            "qualifyingSelectivity=" + qualifying + "/" + fixtureTotal,
            "grantSizes=" + GRANT_SIZES,
            "configuredRuntimeChunkSize=" + RUNTIME_CHUNK_SIZE,
            "actualChunkCounts=recorded from runtime JDBC capture in each plan file"));
    assertThat(qualifying).isEqualTo(ESTATE_SIZE);
    assertThat(fixed).isEqualTo(FIXED_VIOLATION_COUNT);
    assertThat(fixtureTotal).isEqualTo(ESTATE_SIZE + FIXED_VIOLATION_COUNT);
  }

  private void assertExistingViolationIndex() throws Exception {
    long indexCount = count("SELECT count(*) FROM pg_indexes WHERE schemaname = '"
        + operationalDataStore.getDatabaseSchema() + "' "
        + "AND indexname = 'policy_violation_app_stage_open_unfixed_idx'");
    assertThat(indexCount)
        .withFailMessage("Required existing policy_violation_app_stage_open_unfixed_idx is unavailable")
        .isEqualTo(1L);
  }

  private static long total(final List<PolicyViolationDAO.RawThreatLevelCount> counts) {
    return counts.stream().mapToLong(PolicyViolationDAO.RawThreatLevelCount::count).sum();
  }

  private static Set<String> ids(final String type, final int count) {
    Set<String> ids = new LinkedHashSet<>();
    for (int i = 1; i <= count; i++) {
      ids.add(PREFIX + type + String.format("%05d", i));
    }
    return ids;
  }

  private void analyze(final String tableName) throws Exception {
    execute("ANALYZE " + table(tableName));
  }

  private long count(final String sql) throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql))
    {
      resultSet.next();
      return resultSet.getLong(1);
    }
  }

  private void execute(final String sql) throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      statement.execute(sql);
    }
  }

  private String table(final String tableName) {
    String schema = operationalDataStore.getDatabaseSchema();
    if (!schema.matches("[A-Za-z0-9_]+") || !tableName.matches("[a-z_]+")) {
      throw new IllegalStateException("Unsafe PostgreSQL evidence identifier");
    }
    return '"' + schema + "\".\"" + tableName + '"';
  }

  private static void writeEvidence(final String fileName, final List<String> lines) throws IOException {
    Path testWorkingDirectory = Path.of("").toAbsolutePath();
    Path worktreeRoot = testWorkingDirectory.getFileName().toString().equals("insight-brain-service")
        ? testWorkingDirectory.getParent()
        : testWorkingDirectory;
    Path target = worktreeRoot.resolve("insight-brain-service/target/dashboard-sql-evidence");
    write(target.resolve(fileName), lines);
    String durableDir = System.getProperty("dashboard.metrics.sql.evidenceDir");
    if (durableDir != null && !durableDir.isBlank()) {
      write(Path.of(durableDir).resolve(fileName), lines);
    }
  }

  private static void write(final Path path, final List<String> lines) throws IOException {
    Files.createDirectories(path.getParent());
    Files.write(path, lines, StandardCharsets.UTF_8);
  }

  private void withEvidenceLock(final CheckedRunnable work) throws Exception {
    try (Connection lockConnection = operationalDataStore.getDataSource().getConnection()) {
      DashboardMetricsSqlColdEvidenceSupport.withAdvisoryLock(
          new PostgresAdvisoryLock(lockConnection, ADVISORY_LOCK_KEY),
          () -> {
            purgeEvidenceFixture();
            try {
              work.run();
              return null;
            }
            finally {
              purgeEvidenceFixture();
            }
          });
    }
  }

  private static final class PostgresAdvisoryLock
      implements DashboardMetricsSqlColdEvidenceSupport.LockLifecycle
  {
    private final Connection connection;

    private final long key;

    private PostgresAdvisoryLock(final Connection connection, final long key) {
      this.connection = connection;
      this.key = key;
    }

    @Override
    public void acquire() throws Exception {
      try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_lock(?)")) {
        statement.setLong(1, key);
        statement.execute();
      }
    }

    @Override
    public void release() throws Exception {
      try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
        statement.setLong(1, key);
        try (ResultSet result = statement.executeQuery()) {
          assertThat(result.next()).isTrue();
          assertThat(result.getBoolean(1)).as("PostgreSQL plan evidence advisory lock released").isTrue();
        }
      }
    }
  }

  @FunctionalInterface
  private interface CheckedRunnable
  {
    void run() throws Exception;
  }

  private record CapturedDaoCall<T>(
      T result,
      List<DashboardMetricsSqlPlanEvidenceSupport.CapturedStatement> statements)
  {
  }
}
