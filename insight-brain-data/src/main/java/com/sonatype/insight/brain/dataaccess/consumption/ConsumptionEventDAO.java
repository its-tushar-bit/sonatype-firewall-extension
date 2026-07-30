/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.consumption;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.consumption.ConsumptionAppTotal;
import com.sonatype.insight.brain.model.consumption.ConsumptionDailyTotal;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;
import com.sonatype.insight.brain.model.consumption.ConsumptionMonthlyBreakdown;
import com.sonatype.insight.brain.model.consumption.ConsumptionMonthlyTotal;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.CaseConditionStep;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ConsumptionEvents.CONSUMPTION_EVENTS;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION;

/**
 * DAO for consumption events.
 * <p>
 * Queries aggregate across all orgs within a tenant (Usage dashboard is tenant-wide).
 * Cross-tenant isolation is schema-per-tenant. Per-org endpoints MUST add org_id filter.
 *
 * @since 1.204
 */
@Named
@Singleton
public class ConsumptionEventDAO
    extends AbstractOperationalSqlDAO<ConsumptionEvent>
{
  private static final Logger log = LoggerFactory.getLogger(ConsumptionEventDAO.class);

  public static final String STAGE_UNKNOWN = "Unknown";

  @Inject
  public ConsumptionEventDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public Table<?> getJooqTable() {
    return CONSUMPTION_EVENTS;
  }

  @Override
  public Class<ConsumptionEvent> getEntityClass() {
    return ConsumptionEvent.class;
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final ConsumptionEvent entity) {
    super.fromEntity(record, entity);
    if (entity.getEventTimestamp() != null) {
      record.set(CONSUMPTION_EVENTS.EVENT_TIMESTAMP, Date.from(entity.getEventTimestamp()));
    }
    return record;
  }

  @Override
  protected ConsumptionEvent toEntity(final Record record) {
    if (record == null) {
      return null;
    }
    ConsumptionEvent entity = super.toEntity(record);
    Date timestamp = record.get(CONSUMPTION_EVENTS.EVENT_TIMESTAMP);
    if (timestamp != null) {
      entity.setEventTimestamp(timestamp.toInstant());
    }
    return entity;
  }

  /**
   * Inserts a consumption event with idempotency-key dedup.
   *
   * <p>
   * Uses {@code ON CONFLICT (idempotency_key) DO NOTHING} on PostgreSQL to atomically
   * dedup events that share an {@code idempotency_key}. On H2 (test infrastructure)
   * this clause is unsupported, so the H2 branch wraps the insert in a JDBC savepoint
   * and absorbs only SQLState {@code 23505} (unique-violation) — narrower than the
   * pattern in {@code AbstractDAO.insert}, which catches the whole {@code C23}
   * integrity-violation class. We do not delegate to {@code AbstractDAO.insert}
   * because that helper uses {@code onDuplicateKeyIgnore()} (primary-key conflict),
   * whereas dedup here must conflict on the partial unique index
   * {@code idx_consumption_events_idempotency_key}.
   *
   * <p>
   * Events with {@code idempotencyKey == null} insert freely (they fall through the
   * partial unique index's {@code WHERE idempotency_key IS NOT NULL} clause).
   */
  public void recordEvent(final ConsumptionEvent event) {
    generateIdIfNeeded(event);
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      InsertSetMoreStep<?> insert = tx.dsl()
          .insertInto(CONSUMPTION_EVENTS)
          .set(CONSUMPTION_EVENTS.ID, event.getId())
          .set(CONSUMPTION_EVENTS.ORG_ID, event.getOrgId())
          .set(CONSUMPTION_EVENTS.APP_ID, event.getAppId())
          .set(CONSUMPTION_EVENTS.SCAN_ID, event.getScanId())
          .set(CONSUMPTION_EVENTS.USER_ID, event.getUserId())
          .set(CONSUMPTION_EVENTS.TIER, event.getTier())
          .set(CONSUMPTION_EVENTS.SOURCE, event.getSource())
          .set(CONSUMPTION_EVENTS.ACTIVITY_TYPE, event.getActivityTypeRaw())
          .set(CONSUMPTION_EVENTS.COMPONENT_COUNT, event.getComponentCount())
          .set(CONSUMPTION_EVENTS.BILLING_MONTH, event.getBillingMonth())
          .set(CONSUMPTION_EVENTS.EVENT_TIMESTAMP,
              event.getEventTimestamp() != null ? Date.from(event.getEventTimestamp()) : null)
          .set(CONSUMPTION_EVENTS.IDEMPOTENCY_KEY, event.getIdempotencyKey());

      if (tx.dsl().dialect() == SQLDialect.H2) {
        // H2 1.4.196 does not support ON CONFLICT DO NOTHING; use a savepoint to absorb
        // the unique-constraint violation instead (same pattern as AbstractDAO.insert).
        // Catch only SQLState 23505 (unique violation) — broader C23 covers FK / NOT NULL /
        // CHECK / column-overflow which we want to propagate rather than silently swallow.
        tx.dsl().connection(conn -> {
          Savepoint sp = conn.setSavepoint();
          try (PreparedStatement ps = conn.prepareStatement(tx.dsl().renderInlined(insert))) {
            ps.execute();
            conn.releaseSavepoint(sp);
          }
          catch (SQLException e) {
            conn.rollback(sp);
            if (!H2_UNIQUE_VIOLATION_SQLSTATE.equals(e.getSQLState())) {
              throw e;
            }
            if (event.getIdempotencyKey() != null) {
              log.debug("Consumption-event dedup hit (H2) for key {}", event.getIdempotencyKey());
            }
          }
        });
      }
      else {
        // The unique index is partial: WHERE (idempotency_key IS NOT NULL).
        // PostgreSQL only matches ON CONFLICT to a partial index when the conflict
        // clause carries the same predicate. Without it, every insert fails with
        // "no unique or exclusion constraint matching the ON CONFLICT specification".
        int rowsInserted = insert.onConflict(CONSUMPTION_EVENTS.IDEMPOTENCY_KEY)
            .where(CONSUMPTION_EVENTS.IDEMPOTENCY_KEY.isNotNull())
            .doNothing()
            .execute();
        if (rowsInserted == 0 && event.getIdempotencyKey() != null) {
          log.debug("Consumption-event dedup hit (Postgres) for key {}", event.getIdempotencyKey());
        }
      }
      tx.commit();
    }
  }

  /** H2 SQLState for unique-constraint violations. Tighter than the general C23 class. */
  private static final String H2_UNIQUE_VIOLATION_SQLSTATE = "23505";

  public long sumByMonth(final LocalDate billingMonth) {
    try (TransactionContext tx = createTransactionContext()) {
      Long result = tx.dsl()
          .select(DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.BILLING_MONTH.eq(billingMonth))
          .fetchOne(0, Long.class);
      return result != null ? result : 0L;
    }
  }

  /**
   * Sum component_count for events whose timestamp falls within [start, end).
   * Uses idx_consumption_events_timestamp.
   */
  public long sumByTimestampRange(final Instant start, final Instant end) {
    try (TransactionContext tx = createTransactionContext()) {
      Long result = tx.dsl()
          .select(DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .fetchOne(0, Long.class);
      return result != null ? result : 0L;
    }
  }

  /**
   * Get the breakdown of consumption by activity type for a given billing month.
   */
  public Map<String, Long> activityBreakdownByMonth(final LocalDate billingMonth) {
    try (TransactionContext tx = createTransactionContext()) {
      Map<String, Long> result = new HashMap<>();
      tx.dsl()
          .select(CONSUMPTION_EVENTS.ACTIVITY_TYPE, DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.BILLING_MONTH.eq(billingMonth))
          .groupBy(CONSUMPTION_EVENTS.ACTIVITY_TYPE)
          .fetch()
          .forEach(record -> {
            String activityType = record.get(CONSUMPTION_EVENTS.ACTIVITY_TYPE);
            Long count = record.get(1, Long.class);
            result.put(activityType, count != null ? count : 0L);
          });
      return result;
    }
  }

  /**
   * Get the breakdown of consumption by activity type for events whose timestamp
   * falls within [start, end).
   */
  public Map<String, Long> activityBreakdownByRange(final Instant start, final Instant end) {
    try (TransactionContext tx = createTransactionContext()) {
      Map<String, Long> result = new HashMap<>();
      tx.dsl()
          .select(CONSUMPTION_EVENTS.ACTIVITY_TYPE, DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .groupBy(CONSUMPTION_EVENTS.ACTIVITY_TYPE)
          .fetch()
          .forEach(record -> {
            String activityType = record.get(CONSUMPTION_EVENTS.ACTIVITY_TYPE);
            Long count = record.get(1, Long.class);
            result.put(activityType, count != null ? count : 0L);
          });
      return result;
    }
  }

  /**
   * Get monthly consumption history for the last N months.
   */
  public List<ConsumptionMonthlyTotal> monthlyHistory(final int maxMonths) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(CONSUMPTION_EVENTS.BILLING_MONTH, DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .groupBy(CONSUMPTION_EVENTS.BILLING_MONTH)
          .orderBy(CONSUMPTION_EVENTS.BILLING_MONTH.desc())
          .limit(maxMonths)
          .fetch(record -> new ConsumptionMonthlyTotal(
              record.get(CONSUMPTION_EVENTS.BILLING_MONTH),
              record.get(1, Long.class) != null ? record.get(1, Long.class) : 0L));
    }
  }

  /**
   * Build a CASE expression that buckets event_timestamp into its containing window-start.
   * windowStarts and windowEnds must have equal length and represent half-open ranges
   * [start_i, end_i) in any order. Returns null when no window matches.
   * <p>
   * Each window adds two prepared-statement parameters; keep windowStarts.size() bounded
   * (~100 max) to stay well under PostgreSQL's prepared-statement parameter limit.
   */
  private static Field<LocalDate> windowBucketCase(
      final List<Instant> windowStarts,
      final List<Instant> windowEnds,
      final List<LocalDate> windowLabels)
  {
    if (windowStarts.isEmpty()) {
      throw new IllegalArgumentException("windowStarts must not be empty");
    }
    CaseConditionStep<LocalDate> caseExpr = null;
    for (int i = 0; i < windowStarts.size(); i++) {
      Condition cond = CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(windowStarts.get(i)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(windowEnds.get(i))));
      LocalDate label = windowLabels.get(i);
      caseExpr = (caseExpr == null)
          ? DSL.when(cond, DSL.val(label))
          : caseExpr.when(cond, DSL.val(label));
    }
    return caseExpr.otherwise(DSL.castNull(LocalDate.class));
  }

  /**
   * Get consumption history bucketed by billing window. Each row's event_timestamp is
   * mapped to the window-start it falls within (windowStarts/windowEnds define half-open ranges).
   * Single bucketed query — uses idx_consumption_events_timestamp via the {@code &gt;= oldestStart}
   * predicate. The bucket CASE is computed in an inner SELECT and grouped in the outer SELECT
   * so Postgres recognises the GROUP BY column as a derived value, not a transformation
   * referencing the underlying event_timestamp.
   */
  public List<ConsumptionMonthlyTotal> historyByWindows(
      final List<Instant> windowStarts,
      final List<Instant> windowEnds,
      final List<LocalDate> windowLabels)
  {
    if (windowStarts.isEmpty()) {
      return List.of();
    }
    Instant oldestStart = windowStarts.stream().min(Instant::compareTo).orElseThrow();
    Instant newestEnd = windowEnds.stream().max(Instant::compareTo).orElseThrow();
    Field<LocalDate> bucketCase = windowBucketCase(windowStarts, windowEnds, windowLabels);
    Field<LocalDate> windowStartCol = DSL.field("window_start", LocalDate.class);
    Field<Integer> componentCountCol = DSL.field("component_count", Integer.class);
    try (TransactionContext tx = createTransactionContext()) {
      Table<?> inner = tx.dsl()
          .select(bucketCase.as("window_start"),
              CONSUMPTION_EVENTS.COMPONENT_COUNT.as("component_count"))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(oldestStart)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(newestEnd)))
          .asTable("e");
      return tx.dsl()
          .select(windowStartCol, DSL.sum(componentCountCol))
          .from(inner)
          .where(windowStartCol.isNotNull())
          .groupBy(windowStartCol)
          .orderBy(windowStartCol.asc())
          .fetch(record -> new ConsumptionMonthlyTotal(
              record.get(0, LocalDate.class),
              record.get(1, Long.class) != null ? record.get(1, Long.class) : 0L));
    }
  }

  /**
   * Get history with per-activity-type breakdown bucketed by billing window.
   */
  public List<ConsumptionMonthlyBreakdown> historyWithBreakdownByWindows(
      final List<Instant> windowStarts,
      final List<Instant> windowEnds,
      final List<LocalDate> windowLabels)
  {
    return groupedHistoryByWindows(windowStarts, windowEnds, windowLabels,
        CONSUMPTION_EVENTS.ACTIVITY_TYPE, "activity_type");
  }

  /**
   * Get history grouped by source bucketed by billing window.
   */
  public List<ConsumptionMonthlyBreakdown> historyBySourceByWindows(
      final List<Instant> windowStarts,
      final List<Instant> windowEnds,
      final List<LocalDate> windowLabels)
  {
    return groupedHistoryByWindows(windowStarts, windowEnds, windowLabels,
        CONSUMPTION_EVENTS.SOURCE, "source");
  }

  /**
   * Get history grouped by IQ stage (from policy_evaluation.stage_type_id) bucketed by billing
   * window. Events without a matching policy_evaluation row, or with a null scan_id, are bucketed
   * under "Unknown".
   */
  public List<ConsumptionMonthlyBreakdown> historyByStageByWindows(
      final List<Instant> windowStarts,
      final List<Instant> windowEnds,
      final List<LocalDate> windowLabels)
  {
    if (windowStarts.isEmpty()) {
      return List.of();
    }
    Instant oldestStart = windowStarts.stream().min(Instant::compareTo).orElseThrow();
    Instant newestEnd = windowEnds.stream().max(Instant::compareTo).orElseThrow();
    Field<LocalDate> bucketCase = windowBucketCase(windowStarts, windowEnds, windowLabels);
    Field<LocalDate> windowStartCol = DSL.field("window_start", LocalDate.class);
    Field<String> stageCol = DSL.field("stage", String.class);
    Field<Integer> componentCountCol = DSL.field("component_count", Integer.class);
    Field<String> stageOrUnknown = DSL.coalesce(POLICY_EVALUATION.STAGE_TYPE_ID, DSL.inline(STAGE_UNKNOWN));
    try (TransactionContext tx = createTransactionContext()) {
      Table<?> inner = tx.dsl()
          .select(bucketCase.as("window_start"),
              stageOrUnknown.as("stage"),
              CONSUMPTION_EVENTS.COMPONENT_COUNT.as("component_count"))
          .from(CONSUMPTION_EVENTS)
          // Match to the canonical policy_evaluation row for each consumption event:
          // 1. APPLICATION_ID match — `scan_id` is unique only within an application
          // (PolicyEvaluationDAO.getLastByOwnerIdAndScanId always pairs
          // scan_id with applicationId), so cross-application scan_id collisions
          // would otherwise multiply SUM(component_count).
          // 2. REEVALUATION=false — re-evaluation reuses an existing scan_id, producing
          // a second row with reevaluation=true; without this filter SUM doubles.
          // (Continuous-monitoring evaluations always generate a fresh scan_id, so
          // they produce a single row with reevaluation=false, for_monitoring=true
          // and bucket cleanly under their actual stage.)
          // 3. FOR_OBSOLETE_SCAN=false — for consistency with the other queries in
          // PolicyEvaluationDAO (lines 305, 357, 518, 554, 651, 673). The
          // persistence layer (PolicyEvaluationDAO.validate) currently rejects
          // for_obsolete_scan=true paired with reevaluation=false, so this is
          // belt-and-suspenders today, but cheap insurance if validate is relaxed.
          .leftJoin(POLICY_EVALUATION)
          .on(POLICY_EVALUATION.SCAN_ID.eq(CONSUMPTION_EVENTS.SCAN_ID)
              .and(POLICY_EVALUATION.OWNER_ID.eq(CONSUMPTION_EVENTS.APP_ID))
              .and(POLICY_EVALUATION.REEVALUATION.isFalse())
              .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.isFalse()))
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(oldestStart)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(newestEnd)))
          .asTable("e");
      return tx.dsl()
          .select(windowStartCol, stageCol, DSL.sum(componentCountCol))
          .from(inner)
          .where(windowStartCol.isNotNull())
          .groupBy(windowStartCol, stageCol)
          .orderBy(windowStartCol.asc())
          .fetch(record -> new ConsumptionMonthlyBreakdown(
              record.get(0, LocalDate.class),
              record.get(1, String.class),
              record.get(2, Long.class) != null ? record.get(2, Long.class) : 0L));
    }
  }

  private List<ConsumptionMonthlyBreakdown> groupedHistoryByWindows(
      final List<Instant> windowStarts,
      final List<Instant> windowEnds,
      final List<LocalDate> windowLabels,
      final Field<String> groupKeyField,
      final String groupKeyAlias)
  {
    if (windowStarts.isEmpty()) {
      return List.of();
    }
    Instant oldestStart = windowStarts.stream().min(Instant::compareTo).orElseThrow();
    Instant newestEnd = windowEnds.stream().max(Instant::compareTo).orElseThrow();
    Field<LocalDate> bucketCase = windowBucketCase(windowStarts, windowEnds, windowLabels);
    Field<LocalDate> windowStartCol = DSL.field("window_start", LocalDate.class);
    Field<String> groupKeyCol = DSL.field(groupKeyAlias, String.class);
    Field<Integer> componentCountCol = DSL.field("component_count", Integer.class);
    try (TransactionContext tx = createTransactionContext()) {
      Table<?> inner = tx.dsl()
          .select(bucketCase.as("window_start"),
              groupKeyField.as(groupKeyAlias),
              CONSUMPTION_EVENTS.COMPONENT_COUNT.as("component_count"))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(oldestStart)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(newestEnd)))
          .asTable("e");
      return tx.dsl()
          .select(windowStartCol, groupKeyCol, DSL.sum(componentCountCol))
          .from(inner)
          .where(windowStartCol.isNotNull())
          .groupBy(windowStartCol, groupKeyCol)
          .orderBy(windowStartCol.asc())
          .fetch(record -> new ConsumptionMonthlyBreakdown(
              record.get(0, LocalDate.class),
              record.get(1, String.class),
              record.get(2, Long.class) != null ? record.get(2, Long.class) : 0L));
    }
  }

  /**
   * Get top consuming applications for a given billing month, ranked by total component count.
   */
  public List<ConsumptionAppTotal> topAppsByMonth(final LocalDate billingMonth, final int limit) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(
              CONSUMPTION_EVENTS.APP_ID,
              APPLICATION.PUBLIC_ID,
              APPLICATION.NAME,
              DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .leftJoin(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(CONSUMPTION_EVENTS.APP_ID))
          .where(CONSUMPTION_EVENTS.BILLING_MONTH.eq(billingMonth))
          .and(CONSUMPTION_EVENTS.APP_ID.isNotNull())
          .groupBy(CONSUMPTION_EVENTS.APP_ID, APPLICATION.PUBLIC_ID, APPLICATION.NAME)
          .orderBy(DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT).desc())
          .limit(limit)
          .fetch(record -> new ConsumptionAppTotal(
              record.get(CONSUMPTION_EVENTS.APP_ID),
              record.get(APPLICATION.PUBLIC_ID),
              record.get(APPLICATION.NAME),
              record.get(3, Long.class) != null ? record.get(3, Long.class) : 0L));
    }
  }

  /**
   * Get top consuming applications whose timestamp falls within [start, end), ranked by total
   * component count.
   */
  public List<ConsumptionAppTotal> topAppsByRange(final Instant start, final Instant end, final int limit) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(
              CONSUMPTION_EVENTS.APP_ID,
              APPLICATION.PUBLIC_ID,
              APPLICATION.NAME,
              DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .leftJoin(APPLICATION)
          .on(APPLICATION.APPLICATION_ID.eq(CONSUMPTION_EVENTS.APP_ID))
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .and(CONSUMPTION_EVENTS.APP_ID.isNotNull())
          .groupBy(CONSUMPTION_EVENTS.APP_ID, APPLICATION.PUBLIC_ID, APPLICATION.NAME)
          .orderBy(DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT).desc())
          .limit(limit)
          .fetch(record -> new ConsumptionAppTotal(
              record.get(CONSUMPTION_EVENTS.APP_ID),
              record.get(APPLICATION.PUBLIC_ID),
              record.get(APPLICATION.NAME),
              record.get(3, Long.class) != null ? record.get(3, Long.class) : 0L));
    }
  }

  public int countDistinctAppsByMonth(final LocalDate billingMonth) {
    try (TransactionContext tx = createTransactionContext()) {
      Integer result = tx.dsl()
          .select(DSL.countDistinct(CONSUMPTION_EVENTS.APP_ID))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.BILLING_MONTH.eq(billingMonth))
          .and(CONSUMPTION_EVENTS.APP_ID.isNotNull())
          .fetchOne(0, Integer.class);
      return result != null ? result : 0;
    }
  }

  public int countDistinctAppsByRange(final Instant start, final Instant end) {
    try (TransactionContext tx = createTransactionContext()) {
      Integer result = tx.dsl()
          .select(DSL.countDistinct(CONSUMPTION_EVENTS.APP_ID))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .and(CONSUMPTION_EVENTS.APP_ID.isNotNull())
          .fetchOne(0, Integer.class);
      return result != null ? result : 0;
    }
  }

  /**
   * Get daily consumption totals for the last N days, ordered by day ascending.
   */
  public List<ConsumptionDailyTotal> dailyHistory(final int days, final Instant now) {
    try (TransactionContext tx = createTransactionContext()) {
      Field<LocalDate> dayField = DSL.cast(CONSUMPTION_EVENTS.EVENT_TIMESTAMP, LocalDate.class);
      return tx.dsl()
          .select(dayField, DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP
              .greaterOrEqual(Date.from(now.minus(Duration.ofDays(days)))))
          .groupBy(dayField)
          .orderBy(dayField.asc())
          .fetch(record -> new ConsumptionDailyTotal(
              record.get(0, LocalDate.class),
              record.get(1, Long.class) != null ? record.get(1, Long.class) : 0L));
    }
  }

  /**
   * Get daily consumption totals for events whose timestamp falls within [start, end),
   * grouped by calendar day. Used to render the current billing window as a daily chart.
   * <p>
   * Day grouping uses {@code DSL.cast(EVENT_TIMESTAMP, DATE)} which respects the JVM/DB
   * session timezone. Production deployments run TZ=UTC (operational contract; not enforced
   * in code). Matches the project pattern in RepositoryComponentDAO, PolicyWaiverDAO, etc.
   */
  public List<ConsumptionDailyTotal> dailyHistoryByWindow(final Instant start, final Instant end) {
    try (TransactionContext tx = createTransactionContext()) {
      Field<LocalDate> dayField = DSL.cast(CONSUMPTION_EVENTS.EVENT_TIMESTAMP, LocalDate.class);
      return tx.dsl()
          .select(dayField, DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .groupBy(dayField)
          .orderBy(dayField.asc())
          .fetch(record -> new ConsumptionDailyTotal(
              record.get(0, LocalDate.class),
              record.get(1, Long.class) != null ? record.get(1, Long.class) : 0L));
    }
  }

  /**
   * Get daily consumption with per-activity-type breakdown for events whose timestamp falls
   * within [start, end). Used by the in-window Daily chart.
   * <p>
   * Day grouping uses {@code DSL.cast(EVENT_TIMESTAMP, DATE)} which respects the JVM/DB
   * session timezone. Production deployments run TZ=UTC (operational contract; not enforced
   * in code). Matches the project pattern in RepositoryComponentDAO, PolicyWaiverDAO, etc.
   */
  public List<ConsumptionMonthlyBreakdown> dailyHistoryWithBreakdownByWindow(
      final Instant start,
      final Instant end)
  {
    try (TransactionContext tx = createTransactionContext()) {
      Field<LocalDate> dayField = DSL.cast(CONSUMPTION_EVENTS.EVENT_TIMESTAMP, LocalDate.class);
      return tx.dsl()
          .select(dayField, CONSUMPTION_EVENTS.ACTIVITY_TYPE, DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .groupBy(dayField, CONSUMPTION_EVENTS.ACTIVITY_TYPE)
          .orderBy(dayField.asc())
          .fetch(record -> new ConsumptionMonthlyBreakdown(
              record.get(0, LocalDate.class),
              record.get(CONSUMPTION_EVENTS.ACTIVITY_TYPE),
              record.get(2, Long.class) != null ? record.get(2, Long.class) : 0L));
    }
  }

  /**
   * Get weekly consumption with per-activity-type breakdown bucketed by 7-day windows
   * anchored at the current billing-window start. Each weekStart in the input corresponds to
   * a 7-day window [weekStart, weekStart+7). Same subquery-bucketing pattern as
   * {@link #historyWithBreakdownByWindows}.
   */
  public List<ConsumptionMonthlyBreakdown> weeklyHistoryWithBreakdownByWindows(
      final List<Instant> weekStarts,
      final List<Instant> weekEnds,
      final List<LocalDate> weekLabels)
  {
    return groupedHistoryByWindows(
        weekStarts, weekEnds, weekLabels, CONSUMPTION_EVENTS.ACTIVITY_TYPE, "activity_type");
  }

  /**
   * Get total consumption for events whose timestamp falls within [start, end).
   * Returns a single-element list containing the aggregate total labelled with
   * the UTC calendar date of {@code start}. Used when an explicit date range is provided
   * instead of the computed billing-window series.
   */
  public List<ConsumptionMonthlyTotal> historyByWindowsWithRange(final Instant start, final Instant end) {
    try (TransactionContext tx = createTransactionContext()) {
      Long total = tx.dsl()
          .select(DSL.sum(CONSUMPTION_EVENTS.COMPONENT_COUNT))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .fetchOne(0, Long.class);
      LocalDate label = LocalDate.ofInstant(start, ZoneOffset.UTC);
      return List.of(new ConsumptionMonthlyTotal(label, total != null ? total : 0L));
    }
  }

  /**
   * Get consumption with per-activity-type breakdown for events whose timestamp falls within
   * [start, end). Rows are labelled with the UTC calendar date of {@code start}.
   * Used when an explicit date range is provided instead of the computed billing-window series.
   */
  public List<ConsumptionMonthlyBreakdown> historyWithBreakdownByWindowsWithRange(
      final Instant start,
      final Instant end)
  {
    return groupedHistoryByRange(start, end, CONSUMPTION_EVENTS.ACTIVITY_TYPE, "activity_type");
  }

  /**
   * Get consumption grouped by source for events whose timestamp falls within [start, end).
   * Rows are labelled with the UTC calendar date of {@code start}.
   */
  public List<ConsumptionMonthlyBreakdown> historyBySourceByWindowsWithRange(
      final Instant start,
      final Instant end)
  {
    return groupedHistoryByRange(start, end, CONSUMPTION_EVENTS.SOURCE, "source");
  }

  /**
   * Get consumption grouped by IQ stage for events whose timestamp falls within [start, end).
   * Events without a matching policy_evaluation row are bucketed under "Unknown".
   * Rows are labelled with the UTC calendar date of {@code start}.
   */
  public List<ConsumptionMonthlyBreakdown> historyByStageByWindowsWithRange(
      final Instant start,
      final Instant end)
  {
    LocalDate label = LocalDate.ofInstant(start, ZoneOffset.UTC);
    Field<String> stageOrUnknown = DSL.coalesce(POLICY_EVALUATION.STAGE_TYPE_ID, DSL.inline(STAGE_UNKNOWN));
    Field<String> stageCol = DSL.field("stage", String.class);
    Field<Integer> componentCountCol = DSL.field("component_count", Integer.class);
    try (TransactionContext tx = createTransactionContext()) {
      Table<?> inner = tx.dsl()
          .select(stageOrUnknown.as("stage"),
              CONSUMPTION_EVENTS.COMPONENT_COUNT.as("component_count"))
          .from(CONSUMPTION_EVENTS)
          .leftJoin(POLICY_EVALUATION)
          .on(POLICY_EVALUATION.SCAN_ID.eq(CONSUMPTION_EVENTS.SCAN_ID)
              .and(POLICY_EVALUATION.OWNER_ID.eq(CONSUMPTION_EVENTS.APP_ID))
              .and(POLICY_EVALUATION.REEVALUATION.isFalse())
              .and(POLICY_EVALUATION.FOR_OBSOLETE_SCAN.isFalse()))
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .asTable("e");
      LocalDate finalLabel = label;
      return tx.dsl()
          .select(stageCol, DSL.sum(componentCountCol))
          .from(inner)
          .groupBy(stageCol)
          .orderBy(stageCol.asc())
          .fetch(record -> new ConsumptionMonthlyBreakdown(
              finalLabel,
              record.get(0, String.class),
              record.get(1, Long.class) != null ? record.get(1, Long.class) : 0L));
    }
  }

  private List<ConsumptionMonthlyBreakdown> groupedHistoryByRange(
      final Instant start,
      final Instant end,
      final Field<String> groupKeyField,
      final String groupKeyAlias)
  {
    LocalDate label = LocalDate.ofInstant(start, ZoneOffset.UTC);
    Field<String> groupKeyCol = DSL.field(groupKeyAlias, String.class);
    Field<Integer> componentCountCol = DSL.field("component_count", Integer.class);
    try (TransactionContext tx = createTransactionContext()) {
      Table<?> inner = tx.dsl()
          .select(groupKeyField.as(groupKeyAlias),
              CONSUMPTION_EVENTS.COMPONENT_COUNT.as("component_count"))
          .from(CONSUMPTION_EVENTS)
          .where(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.greaterOrEqual(Date.from(start)))
          .and(CONSUMPTION_EVENTS.EVENT_TIMESTAMP.lessThan(Date.from(end)))
          .asTable("e");
      LocalDate finalLabel = label;
      return tx.dsl()
          .select(groupKeyCol, DSL.sum(componentCountCol))
          .from(inner)
          .groupBy(groupKeyCol)
          .orderBy(groupKeyCol.asc())
          .fetch(record -> new ConsumptionMonthlyBreakdown(
              finalLabel,
              record.get(0, String.class),
              record.get(1, Long.class) != null ? record.get(1, Long.class) : 0L));
    }
  }
}
