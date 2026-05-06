/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.service.Configuration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class FrameEmbeddingDetectorTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private Configuration configuration;

  private FrameEmbeddingDetector detector;

  @Before
  public void setUp() {
    detector = new FrameEmbeddingDetector(configuration);
  }

  @Test
  public void nullAllowList_disabled() {
    when(configuration.getFrameAncestorsAllowList()).thenReturn(null);
    assertThat(detector.isFrameEmbeddingEnabled()).isFalse();
  }

  @Test
  public void emptyAllowList_disabled() {
    when(configuration.getFrameAncestorsAllowList()).thenReturn(emptyList());
    assertThat(detector.isFrameEmbeddingEnabled()).isFalse();
  }

  @Test
  public void singleEntry_enabled() {
    when(configuration.getFrameAncestorsAllowList()).thenReturn(singletonList("https://ci.example.com"));
    assertThat(detector.isFrameEmbeddingEnabled()).isTrue();
  }

  @Test
  public void multipleEntries_enabled() {
    when(configuration.getFrameAncestorsAllowList()).thenReturn(asList("'self'", "https://ci.example.com"));
    assertThat(detector.isFrameEmbeddingEnabled()).isTrue();
  }
}
