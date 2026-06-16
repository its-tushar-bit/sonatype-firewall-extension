/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class GuideUsageIdentifiersTest
{
  record PurlRequest(String purl)
  {
  }

  record RefIdRequest(String id)
  {
  }

  @Test
  public void extractsDirectStringArg() {
    assertThat(GuideUsageIdentifiers.extract(new Object[]{"pkg:maven/g/a@1"})).isEqualTo("pkg:maven/g/a@1");
  }

  @Test
  public void extractsPurlAccessorFromRequestRecord() {
    assertThat(GuideUsageIdentifiers.extract(new Object[]{new PurlRequest("pkg:npm/lodash@4")}))
        .isEqualTo("pkg:npm/lodash@4");
  }

  @Test
  public void extractsIdAccessorFromRequestRecord() {
    assertThat(GuideUsageIdentifiers.extract(new Object[]{new RefIdRequest("CVE-2024-1")})).isEqualTo("CVE-2024-1");
  }

  @Test
  public void returnsNullForSearchWithNoIdentifier() {
    assertThat(GuideUsageIdentifiers.extract(new Object[]{Integer.valueOf(25)})).isNull();
    assertThat(GuideUsageIdentifiers.extract(new Object[]{})).isNull();
  }

  @Test
  public void directStringArgWinsOverAccessor() {
    assertThat(GuideUsageIdentifiers.extract(new Object[]{"pkg:maven/g/a@1", new PurlRequest("pkg:npm/y@2")}))
        .isEqualTo("pkg:maven/g/a@1");
  }

  @Test
  public void returnsNullForNullArgsArray() {
    assertThat(GuideUsageIdentifiers.extract(null)).isNull();
  }

  @Test
  public void ignoresQueryAccessorSoSearchesStayCountOnly() {
    record SearchRequest(String query)
    {
    }
    assertThat(GuideUsageIdentifiers.extract(new Object[]{new SearchRequest("log4j")})).isNull();
  }
}
