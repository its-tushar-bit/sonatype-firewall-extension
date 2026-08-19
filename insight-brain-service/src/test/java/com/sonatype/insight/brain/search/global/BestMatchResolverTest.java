/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Focused tests for the component-coordinate BEST MATCH rule, which normalizes a pasted purl and a
 * canonical row id on their purl coordinates so a natural purl (no implicit default qualifier)
 * promotes the matching row. The vulnerability-id and application rules are covered end to end by
 * the suggest tests; these cases pin the coordinate normalization added for pasted purls.
 */
public class BestMatchResolverTest
{
  private final BestMatchResolver resolver = new BestMatchResolver();

  private static SuggestRow component(final String id) {
    return new SuggestRow(id, SuggestItemType.COMPONENT, SearchSource.LOCAL, id, null, null);
  }

  /** The stored canonical Maven id carries the implicit default qualifier {@code ?type=jar}. */
  private static final SuggestRow AOPALLIANCE =
      component("pkg:maven/aopalliance/aopalliance@1.0?type=jar");

  @Test
  public void naturalPurlWithoutDefaultQualifier_promotesCanonicalRow() {
    SuggestRow best = resolver.resolve("pkg:maven/aopalliance/aopalliance@1.0", List.of(AOPALLIANCE));
    assertThat(best).isEqualTo(AOPALLIANCE);
  }

  @Test
  public void exactCanonicalPurlWithQualifier_stillPromotes() {
    SuggestRow best =
        resolver.resolve("pkg:maven/aopalliance/aopalliance@1.0?type=jar", List.of(AOPALLIANCE));
    assertThat(best).isEqualTo(AOPALLIANCE);
  }

  @Test
  public void mixedCasePurl_stillPromotes() {
    SuggestRow best = resolver.resolve("PKG:MAVEN/aopalliance/aopalliance@1.0", List.of(AOPALLIANCE));
    assertThat(best).isEqualTo(AOPALLIANCE);
  }

  @Test
  public void differentVersion_doesNotPromote() {
    SuggestRow best = resolver.resolve("pkg:maven/aopalliance/aopalliance@2.0", List.of(AOPALLIANCE));
    assertThat(best).isNull();
  }

  @Test
  public void differentArtifact_doesNotPromote() {
    SuggestRow best = resolver.resolve("pkg:maven/aopalliance/other@1.0", List.of(AOPALLIANCE));
    assertThat(best).isNull();
  }

  @Test
  public void nonPurlHashFallbackId_usesExactEquals() {
    // A component whose id is a hash/name fallback (not a purl) still matches only on exact equals;
    // a coordinate-shaped query does not coordinate-match a non-purl id.
    SuggestRow hashRow = component("36c29aaa06a9ccc35173");
    assertThat(resolver.resolve("pkg:maven/aopalliance/aopalliance@1.0", List.of(hashRow))).isNull();
  }

  @Test
  public void plainWord_isNotTreatedAsCoordinate() {
    // A bare word is not coordinate-shaped, so the coordinate rule never fires; it falls through to
    // the application rule, which finds no APPLICATION row here.
    assertThat(resolver.resolve("aopalliance", List.of(AOPALLIANCE))).isNull();
  }
}
