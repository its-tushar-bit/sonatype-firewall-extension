/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTokenTest
{
  @Test
  public void testNormalizeUsername() {
    assertThat(UserToken.normalizeUsername(null)).isNull();
    assertThat(UserToken.normalizeUsername("foo")).isEqualTo("foo");
    assertThat(UserToken.normalizeUsername("fOO")).isEqualTo("foo");
    assertThat(UserToken.normalizeUsername("fo o")).isEqualTo("fo o");
  }

  @Test
  public void testSetUsername() {
    UserToken userToken = new UserToken();

    userToken.setUsername(null);
    assertThat(userToken.getUsername()).isNull();
    assertThat(userToken.getUsernameLowercase()).isNull();
    userToken.setUsername("foo");
    assertThat(userToken.getUsername()).isEqualTo("foo");
    assertThat(userToken.getUsernameLowercase()).isEqualTo("foo");
    userToken.setUsername("fOO");
    assertThat(userToken.getUsername()).isEqualTo("fOO");
    assertThat(userToken.getUsernameLowercase()).isEqualTo("foo");
    userToken.setUsername("fo o");
    assertThat(userToken.getUsername()).isEqualTo("fo o");
    assertThat(userToken.getUsernameLowercase()).isEqualTo("fo o");
  }
}
