/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationIdUtilsTest
{
  @Test
  public void testAlphanumericOnly() {
    String input = "App123";
    String expected = "App123";
    assertThat(ApplicationIdUtils.normalizeApplicationPublicId(input)).isEqualTo(expected);
  }

  @Test
  public void testAllowedSpecialCharacters() {
    String input = "App.Name-123_";
    String expected = "App.Name-123_";
    assertThat(ApplicationIdUtils.normalizeApplicationPublicId(input)).isEqualTo(expected);
  }

  @Test
  public void testDisallowedCharactersReplaced() {
    String input = "App@#Name!";
    String expected = "App--Name-";
    assertThat(ApplicationIdUtils.normalizeApplicationPublicId(input)).isEqualTo(expected);
  }

  @Test
  public void testMoreDisallowedCharactersReplaced() {
    String input = " !@#$%^&*()_+=[]{},.<>/?";
    String expected = "-----------_-------.----";
    assertThat(ApplicationIdUtils.normalizeApplicationPublicId(input)).isEqualTo(expected);
  }

  @Test
  public void testEmptyString() {
    String input = "";
    String expected = "";
    assertThat(ApplicationIdUtils.normalizeApplicationPublicId(input)).isEqualTo(expected);
  }

  @Test
  public void testSpacesReplacedWithDash() {
    String input = "My Application Id";
    String expected = "My-Application-Id";
    assertThat(ApplicationIdUtils.normalizeApplicationPublicId(input)).isEqualTo(expected);
  }

  @Test
  public void testUnicodeCharacters() {
    String input = "AppÇüöşğİ";
    String expected = "AppÇüöşğİ";
    assertThat(ApplicationIdUtils.normalizeApplicationPublicId(input)).isEqualTo(expected);
  }
}
