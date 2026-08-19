/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock.hds;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HdsMockResponseTest
{
  private boolean matches(HdsMockResponse response, String uri) {
    return matches(response, null, uri);
  }

  private boolean matches(HdsMockResponse response, String method, String uri) {
    return response.matches(method, new ParsedUri(uri));
  }

  @Test
  public void testMatches_Method() {
    HdsMockResponse response = new HdsMockResponse("").atUri("/dir/subdir/index.html").forMethod("GET");
    assertThat(matches(response, "GET", "/dir/subdir/index.html")).isTrue();
    assertThat(matches(response, "PUT", "/dir/subdir/index.html")).isFalse();
  }

  @Test
  public void testMatches_Path() {
    HdsMockResponse response = new HdsMockResponse("").atUri("/dir/subdir/index.html");
    assertThat(matches(response, "/dir/subdir/index.html")).isTrue();
    assertThat(matches(response, "/dir/subdir/index.HTML")).isFalse();
    assertThat(matches(response, "/dir/subdir/index")).isFalse();
  }

  @Test
  public void testMatches_Query_ParameterOrderIsIrrelevant() {
    HdsMockResponse response = new HdsMockResponse("").atUri("/resource?param1=value1&param2=value2");
    assertThat(matches(response, "/resource?param1=value1&param2=value2")).isTrue();
    assertThat(matches(response, "/resource?param2=value2&param1=value1")).isTrue();
  }

  @Test
  public void testMatches_Query_TargetParametersAreMandatory() {
    HdsMockResponse response = new HdsMockResponse("").atUri("/resource?param=value");
    assertThat(matches(response, "/resource?param=value")).isTrue();
    assertThat(matches(response, "/resource?param=wrong")).isFalse();
    assertThat(matches(response, "/resource?param=")).isFalse();
    assertThat(matches(response, "/resource?param")).isFalse();
    assertThat(matches(response, "/resource")).isFalse();
  }

  @Test
  public void testMatches_Query_RequestParametersAreOptional() {
    HdsMockResponse response = new HdsMockResponse("").atUri("/resource");
    assertThat(matches(response, "/resource")).isTrue();
    assertThat(matches(response, "/resource?param=value")).isTrue();
  }

  @Test
  public void testMatches_Query_DecodesParameters() {
    HdsMockResponse response = new HdsMockResponse("").atUri("/resource?param=%41");
    assertThat(matches(response, "/resource?param=%41")).isTrue();
    assertThat(matches(response, "/resource?param=A")).isTrue();
  }

  @Test
  public void testMatches_Query_RecognizesJsonEncodedMaps() {
    HdsMockResponse response = new HdsMockResponse("")
        .atUri("/resource?id=%7B%22format%22%3A%22maven%22%2C%22coords%22%3A%7B%22version%22%3A%221%22%7D%7D");
    assertThat(matches(response,
        "/resource?id=%7B%22format%22%3A%22maven%22%2C%22coords%22%3A%7B%22version%22%3A%221%22%7D%7D")).isTrue();
    assertThat(matches(response,
        "/resource?id=%7B%22coords%22%3A%7B%22version%22%3A%221%22%7D%2C%22format%22%3A%22maven%22%7D")).isTrue();
    assertThat(matches(response,
        "/resource?id=%7B%22format%22+%3A+%22maven%22%2C+%22coords%22+%3A+%7B+%22version%22+%3A+%221%22+%7D+%7D"))
            .isTrue();
  }

  @Test
  public void testMatches_Query_RecognizesJsonEncodedLists() {
    HdsMockResponse response = new HdsMockResponse("").atUri("/resource?id=%5B1%2C2%5D");
    assertThat(matches(response, "/resource?id=%5B1%2C2%5D")).isTrue();
    assertThat(matches(response, "/resource?id=%5B1%2C%202%5D")).isTrue();
    assertThat(matches(response, "/resource?id=%5B2%2C1%5D")).isFalse();
  }
}
