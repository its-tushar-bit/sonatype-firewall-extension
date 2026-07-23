/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.github.packageurl.PackageURL;

/**
 * Promotes a single highest-confidence row to {@code bestMatch} when the user-entered query exactly
 * matches a well-formed identifier. Operates over the assembled candidate list and returns the first
 * matching row in priority order.
 *
 * <h3>Exact-match rules (in evaluation order)</h3>
 *
 * <ol>
 * <li><b>Vulnerability id</b> — query matches the canonical shape of a CVE, GHSA, or sonatype-NNNNN
 * identifier (case-insensitive). The promoted row is a {@link SuggestItemType#VULNERABILITY} row
 * whose {@code id} or {@code title} equals the query (case-insensitive).</li>
 * <li><b>Application public id</b> — query equals an {@link SuggestItemType#APPLICATION} row's
 * {@code id}, {@code title}, or {@code subtitle} (case-insensitive: application public ids are
 * stored with a case-insensitive uniqueness constraint in IQ). The IQ row builder emits
 * {@code title=application.name} and {@code subtitle=application.public_id}, so the
 * {@code subtitle} check is what makes an exact public-id match promote to BEST MATCH when the
 * app name differs from its public id.</li>
 * <li><b>Component coordinate</b> — query has a coordinate shape ({@code pkg:...},
 * {@code maven:...}, etc.) and matches a {@link SuggestItemType#COMPONENT} row's {@code id} or
 * {@code title}. Row ids are canonical {@code pkg:} coordinates (the IQ purl converter emits them
 * via {@code com.github.packageurl.PackageURL}) and for Maven carry the implicit default qualifier
 * {@code ?type=jar}, which a user pasting the natural purl usually omits. Both sides are therefore
 * compared on their purl coordinates (type + namespace + name + version, ignoring qualifiers) via
 * {@link PackageURL#isCoordinatesEquals}, so a natural pasted purl promotes the matching canonical
 * row. A non-purl id/title (hash/name fallback) falls back to a lowercased exact equals.</li>
 * </ol>
 *
 * <p>
 * BEST MATCH counts toward the 10-row cap that the service applies. When no rule fires (or no
 * candidate row matches), the resolver returns {@code null}.
 */
@Named
@Singleton
public class BestMatchResolver
{
  static final Pattern CVE_PATTERN = Pattern.compile("CVE-\\d{4}-\\d{4,}", Pattern.CASE_INSENSITIVE);

  static final Pattern GHSA_PATTERN = Pattern.compile("GHSA(-[a-z0-9]{4}){3}", Pattern.CASE_INSENSITIVE);

  // Sonatype-issued advisories carry a mandatory 4-digit year segment followed by one or more
  // additional numeric segments (e.g. sonatype-2024-001). Requiring the year segment shape rules out
  // short false-positive matches like sonatype-1234 while still allowing extended forms.
  static final Pattern SONATYPE_VULN_PATTERN =
      Pattern.compile("sonatype-\\d{4}-\\d+(?:-\\d+)*", Pattern.CASE_INSENSITIVE);

  // Component-coordinate heuristic: pkg:... (Package URL), or one of the legacy IQ coordinate
  // prefixes. The list is intentionally non-exhaustive — adding a missing format only ever shrinks
  // the BEST MATCH set, never broadens it past the row's own id/title check, so a false negative here
  // just degrades to a regular group row.
  static final List<String> KNOWN_COORDINATE_PREFIXES = List.of(
      "pkg", "maven", "npm", "pypi", "golang", "go", "nuget", "gem", "conan", "composer", "cargo",
      "cocoapods", "swift", "rpm", "deb", "docker");

  static final Pattern COMPONENT_COORDINATE_PATTERN = Pattern.compile(
      "^(" + KNOWN_COORDINATE_PREFIXES.stream().collect(Collectors.joining("|")) + "):.+",
      Pattern.CASE_INSENSITIVE);

  /**
   * @param query the user query, non-blank (caller already validated).
   * @param candidates the assembled rows in the order they will appear in the response. May include
   *          rows from any source.
   * @return the first candidate row that matches an exact-match rule, or {@code null}.
   */
  public SuggestRow resolve(final String query, final List<SuggestRow> candidates) {
    if (query == null || query.isBlank() || candidates == null || candidates.isEmpty()) {
      return null;
    }
    // Rule selection is mutually exclusive on the query shape: a vulnerability-id-shaped query looks
    // at VULNERABILITY rows only; a coordinate-shaped query at COMPONENT rows only; the application
    // rule fires only when neither vuln nor coordinate shapes match. This prevents an application
    // named after a CVE id (or a vuln id matching a coordinate prefix) from being promoted under the
    // wrong rule.
    if (looksLikeVulnerabilityId(query)) {
      return findMatch(candidates, SuggestItemType.VULNERABILITY, query, /* caseInsensitive */ true);
    }
    if (looksLikeComponentCoordinate(query)) {
      return findComponentCoordinateMatch(candidates, query);
    }
    return findApplicationMatch(candidates, query);
  }

  /**
   * APPLICATION rule: an exact match on {@code id}, {@code title}, or {@code subtitle}. Public ids are
   * surfaced as {@code subtitle} by the IQ row builder, so this check promotes an exact public-id
   * match like {@code webgoat} when the app's display name is {@code Webgoat}. The comparison is
   * case-insensitive: application public ids collide on case in IQ.
   */
  private static SuggestRow findApplicationMatch(final List<SuggestRow> candidates, final String query) {
    for (SuggestRow row : candidates) {
      if (row.type() != SuggestItemType.APPLICATION) {
        continue;
      }
      if (matches(row.id(), query, true)
          || matches(row.title(), query, true)
          || matches(row.subtitle(), query, true))
      {
        return row;
      }
    }
    return null;
  }

  /**
   * COMPONENT coordinate rule. Row ids are canonical purls rendered by the IQ purl converter, which
   * for Maven carries the implicit default qualifier {@code ?type=jar}; a user typically pastes the
   * natural purl without it. A byte-for-byte equals would miss that row, so compare on the purl
   * coordinates (type + namespace + name + version, ignoring qualifiers) via {@link PackageURL}. When
   * either side is not a parseable purl (e.g. a hash/name fallback id), fall back to the previous
   * lowercased exact equals so nothing that used to match stops matching.
   */
  private static SuggestRow findComponentCoordinateMatch(final List<SuggestRow> candidates, final String query) {
    final PackageURL queryPurl = tryParsePurl(query);
    final String loweredQuery = query.toLowerCase(Locale.ROOT);
    for (SuggestRow row : candidates) {
      if (row.type() != SuggestItemType.COMPONENT) {
        continue;
      }
      if (coordinateMatches(row.id(), queryPurl, loweredQuery)
          || coordinateMatches(row.title(), queryPurl, loweredQuery))
      {
        return row;
      }
    }
    return null;
  }

  private static boolean coordinateMatches(
      final String candidate,
      final PackageURL queryPurl,
      final String loweredQuery)
  {
    if (candidate == null) {
      return false;
    }
    if (queryPurl != null) {
      final PackageURL candidatePurl = tryParsePurl(candidate);
      if (candidatePurl != null) {
        return queryPurl.isCoordinatesEquals(candidatePurl);
      }
    }
    // Non-purl candidate (hash/name fallback id or title): keep the prior lowercased exact equals.
    return candidate.toLowerCase(Locale.ROOT).equals(loweredQuery);
  }

  private static PackageURL tryParsePurl(final String value) {
    if (value == null || !value.regionMatches(/* ignoreCase */ true, 0, "pkg:", 0, 4)) {
      return null;
    }
    try {
      // Canonical row ids are fully lowercased by the IQ purl converter, and the prior exact-match
      // rule lowercased the query, so lowercase the whole value before parsing. This also satisfies
      // the purl-spec requirement that the scheme be lowercase (the parser rejects "PKG:").
      return new PackageURL(value.toLowerCase(Locale.ROOT));
    }
    catch (Exception e) {
      return null;
    }
  }

  static boolean looksLikeVulnerabilityId(final String s) {
    return CVE_PATTERN.matcher(s).matches()
        || GHSA_PATTERN.matcher(s).matches()
        || SONATYPE_VULN_PATTERN.matcher(s).matches();
  }

  static boolean looksLikeComponentCoordinate(final String s) {
    return COMPONENT_COORDINATE_PATTERN.matcher(s).matches();
  }

  private static SuggestRow findMatch(
      final List<SuggestRow> candidates,
      final SuggestItemType wantedType,
      final String query,
      final boolean caseInsensitive)
  {
    for (SuggestRow row : candidates) {
      if (row.type() != wantedType) {
        continue;
      }
      if (matches(row.id(), query, caseInsensitive) || matches(row.title(), query, caseInsensitive)) {
        return row;
      }
    }
    return null;
  }

  private static boolean matches(final String a, final String b, final boolean caseInsensitive) {
    if (a == null || b == null) {
      return false;
    }
    return caseInsensitive ? a.equalsIgnoreCase(b) : a.equals(b);
  }
}
