/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.hash.Hashing;

import com.sonatype.insight.brain.model.consumption.ActivityType;

import jakarta.annotation.Nullable;

/**
 * Resolves an HDS request path to a {@link PathMatch} carrying both the
 * {@link ActivityType} and an optional entity id (typically a component hash)
 * extracted from the URI. Paths not in the allow-list are not counted —
 * this is an explicit allow-list; new HDS endpoints are not counted by
 * default.
 *
 * <p>
 * Replaces {@code HdsPathActivityMapper} (which returned only
 * {@link ActivityType}). The entity-id segment is required by
 * {@link IdempotencyKeyGenerator} for {@code COMPONENT_DETAILS},
 * {@code VERSION_RECOMMENDATION}, and {@code REACHABILITY}.
 *
 * <p>
 * For {@code DEVELOPER_PRIORITIES}, {@link PathMatch#entityId()} is
 * always {@code null} because the path
 * {@code /rest/vulnerability/affected} does not encode the affected
 * component identity in the URI; that entity comes from the response
 * payload (see {@code DeveloperPrioritiesPayloadExtractor}).
 *
 * <p>
 * For the legacy {@code /componentDetails} path, {@link PathMatch#entityId()} is
 * a 16-hex SHA-256 prefix of the URL-decoded {@code componentIdentifier=} JSON
 * when present — because that JSON is carried by every {@code /componentDetails}
 * call shape and allows the entry-page call (which also has {@code hash=}) and the
 * sub-mount calls (which do not) to collapse to the same entity id. Falls back to
 * the literal {@code hash=} value only when {@code componentIdentifier=} is absent.
 *
 * @since 1.205 (CLM-40771; renamed from HdsPathActivityMapper)
 */
public final class HdsPathExtractor
{
  /**
   * Result of resolving a path.
   *
   * @param activityType the resolved consumption activity type
   * @param entityId the URI-extracted entity id (usually a component hash);
   *          {@code null} when the path does not encode an entity id
   */
  public record PathMatch(ActivityType activityType, @Nullable String entityId)
  {
  }

  @FunctionalInterface
  private interface EntityExtractor
  {
    @Nullable
    String extract(String path);
  }

  /**
   * Derives a stable entity id from the POST request body when the path extractor cannot
   * (e.g. the entity identifier is not present in the URI).
   */
  @FunctionalInterface
  public interface RequestBodyExtractor
  {
    @Nullable
    String extract(@Nullable Object requestBody);
  }

  private record Mapping(ActivityType activityType, EntityExtractor pathExtractor, RequestBodyExtractor bodyExtractor)
  {
  }

  private static final EntityExtractor NO_ENTITY = path -> null;

  private static final RequestBodyExtractor NO_BODY = body -> null;

  /**
   * HDS path substrings whose calls are deliberately NOT counted as consumption. These
   * are bulk / cross-version metadata endpoints whose URL carries no per-engagement
   * entity id, so the path-extractor would either yield an unkeyed event (just noise)
   * or — worse — extract a literal segment ("list") that collapses every bulk batch in
   * the same (user, scan, session) into a single dedup'd row, undercounting use.
   *
   * <p>
   * Checked BEFORE {@link #MAPPINGS} so longer skip patterns short-circuit before
   * shorter mappings ({@code /componentDetails/list} would otherwise fall through to
   * the {@code /componentDetails} mapping; {@code /version-scoring/list} would
   * otherwise be keyed as {@code ...:VR:list:scan:session}).
   *
   * <p>
   * <b>Contract:</b> entries here must be fully disjoint from entries in
   * {@link #MAPPINGS} — no prefix overlap in either direction. Today the only overlap
   * is intentional (e.g. {@code /componentDetails/list} extends the
   * {@code /componentDetails} mapping, and the iteration order makes the skip win).
   * Adding a NEW skip substring that is a prefix or substring of an unrelated mapped
   * path would silently shadow that mapping; future additions should be paired with a
   * test covering the affected mappings.
   *
   * @since 1.205 (CLM-40771 follow-up)
   */
  private static final List<String> SKIPPED_SUBSTRINGS = List.of(
      "/componentDetails/list",
      "/version-scoring/list");

  /**
   * Build an entity extractor that captures the URI segment immediately
   * following {@code pathSegmentPrefix} (stopping at the next {@code /}
   * or {@code ?}).
   */
  private static EntityExtractor hashAfter(final String pathSegmentPrefix) {
    Pattern p = Pattern.compile(Pattern.quote(pathSegmentPrefix) + "/([^/?]+)");
    return path -> {
      Matcher matcher = p.matcher(path);
      return matcher.find() ? matcher.group(1) : null;
    };
  }

  /**
   * Component-details entity extractor. Prefers a SHA-256-truncated hash of the
   * URL-decoded {@code componentIdentifier=} JSON when present, because that JSON
   * is a complete identity for the user's engagement (group:artifact:version:format)
   * and is carried by every {@code /componentDetails} call shape — both the
   * entry-page request (which also has {@code hash=}) and the no-hash sub-mount
   * requests (Compare drill-in, tab refresh).
   *
   * <p>
   * Falls back to {@code hash=} only when {@code componentIdentifier=} is
   * absent — defense-in-depth for any caller that supplies just the content hash.
   *
   * <p>
   * Why this priority order: a single user-perceived engagement triggers multiple
   * HDS calls (entry plus sub-mount). Some have {@code hash=}, some don't. To dedup
   * them as one engagement we must use a key derivable from BOTH call shapes;
   * {@code componentIdentifier=} is the only field carried by both.
   */
  @SuppressWarnings("UnstableApiUsage")
  private static EntityExtractor componentIdentifierOrHash() {
    Pattern ciPattern = Pattern.compile("[?&]componentIdentifier=([^&]+)");
    Pattern hashPattern = Pattern.compile("[?&]hash=([^&]+)");
    return path -> {
      if (path == null) {
        return null;
      }
      Matcher ciMatcher = ciPattern.matcher(path);
      if (ciMatcher.find()) {
        String encoded = ciMatcher.group(1);
        String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        return Hashing.sha256()
            .hashString(decoded, StandardCharsets.UTF_8)
            .toString()
            .substring(0, 16);
      }
      Matcher hashMatcher = hashPattern.matcher(path);
      if (hashMatcher.find()) {
        return hashMatcher.group(1);
      }
      return null;
    };
  }

  /**
   * The ordered allow-list of path substrings to activity-type mappings.
   * Keys are also used by {@link #getPathMappings()} to expose the activity-type
   * mapping surface for tests.
   */
  private static final Map<String, Mapping> MAPPINGS;

  static {
    LinkedHashMap<String, Mapping> m = new LinkedHashMap<>();
    m.put("rest/component/details/evaluation",
        new Mapping(ActivityType.COMPONENT_DETAILS, hashAfter("rest/component/details/evaluation"), NO_BODY));
    m.put("rest/component/details/integration",
        new Mapping(ActivityType.COMPONENT_DETAILS, hashAfter("rest/component/details/integration"), NO_BODY));
    // Legacy IQ UI Component Details: prefer hashed componentIdentifier=, fall back to hash=.
    m.put("/componentDetails",
        new Mapping(ActivityType.COMPONENT_DETAILS, componentIdentifierOrHash(), NO_BODY));
    // m.put("rest/component/versions", ActivityType.API);
    m.put("rest/component/version-scoring",
        new Mapping(ActivityType.VERSION_RECOMMENDATION, hashAfter("rest/component/version-scoring"), NO_BODY));
    // ComponentRemediationService posts PURLs in the request body — derive entityId from the body.
    m.put("rest/component/dependencies",
        new Mapping(ActivityType.VERSION_RECOMMENDATION, NO_ENTITY,
            PackageUrlIdentifierSetBodyExtractor::extract));
    m.put("rest/component/signatures/vulnerability",
        new Mapping(ActivityType.REACHABILITY, hashAfter("rest/component/signatures/vulnerability"), NO_BODY));
    m.put("rest/vulnerability/affected",
        new Mapping(ActivityType.DEVELOPER_PRIORITIES, NO_ENTITY, NO_BODY));
    MAPPINGS = Collections.unmodifiableMap(m);
  }

  /**
   * Returns an unmodifiable view of the path-to-activity-type mapping,
   * keyed by the path substring used for matching. Exposed for tests.
   */
  static Map<String, ActivityType> getPathMappings() {
    LinkedHashMap<String, ActivityType> result = new LinkedHashMap<>();
    MAPPINGS.forEach((key, mapping) -> result.put(key, mapping.activityType()));
    return Collections.unmodifiableMap(result);
  }

  private HdsPathExtractor() {
  }

  /**
   * Resolve an HDS request path to its activity type and entity id.
   *
   * @return the {@link PathMatch}, or {@code null} if the path is not in the allow-list
   */
  @Nullable
  public static PathMatch resolve(@Nullable final String requestPath) {
    return resolve(requestPath, null);
  }

  /**
   * Resolve an HDS request path to its activity type and entity id, optionally consulting
   * the POST request body when the path extractor yields no entity id.
   *
   * @param requestPath the HDS path-and-query string (may be {@code null})
   * @param requestBody the original POST body object, or {@code null} for GET requests
   * @return the {@link PathMatch}, or {@code null} if the path is not in the allow-list
   */
  @Nullable
  public static PathMatch resolve(@Nullable final String requestPath, @Nullable final Object requestBody) {
    if (requestPath == null) {
      return null;
    }
    for (String skipped : SKIPPED_SUBSTRINGS) {
      if (requestPath.contains(skipped)) {
        return null;
      }
    }
    for (Map.Entry<String, Mapping> entry : MAPPINGS.entrySet()) {
      if (requestPath.contains(entry.getKey())) {
        Mapping mapping = entry.getValue();
        String entityId = mapping.pathExtractor().extract(requestPath);
        if (entityId == null) {
          entityId = mapping.bodyExtractor().extract(requestBody);
        }
        return new PathMatch(mapping.activityType(), entityId);
      }
    }
    return null;
  }
}
