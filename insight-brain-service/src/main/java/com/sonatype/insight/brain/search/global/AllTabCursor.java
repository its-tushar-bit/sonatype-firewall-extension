/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;

/**
 * Opaque, server-wide cursor returned in {@link ResultsResponse#getNextSearchAfter()} for {@link Tab#ALL}
 * responses. Wraps a per-section {@link SectionCursor} pair so {@link AllTabPacker} can resume mid-section
 * after a page boundary lands inside that section's current upstream page.
 *
 * <p>
 * Encoding: base64 of a compact ASCII string with the structural shape
 *
 * <pre>
 *   alltab:g0|COMPONENT=&lt;b64(inner)&gt;,3,0|VULNERABILITY=,0,0|APPLICATION=&lt;b64(inner)&gt;,7,0
 * </pre>
 *
 * Each per-section inner cursor (which may itself contain {@code |}, {@code =}, {@code ,}) is wrapped in
 * URL-safe base64 to keep the outer separators unambiguous.
 *
 * <p>
 * The generation token is derived from the ambient {@link GlobalSearchCursor#currentGenerationToken()}
 * pinned to the ALL discriminator plus the request's sort key and pageSize. Decoding rejects stale tokens
 * with {@link StaleCursorException}. Any malformed input (bad base64, missing magic, malformed segment,
 * unknown tab, oversized input) is also surfaced as {@link StaleCursorException} so the resource layer can
 * return HTTP 410 with the retry-from-page-1 hint uniformly.
 *
 * <p>
 * A request that changes sort or pageSize between pages produces a different expected token and rejects
 * with HTTP 410 rather than silently mispaging.
 *
 * <p>
 * Per-section cursors carry the supplier-defined {@code upstreamCursor} opaque string, the
 * {@code skipWithinPage} offset, and an {@code exhausted} marker. The pageSize used to mint the cursor
 * is reflected indirectly through the per-section inner cursors (the IQ-local and catalog legs include
 * pageSize in their own generation pin), so a request that shrinks pageSize between pages rejects via
 * {@link StaleCursorException} when the per-section cursor is validated by the per-tab dispatch.
 */
public final class AllTabCursor
{
  /**
   * Maximum encoded cursor length. Inputs longer than this are rejected as stale without parsing.
   */
  public static final int MAX_ENCODED_LENGTH = 4096;

  /**
   * Maximum decoded byte length of an inner per-section upstream cursor. An inner cursor larger than this
   * is rejected as stale without further parsing.
   */
  public static final int MAX_INNER_DECODED_LENGTH = 1024;

  private static final String MAGIC = "alltab:";

  private static final String GROUP_SEP = "|";

  private static final String FIELD_SEP = "=";

  private static final String SUB_SEP = ",";

  /**
   * Discriminator baked into the ALL-tab cursor's generation-token pin so a cursor minted for one tab
   * cannot be replayed against another.
   */
  static final String ALL_TAB_DISCRIMINATOR = "ALL";

  /** Backend identifier baked into the ALL-tab pin. */
  static final String ALL_TAB_BACKEND = "all-packer";

  private final String generationToken;

  private final Map<Tab, SectionCursor> sectionCursors;

  /**
   * Construct a cursor pinned to the current ambient generation, the ALL discriminator, and the supplied
   * sort key + pageSize + source. The source is folded into the backend id of the pin so a cursor minted
   * for {@code source=local} cannot be replayed against {@code source=catalog} and vice-versa.
   */
  public AllTabCursor(String sortKey, int pageSize, SearchSource source, Map<Tab, SectionCursor> sectionCursors) {
    this(computePin(sortKey, pageSize, source), sectionCursors);
  }

  /** Overload defaulting the source to {@link SearchSource#DEFAULT}. */
  @VisibleForTesting
  AllTabCursor(String sortKey, int pageSize, Map<Tab, SectionCursor> sectionCursors) {
    this(sortKey, pageSize, SearchSource.DEFAULT, sectionCursors);
  }

  AllTabCursor(String generationToken, Map<Tab, SectionCursor> sectionCursors) {
    this.generationToken = Objects.requireNonNull(generationToken, "generationToken");
    this.sectionCursors = new EnumMap<>(Tab.class);
    if (sectionCursors != null) {
      this.sectionCursors.putAll(sectionCursors);
    }
  }

  /**
   * @return the per-section cursor for {@code tab}, or {@code null} if that section has been exhausted (no
   *         more pages to read for this query).
   */
  public SectionCursor cursorFor(Tab tab) {
    return sectionCursors.get(tab);
  }

  public String getGenerationToken() {
    return generationToken;
  }

  public Map<Tab, SectionCursor> sectionCursors() {
    return Map.copyOf(sectionCursors);
  }

  /**
   * Compute the ALL-tab pin over the ambient generation token, the ALL discriminator, and the supplied
   * sort key + pageSize + source. Public for controller-side round-trip check.
   */
  public static String computePin(String sortKey, int pageSize, SearchSource source) {
    String effectiveSort = (sortKey == null || sortKey.isBlank()) ? GlobalSearchSortAllowlist.RELEVANCE : sortKey;
    SearchSource effectiveSource = source == null ? SearchSource.DEFAULT : source;
    return GlobalSearchCursor.computeGenerationToken(
        GlobalSearchCursor.currentGenerationToken(),
        ALL_TAB_DISCRIMINATOR,
        effectiveSort,
        pageSize,
        effectiveSource.value() + ":" + ALL_TAB_BACKEND,
        GlobalSearchTenancy.currentTenantId());
  }

  /** Overload defaulting the source to {@link SearchSource#DEFAULT}. */
  @VisibleForTesting
  static String computePin(String sortKey, int pageSize) {
    return computePin(sortKey, pageSize, SearchSource.DEFAULT);
  }

  /** Encode this cursor as the opaque {@code nextSearchAfter} value. */
  public String encode() {
    StringBuilder sb = new StringBuilder(MAGIC).append(generationToken);
    for (Map.Entry<Tab, SectionCursor> e : sectionCursors.entrySet()) {
      SectionCursor sc = e.getValue();
      String upstream = sc.upstreamCursor() == null ? "" : sc.upstreamCursor();
      String upstreamB64 = Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(upstream.getBytes(StandardCharsets.UTF_8));
      sb.append(GROUP_SEP)
          .append(e.getKey().name())
          .append(FIELD_SEP)
          .append(upstreamB64)
          .append(SUB_SEP)
          .append(sc.skipWithinPage())
          .append(SUB_SEP)
          .append(sc.exhausted() ? "1" : "0");
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Decode an encoded {@link AllTabCursor} and verify it was pinned to the supplied sort key, pageSize,
   * and source. Any malformed / oversized / mistyped / mismatched input is converted to
   * {@link StaleCursorException}.
   */
  public static AllTabCursor decode(String encoded, String sortKey, int pageSize, SearchSource source) {
    if (encoded == null || encoded.isBlank()) {
      throw new StaleCursorException("cursor is empty");
    }
    if (encoded.length() > MAX_ENCODED_LENGTH) {
      throw new StaleCursorException("cursor exceeds max length");
    }
    try {
      return decodeInternal(encoded, computePin(sortKey, pageSize, source));
    }
    catch (StaleCursorException e) {
      throw e;
    }
    catch (IllegalArgumentException e) {
      throw new StaleCursorException("cursor is malformed");
    }
  }

  /** Overload defaulting the source to {@link SearchSource#DEFAULT}. */
  @VisibleForTesting
  static AllTabCursor decode(String encoded, String sortKey, int pageSize) {
    return decode(encoded, sortKey, pageSize, SearchSource.DEFAULT);
  }

  private static AllTabCursor decodeInternal(String encoded, String expectedPin) {
    String decoded;
    try {
      decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
    catch (IllegalArgumentException e) {
      throw new StaleCursorException("cursor is not valid base64");
    }
    if (!decoded.startsWith(MAGIC)) {
      throw new StaleCursorException("cursor is missing magic prefix");
    }
    String body = decoded.substring(MAGIC.length());
    // split(-1) always yields at least one element, so the token is present; a stale or malformed
    // cursor is caught by the token-pin check below rather than a length guard.
    String[] groups = body.split("\\" + GROUP_SEP, -1);
    String token = groups[0];
    if (!token.equals(expectedPin)) {
      throw new StaleCursorException("cursor is stale; retry from page 1");
    }
    Map<Tab, SectionCursor> map = new EnumMap<>(Tab.class);
    for (int i = 1; i < groups.length; i++) {
      String[] parts = groups[i].split(FIELD_SEP, 2);
      if (parts.length != 2) {
        throw new StaleCursorException("cursor segment malformed");
      }
      Tab tab;
      try {
        tab = Tab.valueOf(parts[0]);
      }
      catch (IllegalArgumentException ex) {
        throw new StaleCursorException("cursor references unknown tab");
      }
      String[] sub = parts[1].split(SUB_SEP, 3);
      String upstream;
      if (sub[0].isEmpty()) {
        upstream = null;
      }
      else {
        byte[] innerBytes;
        try {
          innerBytes = Base64.getUrlDecoder().decode(sub[0]);
        }
        catch (IllegalArgumentException ex) {
          throw new StaleCursorException("cursor inner payload is not valid base64");
        }
        if (innerBytes.length > MAX_INNER_DECODED_LENGTH) {
          throw new StaleCursorException("cursor inner payload exceeds max length");
        }
        upstream = new String(innerBytes, StandardCharsets.UTF_8);
      }
      int skip;
      try {
        skip = sub.length > 1 ? Integer.parseInt(sub[1]) : 0;
      }
      catch (NumberFormatException ex) {
        throw new StaleCursorException("cursor skip is not a number");
      }
      if (skip < 0) {
        throw new StaleCursorException("cursor skip must be non-negative");
      }
      boolean exhausted = sub.length > 2 && "1".equals(sub[2]);
      map.put(tab, new SectionCursor(upstream, skip, exhausted));
    }
    return new AllTabCursor(token, map);
  }

  /**
   * Per-section resume marker.
   *
   * @param upstreamCursor the cursor to pass back to that section's supplier on next page ({@code null}
   *          means "start from the beginning")
   * @param skipWithinPage number of rows to skip after re-fetching the upstream page (needed when the
   *          previous packer pass consumed some — but not all — rows from a freshly fetched
   *          page before hitting its page-size budget)
   * @param exhausted when {@code true}, the section has been fully drained in a prior packer pass and
   *          the next pass MUST NOT re-fetch it (avoids restarting an exhausted section from row 0
   *          on a later page)
   */
  public record SectionCursor(String upstreamCursor, int skipWithinPage, boolean exhausted)
  {
    /** Convenience factory for a non-exhausted section cursor. */
    public static SectionCursor nonExhausted(String upstreamCursor, int skipWithinPage) {
      return new SectionCursor(upstreamCursor, skipWithinPage, false);
    }

    public SectionCursor {
      if (skipWithinPage < 0) {
        throw new IllegalArgumentException("skipWithinPage must be >= 0");
      }
    }
  }
}
