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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsRequestDTO;
import com.sonatype.insight.brain.dashboard.metrics.MetricValueDTO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-gated PostgreSQL cold evidence for the SQL-backed dashboard metrics.
 *
 * <p>
 * This is application-cache-cold evidence: every sample invokes the real resolver and coordinator
 * directly, bypassing the service's five-second cache. The embedded PostgreSQL process remains
 * resident and neither PostgreSQL shared buffers nor OS page cache are evicted.
 * </p>
 */
@ComponentPgTest
public class PostgresDashboardMetricsSqlColdEvidenceTest
    extends AbstractComponentPgTest
{
  private static final Logger log = LoggerFactory.getLogger(PostgresDashboardMetricsSqlColdEvidenceTest.class);

  private static final String OPT_IN_PROPERTY = "dashboard.sql.coldEvidence";

  private static final String PREFIX = "task10-cold-";

  private static final long ADVISORY_LOCK_KEY = 0x44415348434F4C44L;

  private static final String PARENT_ORGANIZATION_ID = PREFIX + "parent";

  private static final int ESTATE_SIZE = 5_000;

  private static final int FIXED_VIOLATION_COUNT = 15_000;

  private static final int WARMUP_SAMPLES = 3;

  private static final int MEASURED_SAMPLES = 20;

  private static final Map<String, Long> TARGET_MILLIS = Map.of(
      "applications", 200L,
      "organizations", 50L,
      "policies", 50L,
      "violations", 500L);

  @Inject
  private OperationalDataStore operationalDataStore;

  @Inject
  private DashboardMetricsScopeResolver scopeResolver;

  @Inject
  private DashboardMetricsSqlCoordinator coordinator;

  @BeforeEach
  public void requireExplicitOptIn() {
    Assumptions.assumeTrue(
        Boolean.getBoolean(OPT_IN_PROPERTY),
        "Manual cold evidence requires -D" + OPT_IN_PROPERTY + "=true");
  }

  private void purgeEvidenceFixture() throws Exception {
    execute("DELETE FROM " + table("membership_mapping")
        + " WHERE membership_mapping_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("policy_violation")
        + " WHERE policy_violation_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("policy")
        + " WHERE policy_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("application")
        + " WHERE application_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("organization_ancestor")
        + " WHERE organization_ancestor_id LIKE '" + PREFIX + "%'");
    execute("DELETE FROM " + table("organization")
        + " WHERE organization_id LIKE '" + PREFIX + "%'");
  }

  @Test
  public void recordColdPercentilesForRequiredPrincipalShapes() throws Exception {
    try (Connection lockConnection = operationalDataStore.getDataSource().getConnection()) {
      DashboardMetricsSqlColdEvidenceSupport.withAdvisoryLock(
          new PostgresAdvisoryLock(lockConnection, ADVISORY_LOCK_KEY),
          () -> {
            purgeEvidenceFixture();
            try {
              seedEstate();
              Principals principals = seedPrincipals();
              writeEnvironmentEvidence();
              writeFixtureEvidence(principals);

              List<String> failures = new ArrayList<>();
              List<DashboardMetricsSqlColdEvidenceSupport.CheckedSupplier<PrincipalEvidence>> executions = List.of(
                  () -> measureAndWritePrincipal(
                      "GLOBAL",
                      principals.global(),
                      ResolvedScope.Kind.GLOBAL,
                      ESTATE_SIZE,
                      ESTATE_SIZE + 1),
                  () -> measureAndWritePrincipal(
                      "SUBTREE",
                      principals.subtree(),
                      ResolvedScope.Kind.RESTRICTED,
                      ESTATE_SIZE,
                      ESTATE_SIZE + 1),
                  () -> measureAndWritePrincipal(
                      "WIDE_5000",
                      principals.wide(),
                      ResolvedScope.Kind.RESTRICTED,
                      ESTATE_SIZE,
                      ESTATE_SIZE));
              DashboardMetricsSqlColdEvidenceSupport.runSequentiallyUntil(
                  executions,
                  evidence -> evaluateThresholds(evidence, failures));
              assertThat(failures).as("cold SQL threshold failures").isEmpty();
              return null;
            }
            finally {
              purgeEvidenceFixture();
            }
          });
    }
  }

  private PrincipalEvidence measureAndWritePrincipal(
      final String shape,
      final User user,
      final ResolvedScope.Kind expectedKind,
      final int expectedApplications,
      final int expectedOrganizations) throws IOException
  {
    PrincipalEvidence evidence =
        measurePrincipal(shape, user, expectedKind, expectedApplications, expectedOrganizations);
    writePrincipalEvidence(evidence);
    return evidence;
  }

  private boolean evaluateThresholds(
      final PrincipalEvidence evidence,
      final List<String> failures)
  {
    boolean overTwicePrediction = false;
    for (Map.Entry<String, Samples> metric : evidence.metrics()
        .entrySet()
        .stream()
        .filter(entry -> TARGET_MILLIS.containsKey(entry.getKey()))
        .toList())
    {
      long targetMillis = TARGET_MILLIS.get(metric.getKey());
      long p95 = DashboardMetricsSqlColdEvidenceSupport.nearestRank(metric.getValue().nanos(), 95);
      if (!"PASS".equals(DashboardMetricsSqlColdEvidenceSupport.thresholdVerdict(p95, targetMillis))) {
        failures.add(evidence.shape() + "/" + metric.getKey() + " p95="
            + formatMillis(p95) + "ms target=" + targetMillis + "ms");
        if (p95 > targetMillis * 2_000_000L) {
          overTwicePrediction = true;
        }
      }
    }
    return overTwicePrediction;
  }

  private PrincipalEvidence measurePrincipal(
      final String shape,
      final User user,
      final ResolvedScope.Kind expectedKind,
      final int expectedApplications,
      final int expectedOrganizations)
  {
    loginAs(user);
    for (int i = 0; i < WARMUP_SAMPLES; i++) {
      sample(shape, 0, expectedKind, expectedApplications, expectedOrganizations);
    }

    Map<String, List<Long>> raw = new LinkedHashMap<>();
    raw.put("scope-resolution", new ArrayList<>());
    TARGET_MILLIS.keySet().stream().sorted().forEach(metric -> raw.put(metric, new ArrayList<>()));
    for (int sample = 1; sample <= MEASURED_SAMPLES; sample++) {
      Sample measured = sample(shape, sample, expectedKind, expectedApplications, expectedOrganizations);
      raw.get("scope-resolution").add(measured.scopeNanos());
      raw.get("applications").add(measured.applicationsNanos());
      raw.get("organizations").add(measured.organizationsNanos());
      raw.get("policies").add(measured.policiesNanos());
      raw.get("violations").add(measured.violationsNanos());
    }
    Map<String, Samples> immutable = new LinkedHashMap<>();
    raw.forEach((metric, values) -> immutable.put(metric, new Samples(List.copyOf(values))));
    return new PrincipalEvidence(shape, Map.copyOf(immutable));
  }

  private Sample sample(
      final String shape,
      final int sample,
      final ResolvedScope.Kind expectedKind,
      final int expectedApplications,
      final int expectedOrganizations)
  {
    long scopeStartedAt = System.nanoTime();
    ResolvedScope scope = scopeResolver.resolve(new DashboardMetricsRequestDTO());
    long scopeNanos = System.nanoTime() - scopeStartedAt;

    assertThat(scope.kind()).as(shape + " scope kind").isEqualTo(expectedKind);
    if (expectedKind == ResolvedScope.Kind.RESTRICTED) {
      assertThat(scope.applicationIds()).as(shape + " application scope").hasSize(expectedApplications);
      assertThat(scope.organizationIds()).as(shape + " organization scope").hasSize(expectedOrganizations);
    }

    TimedValue<MetricValueDTO> applications = timed(() -> coordinator.countApplications(scope));
    TimedValue<MetricValueDTO> organizations = timed(() -> coordinator.countOrganizations(scope));
    TimedValue<MetricValueDTO> policies = timed(() -> coordinator.countPolicies(scope));
    TimedValue<MetricValueDTO> violations = timed(() -> coordinator.countViolations(scope));

    assertMetric(shape, "applications", applications.value(), expectedApplications, expectedKind);
    assertMetric(shape, "organizations", organizations.value(), expectedOrganizations, expectedKind);
    assertMetric(shape, "policies", policies.value(), ESTATE_SIZE, expectedKind);
    assertMetric(shape, "violations", violations.value(), ESTATE_SIZE, expectedKind);

    if (sample > 0) {
      logSample(shape, "scope-resolution", sample, scopeNanos);
      logSample(shape, "applications", sample, applications.nanos());
      logSample(shape, "organizations", sample, organizations.nanos());
      logSample(shape, "policies", sample, policies.nanos());
      logSample(shape, "violations", sample, violations.nanos());
    }
    return new Sample(
        scopeNanos,
        applications.nanos(),
        organizations.nanos(),
        policies.nanos(),
        violations.nanos());
  }

  private static void assertMetric(
      final String shape,
      final String metric,
      final MetricValueDTO value,
      final long fixtureMinimum,
      final ResolvedScope.Kind kind)
  {
    assertThat(value.total).as(shape + " " + metric + " total").isNotNull();
    if (kind == ResolvedScope.Kind.GLOBAL) {
      assertThat(value.total).as(shape + " " + metric + " fixture minimum").isGreaterThanOrEqualTo(fixtureMinimum);
    }
    else {
      assertThat(value.total).as(shape + " " + metric + " fixture count").isEqualTo(fixtureMinimum);
    }
  }

  private static <T> TimedValue<T> timed(final Supplier<T> supplier) {
    long startedAt = System.nanoTime();
    T value = supplier.get();
    return new TimedValue<>(value, System.nanoTime() - startedAt);
  }

  private static void logSample(
      final String shape,
      final String metric,
      final int sample,
      final long durationNanos)
  {
    log.info(
        "DASHBOARD_BENCHMARK metric={} principalShape={} sample={} durationMs={}",
        metric,
        shape,
        sample,
        formatMillis(durationNanos));
  }

  private void seedEstate() throws Exception {
    execute("INSERT INTO " + table("organization")
        + " (organization_id, parent_organization_id, name, name_lowercase_no_whitespace) VALUES "
        + "('" + PARENT_ORGANIZATION_ID + "', 'ROOT_ORGANIZATION_ID', "
        + "'Task 10 cold parent', 'task10coldparent')");
    execute("INSERT INTO " + table("organization_ancestor")
        + " (organization_ancestor_id, organization_id, ancestor_id, ancestor_distance) VALUES "
        + "('" + PREFIX + "oa-parent-self', '" + PARENT_ORGANIZATION_ID + "', '"
        + PARENT_ORGANIZATION_ID + "', 0), "
        + "('" + PREFIX + "oa-parent-root', '" + PARENT_ORGANIZATION_ID
        + "', 'ROOT_ORGANIZATION_ID', 1)");
    execute("INSERT INTO " + table("organization")
        + " (organization_id, parent_organization_id, name, name_lowercase_no_whitespace) "
        + "SELECT '" + PREFIX + "org-' || lpad(i::text, 4, '0'), '" + PARENT_ORGANIZATION_ID + "', "
        + "'Task 10 cold org ' || i, 'task10coldorg' || i "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("organization_ancestor")
        + " (organization_ancestor_id, organization_id, ancestor_id, ancestor_distance) "
        + "SELECT '" + PREFIX + "oa-self-' || lpad(i::text, 4, '0'), "
        + "'" + PREFIX + "org-' || lpad(i::text, 4, '0'), "
        + "'" + PREFIX + "org-' || lpad(i::text, 4, '0'), 0 "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("organization_ancestor")
        + " (organization_ancestor_id, organization_id, ancestor_id, ancestor_distance) "
        + "SELECT '" + PREFIX + "oa-parent-' || lpad(i::text, 4, '0'), "
        + "'" + PREFIX + "org-' || lpad(i::text, 4, '0'), '" + PARENT_ORGANIZATION_ID + "', 1 "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("organization_ancestor")
        + " (organization_ancestor_id, organization_id, ancestor_id, ancestor_distance) "
        + "SELECT '" + PREFIX + "oa-root-' || lpad(i::text, 4, '0'), "
        + "'" + PREFIX + "org-' || lpad(i::text, 4, '0'), 'ROOT_ORGANIZATION_ID', 2 "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("application")
        + " (application_id, public_id, public_id_lowercase, name, name_lowercase_no_whitespace, organization_id) "
        + "SELECT '" + PREFIX + "app-' || lpad(i::text, 4, '0'), '" + PREFIX + "app-' || i, "
        + "'" + PREFIX + "app-' || i, 'Task 10 cold app ' || i, 'task10coldapp' || i, "
        + "'" + PREFIX + "org-' || lpad(i::text, 4, '0') "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("policy")
        + " (policy_id, owner_id, name, name_lowercase_no_whitespace, threat_level, "
        + "legacy_violation_allowed, content, drools_code) "
        + "SELECT '" + PREFIX + "policy-' || lpad(i::text, 4, '0'), "
        + "'" + PREFIX + "org-' || lpad(i::text, 4, '0'), 'Task 10 cold policy ' || i, "
        + "'task10coldpolicy' || i, (i % 10)::smallint, false, '{}', '' "
        + "FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("policy_violation")
        + " (policy_violation_id, application_id, stage_type_id, policy_id, policy_name, threat_level, "
        + "threat_category, constraint_facts_json, open_time, seen_by_primary_evaluation, "
        + "seen_by_monitoring_evaluation) "
        + "SELECT '" + PREFIX + "violation-' || lpad(i::text, 4, '0'), "
        + "'" + PREFIX + "app-' || lpad(i::text, 4, '0'), 'build', "
        + "'" + PREFIX + "policy-' || lpad(i::text, 4, '0'), 'Task 10 cold policy', "
        + "(i % 10)::smallint, 'SECURITY', '[]', timestamp '2026-01-01 00:00:00' + i * interval '1 second', "
        + "true, false FROM generate_series(1, " + ESTATE_SIZE + ") AS i");
    execute("INSERT INTO " + table("policy_violation")
        + " (policy_violation_id, application_id, stage_type_id, policy_id, policy_name, threat_level, "
        + "threat_category, constraint_facts_json, open_time, fix_time, seen_by_primary_evaluation, "
        + "seen_by_monitoring_evaluation) "
        + "SELECT '" + PREFIX + "fixed-' || lpad(i::text, 5, '0'), "
        + "'" + PREFIX + "app-' || lpad((((i - 1) % " + ESTATE_SIZE + ") + 1)::text, 4, '0'), "
        + "'build', 'task10-cold-policy', 'Task 10 cold policy', (i % 10)::smallint, 'SECURITY', '[]', "
        + "timestamp '2025-01-01 00:00:00' + i * interval '1 second', "
        + "timestamp '2025-06-01 00:00:00' + i * interval '1 second', true, false "
        + "FROM generate_series(1, " + FIXED_VIOLATION_COUNT + ") AS i");
    analyze("organization");
    analyze("organization_ancestor");
    analyze("application");
    analyze("policy");
    analyze("policy_violation");
  }

  private Principals seedPrincipals() throws Exception {
    User global = tempEntity.newUser(PREFIX + "global");
    Role globalRole = tempEntity.newRole(true, Permission.READ);
    tempEntity.newMembershipMapping(
        MembershipMapping.GLOBAL_CONTEXT_ID, globalRole.getId(), global.getUsername());

    User subtree = tempEntity.newUser(PREFIX + "subtree");
    Role subtreeRole = tempEntity.newRole(false, Permission.READ);
    tempEntity.newMembershipMapping(PARENT_ORGANIZATION_ID, subtreeRole.getId(), subtree.getUsername());

    User wide = tempEntity.newUser(PREFIX + "wide");
    Role wideRole = tempEntity.newRole(false, Permission.READ);
    insertWideMemberships(wideRole.getId(), wide.getUsername());
    return new Principals(global, subtree, wide, globalRole.getId(), subtreeRole.getId(), wideRole.getId());
  }

  private void insertWideMemberships(final String roleId, final String username) throws Exception {
    String sql = "INSERT INTO " + table("membership_mapping")
        + " (membership_mapping_id, context_id, role_id, member_name, member_type) VALUES (?, ?, ?, ?, 'USER')";
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(sql))
    {
      for (int i = 1; i <= ESTATE_SIZE; i++) {
        statement.setString(1, PREFIX + "mm-" + String.format(Locale.ROOT, "%04d", i));
        statement.setString(2, PREFIX + "org-" + String.format(Locale.ROOT, "%04d", i));
        statement.setString(3, roleId);
        statement.setString(4, username);
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  private void writeEnvironmentEvidence() throws Exception {
    writeEvidence("cold-environment.txt", List.of(
        "capturedAt=" + Instant.now(),
        "semantics=application-cache-cold/fresh-invocation",
        "serviceCache=bypassed; DashboardMetricsService is not invoked",
        "resolverInvocation=fresh for every warmup and measured sample",
        "coordinatorInvocation=fresh per metric for every warmup and measured sample",
        "postgresBufferState=uncontrolled; embedded PostgreSQL remains resident",
        "osPageCacheState=uncontrolled; no repository-native eviction mechanism exists",
        "diskColdClaim=false",
        "targetDecision=SUPPORTED for application-cache-cold/fresh-invocation latency; not a disk-cold claim",
        "documentedProtocol=No additional eviction semantics were found beyond the Task 10 brief and design",
        "execution=sequential",
        "warmupSamples=" + WARMUP_SAMPLES,
        "measuredSamples=" + MEASURED_SAMPLES,
        "percentileMethod=nearest-rank ceil(percentile/100 * sampleCount)",
        "javaVersion=" + System.getProperty("java.version"),
        "osName=" + System.getProperty("os.name"),
        "osArch=" + System.getProperty("os.arch"),
        "availableProcessors=" + Runtime.getRuntime().availableProcessors(),
        "postgresVersion=" + queryString("SHOW server_version")));
  }

  private void writeFixtureEvidence(final Principals principals) throws Exception {
    writeEvidence("cold-fixture.txt", List.of(
        "deterministicPrefix=" + PREFIX,
        "estateOrganizations=" + ESTATE_SIZE,
        "estateApplications=" + ESTATE_SIZE,
        "estatePolicies=" + ESTATE_SIZE,
        "estateOpenUnfixedViolations=" + ESTATE_SIZE,
        "estateFixedBackgroundViolations=" + FIXED_VIOLATION_COUNT,
        "subtreeGrantCount=1",
        "subtreeGrantedContext=" + PARENT_ORGANIZATION_ID,
        "wideGrantCount=" + count("SELECT count(*) FROM " + table("membership_mapping")
            + " WHERE membership_mapping_id LIKE '" + PREFIX + "mm-%'"),
        "globalRoleIdRecorded=false",
        "subtreeRoleIdRecorded=false",
        "wideRoleIdRecorded=false",
        "constructibleAuthorizationShapes=GLOBAL,organization-subtree,5000-organization-grants",
        "tagGrantProvenance=not claimed",
        "roleCount=3",
        "roleIdsStoredOnlyInDatabase=" + (principals.globalRoleId() != null
            && principals.subtreeRoleId() != null && principals.wideRoleId() != null)));
  }

  private void writePrincipalEvidence(final PrincipalEvidence evidence) throws IOException {
    List<String> lines = new ArrayList<>();
    lines.add("principalShape=" + evidence.shape());
    lines.add("warmupSamples=" + WARMUP_SAMPLES);
    lines.add("measuredSamples=" + MEASURED_SAMPLES);
    lines.add("semantics=application-cache-cold/fresh-invocation; PostgreSQL buffers uncontrolled");
    for (Map.Entry<String, Samples> entry : evidence.metrics()
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .toList())
    {
      String metric = entry.getKey();
      List<Long> samples = entry.getValue().nanos();
      long p50 = DashboardMetricsSqlColdEvidenceSupport.nearestRank(samples, 50);
      long p95 = DashboardMetricsSqlColdEvidenceSupport.nearestRank(samples, 95);
      lines.add(metric + ".rawNanos=" + samples);
      lines.add(metric + ".p50Nanos=" + p50);
      lines.add(metric + ".p50Ms=" + formatMillis(p50));
      lines.add(metric + ".p95Nanos=" + p95);
      lines.add(metric + ".p95Ms=" + formatMillis(p95));
      if (TARGET_MILLIS.containsKey(metric)) {
        long target = TARGET_MILLIS.get(metric);
        lines.add(metric + ".targetMs=" + target);
        lines.add(metric + ".verdict=" + DashboardMetricsSqlColdEvidenceSupport.thresholdVerdict(p95, target));
        lines.add(metric + ".over2xPrediction=" + (p95 > target * 2_000_000L));
      }
      else {
        lines.add(metric + ".targetMs=separate-no-fixed-threshold");
      }
    }
    long scopeP95 = DashboardMetricsSqlColdEvidenceSupport.nearestRank(
        evidence.metrics().get("scope-resolution").nanos(), 95);
    long metricP95Total = TARGET_MILLIS.keySet()
        .stream()
        .mapToLong(metric -> DashboardMetricsSqlColdEvidenceSupport.nearestRank(
            evidence.metrics().get(metric).nanos(), 95))
        .sum();
    lines.add("scopeResolutionDominatesMeasuredP95Total=" + (scopeP95 > metricP95Total));
    writeEvidence("cold-" + evidence.shape().toLowerCase(Locale.ROOT) + ".txt", lines);
  }

  private void loginAs(final User user) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    principals.add(new UserPrincipal(user.getUsername(), user.getUsername(), User.INTERNAL_REALM_ID),
        User.INTERNAL_REALM_ID);
    SimpleSession session = new SimpleSession();
    session.setId(UUID.randomUUID().toString());
    session.setStartTimestamp(new Date());
    Subject subject = new Subject.Builder(lookup(SecurityManager.class))
        .session(session)
        .principals(principals)
        .authenticated(true)
        .buildSubject();
    ThreadContext.bind(lookup(SecurityManager.class));
    ThreadContext.bind(subject);
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

  private String queryString(final String sql) throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql))
    {
      resultSet.next();
      return resultSet.getString(1);
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

  private static String formatMillis(final long nanos) {
    return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0d);
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
          assertThat(result.getBoolean(1)).as("PostgreSQL evidence advisory lock released").isTrue();
        }
      }
    }
  }

  private record Principals(
      User global,
      User subtree,
      User wide,
      String globalRoleId,
      String subtreeRoleId,
      String wideRoleId)
  {
  }

  private record Sample(
      long scopeNanos,
      long applicationsNanos,
      long organizationsNanos,
      long policiesNanos,
      long violationsNanos)
  {
  }

  private record TimedValue<T>(
      T value,
      long nanos)
  {
  }

  private record Samples(List<Long> nanos)
  {
  }

  private record PrincipalEvidence(
      String shape,
      Map<String, Samples> metrics)
  {
  }
}
