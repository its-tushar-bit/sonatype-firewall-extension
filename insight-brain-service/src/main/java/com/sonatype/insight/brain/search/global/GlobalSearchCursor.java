/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opaque, immutable, thread-safe base64 cursor for Global Search pagination. Carries the per-tab
 * {@code searchAfter} sort tuple plus a generation token pinning the cursor to the index/sort-allowlist
 * generation it was issued against; clients must round-trip the exact string.
 *
 * <p>
 * Encoding: URL-safe base64 of {@code "gen=<generation>;t=<sort1>,<sort2>,..."} with commas, semicolons
 * and backslashes in sort values backslash-escaped. The generation token is SHA-256 over a
 * length-prefixed concatenation of index generation, {@link GlobalSearchSortAllowlist#fingerprint()},
 * tab, sort key, page size, backend id, and tenant id — binding backend and tenant defeats cross-tenant /
 * cross-backend replay, and any component change invalidates in-flight cursors.
 */
public final class GlobalSearchCursor
{
  private static final Logger log = LoggerFactory.getLogger(GlobalSearchCursor.class);

  /** Anything longer is rejected unparsed, to defend against pathological inputs. */
  static final int MAX_ENCODED_LENGTH = 1024;

  private static final String GEN_PREFIX = "gen=";

  private static final String TUPLE_PREFIX = "t=";

  private static final AtomicReference<String> CURRENT_GENERATION =
      new AtomicReference<>(computeDefaultGenerationToken());

  private static String computeDefaultGenerationToken() {
    return computeGenerationToken("default", "default", GlobalSearchSortAllowlist.RELEVANCE, 25, "default", "");
  }

  private final String generationToken;

  private final List<String> sortValues;

  /** Most callers should prefer the {@link #newCursor(String, List)} factory. */
  public GlobalSearchCursor(final String generationToken, final List<String> sortValues) {
    Objects.requireNonNull(generationToken, "generationToken");
    Objects.requireNonNull(sortValues, "sortValues");
    if (generationToken.isEmpty()) {
      throw new IllegalArgumentException("generationToken must not be empty");
    }
    if (sortValues.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("sortValues must not contain null elements");
    }
    this.generationToken = generationToken;
    this.sortValues = Collections.unmodifiableList(new ArrayList<>(sortValues));
  }

  /**
   * Rejects a token containing {@code ;} up-front, so a hand-built cursor cannot disguise an
   * injected tuple boundary inside the {@code gen=...} segment.
   */
  public static GlobalSearchCursor newCursor(final String generationToken, final List<String> sortValues) {
    Objects.requireNonNull(generationToken, "generationToken");
    Objects.requireNonNull(sortValues, "sortValues");
    if (generationToken.isEmpty()) {
      throw new IllegalArgumentException("generationToken must not be empty");
    }
    if (generationToken.indexOf(';') >= 0) {
      throw new IllegalArgumentException("generationToken must not contain ';'");
    }
    return new GlobalSearchCursor(generationToken, sortValues);
  }

  public List<String> sortValues() {
    return sortValues;
  }

  public String generationToken() {
    return generationToken;
  }

  public String encode() {
    StringBuilder raw = new StringBuilder();
    raw.append(GEN_PREFIX).append(generationToken).append(';').append(TUPLE_PREFIX);
    boolean first = true;
    for (String v : sortValues) {
      if (!first) {
        raw.append(',');
      }
      raw.append(escape(v));
      first = false;
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toString().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Decode and validate against {@link #currentGenerationToken()}. Null/empty/malformed input throws
   * {@link IllegalArgumentException}; a stale generation token throws {@link StaleCursorException}
   * (mapped to HTTP 410).
   */
  public static GlobalSearchCursor decode(final String encoded) {
    if (StringUtils.isEmpty(encoded)) {
      throw new IllegalArgumentException("cursor must not be blank");
    }
    if (encoded.length() > MAX_ENCODED_LENGTH) {
      throw new IllegalArgumentException("cursor exceeds max length");
    }
    return decodeInternal(encoded, currentGenerationToken(), /* malformedAsStale */ false);
  }

  /**
   * Decode structure WITHOUT validating the embedded generation token; the caller must re-validate it
   * against the current request preimage.
   *
   * @throws IllegalArgumentException on null / oversized / non-base64 / structurally malformed input
   */
  static GlobalSearchCursor decodeUnvalidated(final String encoded) {
    if (StringUtils.isEmpty(encoded)) {
      throw new IllegalArgumentException("cursor must not be blank");
    }
    if (encoded.length() > MAX_ENCODED_LENGTH) {
      throw new IllegalArgumentException("cursor exceeds max length");
    }
    byte[] decoded;
    try {
      decoded = Base64.getUrlDecoder().decode(encoded);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("cursor is malformed", e);
    }
    String raw = new String(decoded, StandardCharsets.UTF_8);
    int sep = raw.indexOf(';' + TUPLE_PREFIX);
    if (sep < 0 || !raw.startsWith(GEN_PREFIX)) {
      throw new IllegalArgumentException("cursor is malformed");
    }
    String embeddedGen = raw.substring(GEN_PREFIX.length(), sep);
    String tuple = raw.substring(sep + 1 + TUPLE_PREFIX.length());
    List<String> sortValues;
    try {
      sortValues = splitEscaped(tuple);
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("cursor is malformed", e);
    }
    return new GlobalSearchCursor(embeddedGen, sortValues);
  }

  /**
   * Decode and validate against {@code expectedGenerationToken}. Any malformed/tampered/null/oversized
   * input is rejected as {@link StaleCursorException} — the HTTP 410 retry-from-page-1 outcome.
   */
  public static GlobalSearchCursor decode(final String encoded, final String expectedGenerationToken) {
    Objects.requireNonNull(expectedGenerationToken, "expectedGenerationToken");
    if (StringUtils.isEmpty(encoded)) {
      throw new StaleCursorException("cursor is empty");
    }
    if (encoded.length() > MAX_ENCODED_LENGTH) {
      throw new StaleCursorException("cursor exceeds max length");
    }
    return decodeInternal(encoded, expectedGenerationToken, /* malformedAsStale */ true);
  }

  private static GlobalSearchCursor decodeInternal(
      final String encoded,
      final String expectedGenerationToken,
      final boolean malformedAsStale)
  {
    byte[] decoded;
    try {
      decoded = Base64.getUrlDecoder().decode(encoded);
    }
    catch (IllegalArgumentException e) {
      if (malformedAsStale) {
        throw new StaleCursorException("cursor is not valid base64");
      }
      throw new IllegalArgumentException("cursor is malformed", e);
    }
    String raw = new String(decoded, StandardCharsets.UTF_8);

    int sep = raw.indexOf(';' + TUPLE_PREFIX);
    if (sep < 0 || !raw.startsWith(GEN_PREFIX)) {
      if (malformedAsStale) {
        throw new StaleCursorException("cursor is malformed");
      }
      throw new IllegalArgumentException("cursor is malformed");
    }
    String embeddedGen = raw.substring(GEN_PREFIX.length(), sep);
    if (!embeddedGen.equals(expectedGenerationToken)) {
      // Do NOT echo attacker-controlled embeddedGen in the response; log at DEBUG only.
      if (log.isDebugEnabled()) {
        log.debug("stale cursor: embedded token did not match expected token");
      }
      throw new StaleCursorException("cursor is stale; retry from page 1");
    }
    String tuple = raw.substring(sep + 1 + TUPLE_PREFIX.length());
    List<String> sortValues;
    try {
      sortValues = splitEscaped(tuple);
    }
    catch (IllegalArgumentException e) {
      if (malformedAsStale) {
        throw new StaleCursorException("cursor is malformed");
      }
      throw e;
    }
    return new GlobalSearchCursor(embeddedGen, sortValues);
  }

  /**
   * Compute the token pinning a cursor to its full context; any input change invalidates in-flight
   * cursors. {@code backendId} binding prevents cross-backend decode (backends emit different
   * {@code searchAfter} tuple shapes); {@code tenantId} binding prevents cross-tenant decode (sort
   * tuples encode docIds valid only against one tenant's snapshot). Each component is length-prefixed
   * (4-byte big-endian) so no boundary can be forged by injecting a separator inside a value.
   */
  public static String computeGenerationToken(
      final String indexGeneration,
      final String tabName,
      final String sortKey,
      final int pageSize,
      final String backendId,
      final String tenantId)
  {
    Objects.requireNonNull(indexGeneration, "indexGeneration");
    Objects.requireNonNull(tabName, "tabName");
    Objects.requireNonNull(sortKey, "sortKey");
    Objects.requireNonNull(backendId, "backendId");
    Objects.requireNonNull(tenantId, "tenantId");
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      updateWithLength(md, indexGeneration);
      updateWithLength(md, GlobalSearchSortAllowlist.fingerprint());
      updateWithLength(md, tabName);
      updateWithLength(md, sortKey);
      updateWithLength(md, Integer.toString(pageSize));
      updateWithLength(md, backendId);
      updateWithLength(md, tenantId);
      return HexFormat.of().formatHex(md.digest());
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void updateWithLength(final MessageDigest md, final String s) {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    byte[] len = new byte[4];
    len[0] = (byte) ((bytes.length >>> 24) & 0xff);
    len[1] = (byte) ((bytes.length >>> 16) & 0xff);
    len[2] = (byte) ((bytes.length >>> 8) & 0xff);
    len[3] = (byte) (bytes.length & 0xff);
    md.update(len);
    md.update(bytes);
  }

  /** Allowlist-only token component, exposed for tests. */
  static String computeAllowlistComponent() {
    return GlobalSearchSortAllowlist.fingerprint();
  }

  public static String currentGenerationToken() {
    return CURRENT_GENERATION.get();
  }

  /** Called from the index-rebuild hook on a full reindex / sort-allowlist update, and from tests. */
  public static void bumpGenerationToken(final String newToken) {
    Objects.requireNonNull(newToken, "newToken");
    if (newToken.isEmpty()) {
      throw new IllegalArgumentException("newToken must not be empty");
    }
    CURRENT_GENERATION.set(newToken);
  }

  private static String escape(final String v) {
    StringBuilder sb = new StringBuilder(v.length() + 4);
    for (int i = 0; i < v.length(); i++) {
      char c = v.charAt(i);
      if (c == '\\' || c == ',' || c == ';') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  // Permissive on escapes: cursors are server-minted, and tampering still fails token validation
  // before the contents are used.
  private static List<String> splitEscaped(final String s) {
    List<String> out = new ArrayList<>();
    if (s.isEmpty()) {
      return out;
    }
    StringBuilder cur = new StringBuilder();
    boolean escaped = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (escaped) {
        cur.append(c);
        escaped = false;
      }
      else if (c == '\\') {
        escaped = true;
      }
      else if (c == ',') {
        out.add(cur.toString());
        cur.setLength(0);
      }
      else if (c == ';') {
        // Unescaped ';' here can only be the gen/tuple separator leaking in — tampered encoding.
        throw new IllegalArgumentException("cursor tuple contains unescaped ';'");
      }
      else {
        cur.append(c);
      }
    }
    if (escaped) {
      throw new IllegalArgumentException("cursor has trailing escape");
    }
    out.add(cur.toString());
    return out;
  }
}
