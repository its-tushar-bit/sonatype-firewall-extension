/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.spdx;

import com.sonatype.insight.brain.sbom.utils.SbomFileDetector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Spdx3DetectionTest
{
  @Test
  public void looksLikeSpdx3JsonLd_withSpdx30Context_returnsTrue() {
    String content = "{ \"@context\": \"https://spdx.org/rdf/3.0.1/spdx-context.jsonld\", \"@graph\": [] }";
    assertTrue(SbomFileDetector.looksLikeSpdx3JsonLd(content));
  }

  @Test
  public void looksLikeSpdx3JsonLd_withSpdx30RdfV3_returnsTrue() {
    String content = "{ \"@context\": \"https://spdx.org/rdf/v3/spdx-context.jsonld\", \"@graph\": [] }";
    assertTrue(SbomFileDetector.looksLikeSpdx3JsonLd(content));
  }

  @Test
  public void looksLikeSpdx3JsonLd_withSpdx2Content_returnsFalse() {
    String content = "{ \"spdxVersion\": \"SPDX-2.3\", \"SPDXID\": \"SPDXRef-DOCUMENT\" }";
    assertFalse(SbomFileDetector.looksLikeSpdx3JsonLd(content));
  }

  @Test
  public void looksLikeSpdx3JsonLd_withCycloneDxContent_returnsFalse() {
    String content = "{ \"bomFormat\": \"CycloneDX\", \"specVersion\": \"1.6\" }";
    assertFalse(SbomFileDetector.looksLikeSpdx3JsonLd(content));
  }

  @Test
  public void looksLikeSpdx3JsonLd_withEmptyContent_returnsFalse() {
    assertFalse(SbomFileDetector.looksLikeSpdx3JsonLd(""));
  }
}
