/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.fieldmap;

import java.util.Locale;
import java.util.Optional;

import com.sonatype.insight.brain.search.index.FieldIdentifier;

import com.github.packageurl.PackageURL;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Compiles a pasted Package URL into a targeted Lucene query over the indexed component-coordinate
 * fields, so a coordinate-shaped query retrieves the exact matching component instead of falling
 * open to a match-all over every component.
 *
 * <p>
 * The generic query grammar treats {@code pkg:maven/g/a@1.0} as a field-scoped predicate on the
 * field {@code pkg}, which is not in the {@link FieldMap} vocabulary and therefore fails open to
 * {@code MatchAllDocsQuery}. Under a small typeahead fetch window that leaves the target row ranked
 * arbitrarily among hundreds of components, so it never reaches the best-match candidate list. This
 * helper detects the purl up front and, for component-bearing types, builds a MUST query on the
 * coordinate parts the {@code DocumentBuilder} indexes ({@code componentCoordinateGroupId},
 * {@code componentCoordinateArtifactId}, {@code componentCoordinateVersion}) plus
 * {@code componentCoordinateName} as a fallback for formats that carry the name outside artifactId.
 * Qualifiers (e.g. Maven's implicit {@code ?type=jar}) are intentionally ignored — a pasted natural
 * purl usually omits them, matching how best-match normalizes on coordinates.
 */
public final class ComponentCoordinateQuery
{
  private ComponentCoordinateQuery() {
  }

  /**
   * Parse {@code rawQuery} as a Package URL and, if it is well-formed, build a coordinate-targeted
   * query. Returns empty when the query is not a parseable {@code pkg:} coordinate, so the caller
   * falls back to the generic parser.
   */
  public static Optional<Query> compile(final String rawQuery) {
    if (rawQuery == null) {
      return Optional.empty();
    }
    final String trimmed = rawQuery.trim();
    if (!looksLikePackageUrl(trimmed)) {
      return Optional.empty();
    }
    final PackageURL purl;
    try {
      // The purl scheme is case-insensitive per spec but the parser rejects a non-lowercase scheme,
      // so normalize the leading "pkg:" (a pasted "PKG:..." still retrieves).
      purl = new PackageURL("pkg:" + trimmed.substring(4));
    }
    catch (Exception e) {
      return Optional.empty();
    }

    final BooleanQuery.Builder must = new BooleanQuery.Builder();
    int clauses = 0;
    // Documents index individual coordinate fields lowercased (LowerCaseKeywordAnalyzer). The purl
    // parser lowercases type/namespace but preserves name/version case, so lowercase every value
    // here to match the indexed token.
    clauses += addTerm(must, FieldIdentifier.COMPONENT_FORMAT.label, purl.getType());
    final int namespaceClauses = addTerm(must, FieldIdentifier.COMPONENT_COORDINATE_GROUP_ID.label,
        purl.getNamespace());
    clauses += namespaceClauses;
    // A purl's name maps to artifactId for maven-style coordinates and to coordinateName for
    // formats that have no group/artifact split; match either so a single builder covers both.
    final int nameClauses = addNameClause(must, purl.getName());
    clauses += nameClauses;
    clauses += addTerm(must, FieldIdentifier.COMPONENT_COORDINATE_VERSION.label, purl.getVersion());

    // A purl with a type but no name/version would compile to a near-match-all over the format; only
    // build the targeted query when it carries a name (the discriminating part), otherwise fall back.
    if (nameClauses == 0) {
      return Optional.empty();
    }
    return clauses == 0 ? Optional.empty() : Optional.of(must.build());
  }

  static boolean looksLikePackageUrl(final String s) {
    return s != null && s.regionMatches(/* ignoreCase */ true, 0, "pkg:", 0, 4);
  }

  private static int addNameClause(final BooleanQuery.Builder must, final String name) {
    if (name == null || name.isBlank()) {
      return 0;
    }
    final String value = name.toLowerCase(Locale.ROOT);
    final BooleanQuery.Builder nameOr = new BooleanQuery.Builder();
    nameOr.add(new TermQuery(new Term(FieldIdentifier.COMPONENT_COORDINATE_ARTIFACT_ID.label, value)), Occur.SHOULD);
    nameOr.add(new TermQuery(new Term(FieldIdentifier.COMPONENT_COORDINATE_NAME.label, value)), Occur.SHOULD);
    nameOr.setMinimumNumberShouldMatch(1);
    must.add(nameOr.build(), Occur.MUST);
    return 1;
  }

  private static int addTerm(final BooleanQuery.Builder must, final String field, final String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    must.add(new TermQuery(new Term(field, value.toLowerCase(Locale.ROOT))), Occur.MUST);
    return 1;
  }
}
