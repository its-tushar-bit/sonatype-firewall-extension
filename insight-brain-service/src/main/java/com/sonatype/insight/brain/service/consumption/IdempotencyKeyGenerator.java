/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sonatype.insight.brain.model.consumption.ActivityType;

import com.google.common.hash.Hashing;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the deterministic idempotency key stamped onto every keyable {@link
 * com.sonatype.insight.brain.model.consumption.ConsumptionEvent}. The key is opaque
 * to consumers — the only operation against it is equality comparison via the
 * partial unique index on {@code consumption_events.idempotency_key}.
 *
 * <p>
 * Per-activity-type key shapes (see CLM-40771 design doc §7.1):
 * <ul>
 * <li>{@code COMPONENT_DETAILS}/{@code VERSION_RECOMMENDATION}/{@code REACHABILITY}:
 * {@code userId:TYPE:entityId:scanId:sessionIdHash}</li>
 * <li>{@code DEVELOPER_PRIORITIES}: {@code userId:TYPE:entityId:appId:sessionIdHash}</li>
 * <li>{@code APP_SCAN}/{@code RE_EVALUATE}/{@code CONTINUOUS_MONITORING}:
 * {@code userId:TYPE:scanId} (session-less; background events)</li>
 * <li>{@code API}: {@code userId:API:requestId} (session-less)</li>
 * <li>{@code OTHERS}: always {@code null} (excluded from dedup)</li>
 * </ul>
 *
 * <p>
 * Returns {@code null} when any required field for the activity-type's shape is
 * missing — the partial unique index lets {@code NULL}-keyed rows insert freely.
 *
 * <p>
 * <b>scanId contract (3-segment shapes):</b> {@code APP_SCAN}, {@code RE_EVALUATE},
 * and {@code CONTINUOUS_MONITORING} use the 2-tenant-relevant-segment key
 * {@code userId:TYPE:scanId} — cross-tenant isolation depends entirely on
 * {@code scanId} being globally unique. The current production caller
 * ({@code ScanTask}) generates {@code scanId} via {@code UUID.randomUUID()}, so the
 * guarantee holds. Future callers populating {@code ConsumptionContext.scanId} for
 * these activity types MUST emit a globally unique value (preferably a UUID) — a
 * non-unique scanId (e.g. a DB row id namespaced per-tenant) would break tenant
 * isolation for billing on these three types.
 *
 * @since 1.205 (CLM-40771)
 */
public final class IdempotencyKeyGenerator
{
  private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyGenerator.class);

  private static final String SEP = ":";

  /**
   * SHA-256 truncated to 16 hex chars (64 bits) for the sessionId segment of keys.
   * Birthday-paradox collision probability stays well below 1e-9 at 2^32 keys, with
   * comfortable headroom for the largest tenants. The session hash is one of five
   * key segments — collisions only matter when all other segments also match — so
   * effective collision space is tighter still.
   */
  private static final int SESSION_HASH_LENGTH = 16;

  /**
   * Hard cap matching the {@code consumption_events.idempotency_key} column width.
   * If the joined key exceeds this (pathological username + entity-id combination),
   * fall back to {@code null} so the event still inserts as an unkeyed row rather
   * than failing the INSERT and being silently dropped by the recorder's catch-warn.
   */
  private static final int MAX_KEY_LENGTH = 255;

  /**
   * Sample dedup for the overflow warn-log. Set membership keys on
   * {@code TYPE:userIdLengthBucket}; the first occurrence in each bucket logs,
   * subsequent ones are suppressed. Unbounded growth is bounded by the number of
   * distinct (ActivityType, userId-length/32) pairs the process sees in its lifetime,
   * which is at most a few dozen.
   */
  private static final Set<String> overflowsSeen = ConcurrentHashMap.newKeySet();

  private IdempotencyKeyGenerator() {
  }

  @Nullable
  public static String generate(
      final ActivityType type,
      final ConsumptionContext ctx,
      @Nullable final String entityId)
  {
    if (type == null || ctx == null) {
      return null;
    }
    final String userId = ctx.getUserId();
    if (userId == null) {
      return debugNoKey(type, "userId is null");
    }
    String key = switch (type) {
      case COMPONENT_DETAILS, VERSION_RECOMMENDATION, REACHABILITY -> {
        String sessHash = sessionIdHash(ctx.getSessionId());
        if (allNonNull(entityId, ctx.getScanId(), sessHash)) {
          yield join(userId, type.name(), entityId, ctx.getScanId(), sessHash);
        }
        yield debugNoKey(type, String.format("entityId=%s scanId=%s sessionId=%s",
            entityId, ctx.getScanId(), ctx.getSessionId() == null ? "null" : "set"));
      }
      case DEVELOPER_PRIORITIES -> {
        String sessHash = sessionIdHash(ctx.getSessionId());
        if (allNonNull(entityId, ctx.getAppId(), sessHash)) {
          yield join(userId, type.name(), entityId, ctx.getAppId(), sessHash);
        }
        yield debugNoKey(type, String.format("entityId=%s appId=%s sessionId=%s",
            entityId, ctx.getAppId(), ctx.getSessionId() == null ? "null" : "set"));
      }
      case APP_SCAN, RE_EVALUATE, CONTINUOUS_MONITORING -> ctx.getScanId() != null
          ? join(userId, type.name(), ctx.getScanId())
          : debugNoKey(type, "scanId is null");
      // API events are billed per-call: the only direct-API caller in HdsClient.emitEvent
      // explicitly passes entityId=null so events land as unkeyed rows (no dedup). The
      // case still accepts a non-null entityId so non-HDS API callers (if added) can opt
      // in by passing a per-call requestId.
      case API -> entityId != null
          ? join(userId, type.name(), entityId)
          : debugNoKey(type, "entityId is null");
      case OTHERS -> null;
    };
    if (key != null && key.length() > MAX_KEY_LENGTH) {
      // Overflow is rare and customer-billing-impacting: when it fires, dedup is silently
      // disabled for the affected (user, entity, scan, session) and every event for that
      // engagement lands as an unkeyed row — over-billing the tenant. Realistic trigger
      // is a SAML/LDAP DN-style username (~150 chars) combined with a 40-char component
      // hash. Warn (not debug) so ops surfaces it before a customer notices a bill skew.
      // Sampled to first-occurrence per (type, length-bucket) to keep log volume bounded
      // for high-throughput pathological tenants.
      logOverflowSampled(type, userId, key.length());
      return null;
    }
    return key;
  }

  /**
   * First-seen-only warn-log per (type, userId-length-bucket) to keep volume bounded
   * when a pathological tenant repeatedly triggers the cap. Length is bucketed in
   * 32-char bins so the second-occurrence dedup remains meaningful even when userIds
   * vary slightly within the same tenant.
   */
  private static void logOverflowSampled(ActivityType type, String userId, int generatedLength) {
    int bucket = userId.length() / 32;
    String dedupKey = type.name() + ":" + bucket;
    if (overflowsSeen.add(dedupKey)) {
      log.warn(
          "Idempotency key length {} exceeds {} chars for {} (userId length {}); event will land unkeyed and dedup is disabled for this engagement. "
              + "Sampled — further occurrences in the same (type, userId-length/32) bucket are suppressed.",
          generatedLength, MAX_KEY_LENGTH, type, userId.length());
    }
  }

  /**
   * Log at debug and return null. Centralizes the diagnostic for cases where a key
   * could not be generated for a keyable activity type — useful when investigating
   * why dedup isn't firing in a given environment. Not emitted for OTHERS, which
   * legitimately has no key shape.
   */
  @Nullable
  private static String debugNoKey(ActivityType type, String reason) {
    log.debug("Cannot generate idempotency key for {}: {}", type, reason);
    return null;
  }

  private static boolean allNonNull(final Object... vals) {
    for (Object v : vals) {
      if (v == null) {
        return false;
      }
    }
    return true;
  }

  private static String join(final String... parts) {
    return String.join(SEP, parts);
  }

  @Nullable
  private static String sessionIdHash(@Nullable final String sessionId) {
    if (sessionId == null) {
      return null;
    }
    return Hashing.sha256()
        .hashString(sessionId, StandardCharsets.UTF_8)
        .toString()
        .substring(0, SESSION_HASH_LENGTH);
  }
}
