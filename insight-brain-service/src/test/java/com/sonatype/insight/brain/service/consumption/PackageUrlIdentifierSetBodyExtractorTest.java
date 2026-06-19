/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageUrlIdentifierSetBodyExtractorTest
{
  @Test
  public void extract_nullBody_returnsNull() {
    assertThat(PackageUrlIdentifierSetBodyExtractor.extract(null)).isNull();
  }

  @Test
  public void extract_nonCollectionBody_returnsNull() {
    assertThat(PackageUrlIdentifierSetBodyExtractor.extract("not-a-collection")).isNull();
    assertThat(PackageUrlIdentifierSetBodyExtractor.extract(42)).isNull();
  }

  @Test
  public void extract_emptyCollection_returnsNull() {
    assertThat(PackageUrlIdentifierSetBodyExtractor.extract(Collections.emptyList())).isNull();
  }

  @Test
  public void extract_collectionWithNonPurlElements_returnsNull() {
    assertThat(PackageUrlIdentifierSetBodyExtractor.extract(List.of("not-a-purl", 99))).isNull();
  }

  @Test
  public void extract_singlePurl_returns16HexChars() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0");
    String result = PackageUrlIdentifierSetBodyExtractor.extract(List.of(purl));
    assertThat(result).isNotNull().hasSize(16).matches("[0-9a-f]{16}");
  }

  @Test
  public void extract_samePurlsDifferentOrder_returnsSameHash() {
    PackageUrlIdentifier p1 = new PackageUrlIdentifier("pkg:maven/com.example/aaa@1.0");
    PackageUrlIdentifier p2 = new PackageUrlIdentifier("pkg:maven/com.example/bbb@2.0");
    String hash1 = PackageUrlIdentifierSetBodyExtractor.extract(List.of(p1, p2));
    String hash2 = PackageUrlIdentifierSetBodyExtractor.extract(List.of(p2, p1));
    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void extract_duplicatePurls_deduplicatesBeforeHashing() {
    PackageUrlIdentifier p = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0");
    String hashSingle = PackageUrlIdentifierSetBodyExtractor.extract(List.of(p));
    String hashDuplicate = PackageUrlIdentifierSetBodyExtractor.extract(List.of(p, p));
    assertThat(hashSingle).isEqualTo(hashDuplicate);
  }

  @Test
  public void extract_differentPurlSets_returnDifferentHashes() {
    PackageUrlIdentifier p1 = new PackageUrlIdentifier("pkg:maven/com.example/aaa@1.0");
    PackageUrlIdentifier p2 = new PackageUrlIdentifier("pkg:maven/com.example/bbb@2.0");
    String hash1 = PackageUrlIdentifierSetBodyExtractor.extract(List.of(p1));
    String hash2 = PackageUrlIdentifierSetBodyExtractor.extract(List.of(p2));
    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  public void extract_mixedCollectionWithSomePurls_hashesOnlyPurlElements() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0");
    // Collection with a mix of PackageUrlIdentifier and non-PURL — only the PURL is used.
    String hashMixed = PackageUrlIdentifierSetBodyExtractor.extract(List.of(purl, "other-string"));
    String hashPurlOnly = PackageUrlIdentifierSetBodyExtractor.extract(List.of(purl));
    assertThat(hashMixed).isEqualTo(hashPurlOnly);
  }

  @Test
  public void extract_skipsNullPackageUrlValues() {
    // A PackageUrlIdentifier whose getPackageUrl() returns null is theoretical in production
    // (ComponentRemediationService always passes non-null PURLs), but the contract must be
    // enforced defensively: without the null filter, .sorted() would throw NPE which is caught
    // by recordConsumption's catch-warn and silently drops the event.
    PackageUrlIdentifier nullPurl = mock(PackageUrlIdentifier.class);
    when(nullPurl.getPackageUrl()).thenReturn(null);

    PackageUrlIdentifier realPurl = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0");
    String expected = PackageUrlIdentifierSetBodyExtractor.extract(List.of(realPurl));

    // Should not throw, and the null-PURL element is silently skipped.
    String result = PackageUrlIdentifierSetBodyExtractor.extract(List.of(nullPurl, realPurl));
    assertThat(result).isNotNull().isEqualTo(expected);

    // A collection containing only the null-PURL element yields null (no valid PURLs).
    assertThat(PackageUrlIdentifierSetBodyExtractor.extract(List.of(nullPurl))).isNull();
  }
}
