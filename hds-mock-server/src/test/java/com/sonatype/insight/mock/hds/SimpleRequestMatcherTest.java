/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import com.sonatype.insight.mock.hds.HdsMockServer.RequestMatcher;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleRequestMatcherTest
{
  @Test
  public void testMatches_Path() {
    RequestMatcher matcher = new SimpleRequestMatcher("/dir/subdir/index.html");
    assertThat(matcher.matches("/dir/subdir/index.html")).isTrue();
    assertThat(matcher.matches("/dir/subdir/index.HTML")).isFalse();
    assertThat(matcher.matches("/dir/subdir/index")).isFalse();
  }

  @Test
  public void testMatches_Query_ParameterOrderIsIrrelevant() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?param1=value1&param2=value2");
    assertThat(matcher.matches("/resource?param1=value1&param2=value2")).isTrue();
    assertThat(matcher.matches("/resource?param2=value2&param1=value1")).isTrue();
  }

  @Test
  public void testMatches_Query_TargetParametersAreMandatory() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?param=value");
    assertThat(matcher.matches("/resource?param=value")).isTrue();
    assertThat(matcher.matches("/resource?param=wrong")).isFalse();
    assertThat(matcher.matches("/resource?param=")).isFalse();
    assertThat(matcher.matches("/resource?param")).isFalse();
    assertThat(matcher.matches("/resource")).isFalse();
  }

  @Test
  public void testMatches_Query_RequestParametersAreOptional() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource");
    assertThat(matcher.matches("/resource")).isTrue();
    assertThat(matcher.matches("/resource?param=value")).isTrue();
  }

  @Test
  public void testMatches_Query_DecodesParameters() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?param=%41");
    assertThat(matcher.matches("/resource?param=%41")).isTrue();
    assertThat(matcher.matches("/resource?param=A")).isTrue();
  }

  @Test
  public void testMatches_Query_RecognizesJsonEncodedMaps() {
    RequestMatcher matcher = new SimpleRequestMatcher(
        "/resource?id=%7B%22format%22%3A%22maven%22%2C%22coords%22%3A%7B%22version%22%3A%221%22%7D%7D");
    assertThat(
        matcher.matches("/resource?id=%7B%22format%22%3A%22maven%22%2C%22coords%22%3A%7B%22version%22%3A%221%22%7D%7D"))
            .isTrue();
    assertThat(
        matcher.matches("/resource?id=%7B%22coords%22%3A%7B%22version%22%3A%221%22%7D%2C%22format%22%3A%22maven%22%7D"))
            .isTrue();
    assertThat(matcher.matches(
        "/resource?id=%7B%22format%22+%3A+%22maven%22%2C+%22coords%22+%3A+%7B+%22version%22+%3A+%221%22+%7D+%7D"))
            .isTrue();
  }

  @Test
  public void testMatches_Query_RecognizesJsonEncodedLists() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?id=%5B1%2C2%5D");
    assertThat(matcher.matches("/resource?id=%5B1%2C2%5D")).isTrue();
    assertThat(matcher.matches("/resource?id=%5B1%2C%202%5D")).isTrue();
    assertThat(matcher.matches("/resource?id=%5B2%2C1%5D")).isFalse();
  }
}
