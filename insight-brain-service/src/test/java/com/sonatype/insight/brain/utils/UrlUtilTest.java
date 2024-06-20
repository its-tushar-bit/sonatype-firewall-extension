/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class UrlUtilTest
{
  @Test
  public void testGetDomainWithProtocol_urlNotValid() {
    assertThatThrownBy(() -> UrlUtil.getDomainWithProtocol("http://hello world"))
        .isInstanceOf(BadRequestException.class).hasMessage("'baseUrl' is not valid");
  }

  @Test
  public void testGetDomainWithProtocol_missingProtocol() {
    assertThatThrownBy(() -> UrlUtil.getDomainWithProtocol("hello.world.com"))
        .isInstanceOf(BadRequestException.class).hasMessage("'baseUrl' is not valid");
  }

  @Test
  public void testGetDomainWithProtocol() {
    String embedDomain = UrlUtil.getDomainWithProtocol("http://hello.world.com/path/to/something?query=param");

    assertThat(embedDomain).isEqualTo("http://hello.world.com");
  }
}
