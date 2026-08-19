/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import static com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlPlanEvidenceSupport.CaptureSource.RUNTIME_JDBC;

final class DashboardMetricsSqlPlanEvidenceSupport
{
  private static final Pattern EXECUTION_TIME_PATTERN =
      Pattern.compile("Execution Time: ([0-9.]+) ms");

  private DashboardMetricsSqlPlanEvidenceSupport() {
  }

  static PlanAssessment assess(final Metric metric, final List<String> plan) {
    String joined = String.join("\n", plan);
    if (!joined.contains("Planning Time:") || !joined.contains("Execution Time:") || !joined.contains("Buffers:")) {
      return new PlanAssessment("RED", "EXPLAIN output omitted required timing or buffer evidence");
    }
    Matcher executionTime = EXECUTION_TIME_PATTERN.matcher(joined);
    if (!executionTime.find()) {
      return new PlanAssessment("RED", "EXPLAIN output omitted parseable execution time");
    }
    double measuredMillis = Double.parseDouble(executionTime.group(1));
    if (measuredMillis > metric.executionTargetMillis()) {
      return new PlanAssessment(
          "RED",
          "execution " + measuredMillis + " ms exceeded fixed " + metric.executionTargetMillis() + " ms target");
    }
    return new PlanAssessment(
        "GREEN",
        "execution " + measuredMillis + " ms met fixed " + metric.executionTargetMillis() + " ms target");
  }

  static String heapFetchEvidence(final List<String> plan) {
    return plan.stream()
        .filter(line -> line.contains("Heap Fetches:"))
        .map(String::trim)
        .findFirst()
        .orElse("Heap Fetches: N/A (not reported by PostgreSQL)");
  }

  static void requireRuntimeCapture(final CapturedStatement statement) {
    if (statement.source() != RUNTIME_JDBC) {
      throw new IllegalArgumentException("SQL evidence requires runtime JDBC capture");
    }
    if (statement.sql() == null || statement.sql().isBlank()) {
      throw new IllegalArgumentException("Runtime JDBC capture did not contain SQL");
    }
  }

  enum Metric
  {
    ORGANIZATIONS(50.0),
    APPLICATIONS(200.0),
    POLICIES(50.0),
    VIOLATIONS(500.0);

    private final double executionTargetMillis;

    Metric(final double executionTargetMillis) {
      this.executionTargetMillis = executionTargetMillis;
    }

    double executionTargetMillis() {
      return executionTargetMillis;
    }
  }

  enum CaptureSource
  {
    RUNTIME_JDBC,
    RECONSTRUCTED
  }

  record CapturedStatement(String sql, List<Object> bindValues, CaptureSource source)
  {
  }

  record PlanAssessment(String classification, String reason)
  {
  }

  static final class RuntimeJdbcCapture
      implements AutoCloseable
  {
    private final Field dataSourceField;

    private final Object dataStore;

    private final DataSource originalDataSource;

    private final List<CapturedStatement> statements = new ArrayList<>();

    RuntimeJdbcCapture(final Object dataStore) {
      this.dataStore = dataStore;
      this.dataSourceField = findField(dataStore.getClass(), "dataSource");
      try {
        dataSourceField.setAccessible(true);
        originalDataSource = (DataSource) dataSourceField.get(dataStore);
        dataSourceField.set(dataStore, capturingDataSource(originalDataSource));
      }
      catch (IllegalAccessException e) {
        throw new IllegalStateException("Unable to install test-only runtime JDBC capture", e);
      }
    }

    List<CapturedStatement> statements() {
      return List.copyOf(statements);
    }

    @Override
    public void close() {
      try {
        dataSourceField.set(dataStore, originalDataSource);
      }
      catch (IllegalAccessException e) {
        throw new IllegalStateException("Unable to restore operational data source after JDBC capture", e);
      }
    }

    private DataSource capturingDataSource(final DataSource delegate) {
      return (DataSource) Proxy.newProxyInstance(
          DataSource.class.getClassLoader(),
          new Class<?>[]{DataSource.class},
          (proxy, method, arguments) -> {
            Object result = invoke(delegate, method, arguments);
            if (method.getName().equals("getConnection") && result instanceof Connection connection) {
              return capturingConnection(connection);
            }
            return result;
          });
    }

    private Connection capturingConnection(final Connection delegate) {
      return (Connection) Proxy.newProxyInstance(
          Connection.class.getClassLoader(),
          new Class<?>[]{Connection.class},
          (proxy, method, arguments) -> {
            Object result = invoke(delegate, method, arguments);
            if (method.getName().equals("prepareStatement")
                && arguments != null
                && arguments.length > 0
                && arguments[0] instanceof String sql
                && result instanceof PreparedStatement statement)
          {
              return capturingPreparedStatement(statement, sql);
            }
            return result;
          });
    }

    private PreparedStatement capturingPreparedStatement(final PreparedStatement delegate, final String sql) {
      Map<Integer, Object> bindValues = new LinkedHashMap<>();
      return (PreparedStatement) Proxy.newProxyInstance(
          PreparedStatement.class.getClassLoader(),
          new Class<?>[]{PreparedStatement.class},
          (proxy, method, arguments) -> {
            if (method.getName().equals("clearParameters")) {
              bindValues.clear();
            }
            else if (method.getName().startsWith("set")
                && arguments != null
                && arguments.length >= 2
                && arguments[0] instanceof Integer bindIndex)
          {
              bindValues.put(bindIndex, method.getName().equals("setNull") ? null : arguments[1]);
            }
            Object result = invoke(delegate, method, arguments);
            if (method.getName().equals("execute")
                || method.getName().equals("executeQuery")
                || method.getName().equals("executeUpdate")
                || method.getName().equals("executeLargeUpdate"))
          {
              statements.add(new CapturedStatement(sql, orderedBindValues(bindValues), RUNTIME_JDBC));
            }
            return result;
          });
    }

    private static List<Object> orderedBindValues(final Map<Integer, Object> bindValues) {
      if (bindValues.isEmpty()) {
        return List.of();
      }
      List<Object> ordered = new ArrayList<>(Collections.nCopies(Collections.max(bindValues.keySet()), null));
      bindValues.forEach((index, value) -> ordered.set(index - 1, value));
      return Collections.unmodifiableList(ordered);
    }

    private static Field findField(final Class<?> start, final String name) {
      for (Class<?> type = start; type != null; type = type.getSuperclass()) {
        try {
          return type.getDeclaredField(name);
        }
        catch (NoSuchFieldException ignored) {
          // Continue through the concrete data store hierarchy.
        }
      }
      throw new IllegalStateException("Unable to find operational data source field for test capture");
    }

    private static Object invoke(final Object target, final Method method, final Object[] arguments) throws Throwable {
      try {
        return method.invoke(target, arguments);
      }
      catch (InvocationTargetException e) {
        throw e.getCause();
      }
    }
  }
}
