/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HttpRequestTest
{
  @Test
  public void testPathNormalizesLeadingSlashes() {
    String url = HttpRequest.to("http://localhost:8070/")
        .anon()
        .path("/rest/user/session", "/logout")
        .getUrl();

    assertThat(url).isEqualTo("http://localhost:8070/rest/user/session/logout");
  }
}
