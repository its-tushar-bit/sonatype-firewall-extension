/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock;

import com.sonatype.insight.mock.InsightMockServer.RequestMatcher;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SimpleRequestMatcherTest
{
  @Test
  public void testMatches_Path() {
    RequestMatcher matcher = new SimpleRequestMatcher("/dir/subdir/index.html");
    assertThat(matcher.matches("/dir/subdir/index.html"), is(true));
    assertThat(matcher.matches("/dir/subdir/index.HTML"), is(false));
    assertThat(matcher.matches("/dir/subdir/index"), is(false));
  }

  @Test
  public void testMatches_Query_ParameterOrderIsIrrelevant() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?param1=value1&param2=value2");
    assertThat(matcher.matches("/resource?param1=value1&param2=value2"), is(true));
    assertThat(matcher.matches("/resource?param2=value2&param1=value1"), is(true));
  }

  @Test
  public void testMatches_Query_TargetParametersAreMandatory() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?param=value");
    assertThat(matcher.matches("/resource?param=value"), is(true));
    assertThat(matcher.matches("/resource?param=wrong"), is(false));
    assertThat(matcher.matches("/resource?param="), is(false));
    assertThat(matcher.matches("/resource?param"), is(false));
    assertThat(matcher.matches("/resource"), is(false));
  }

  @Test
  public void testMatches_Query_RequestParametersAreOptional() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource");
    assertThat(matcher.matches("/resource"), is(true));
    assertThat(matcher.matches("/resource?param=value"), is(true));
  }

  @Test
  public void testMatches_Query_DecodesParameters() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?param=%41");
    assertThat(matcher.matches("/resource?param=%41"), is(true));
    assertThat(matcher.matches("/resource?param=A"), is(true));
  }

  @Test
  public void testMatches_Query_RecognizesJsonEncodedMaps() {
    RequestMatcher matcher = new SimpleRequestMatcher(
        "/resource?id=%7B%22format%22%3A%22maven%22%2C%22coords%22%3A%7B%22version%22%3A%221%22%7D%7D");
    assertThat(
        matcher.matches("/resource?id=%7B%22format%22%3A%22maven%22%2C%22coords%22%3A%7B%22version%22%3A%221%22%7D%7D"),
        is(true));
    assertThat(
        matcher.matches("/resource?id=%7B%22coords%22%3A%7B%22version%22%3A%221%22%7D%2C%22format%22%3A%22maven%22%7D"),
        is(true));
    assertThat(
        matcher
            .matches("/resource?id=%7B%22format%22+%3A+%22maven%22%2C+%22coords%22+%3A+%7B+%22version%22+%3A+%221%22+%7D+%7D"),
        is(true));
  }

  @Test
  public void testMatches_Query_RecognizesJsonEncodedLists() {
    RequestMatcher matcher = new SimpleRequestMatcher("/resource?id=%5B1%2C2%5D");
    assertThat(matcher.matches("/resource?id=%5B1%2C2%5D"), is(true));
    assertThat(matcher.matches("/resource?id=%5B1%2C%202%5D"), is(true));
    assertThat(matcher.matches("/resource?id=%5B2%2C1%5D"), is(false));
  }
}
