/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.jupiter.api.Test;

public class CheapSimulatedCredentialsMatcherTest
{
  private final CheapSimulatedCredentialsMatcher matcher = new CheapSimulatedCredentialsMatcher();

  @Test
  public void testCreateSimulatedCredentials_usesCheapSha256NotArgon2() {
    Optional<AuthenticationInfo> simulated = matcher.createSimulatedCredentials();

    assertThat(simulated).isPresent();
    String storedHash = (String) simulated.get().getCredentials();

    assertThat(storedHash)
        .startsWith("$shiro1$")
        .contains("SHA-256")
        .doesNotContain("argon2");
  }

  @Test
  public void testCreateSimulatedCredentials_neverMatchesSubmittedPassword() {
    matcher.setPasswordService(new PasswordService());
    AuthenticationInfo simulated = matcher.createSimulatedCredentials().orElseThrow();

    boolean matched = matcher.doCredentialsMatch(new UsernamePasswordToken("anyone", "any-password"), simulated);

    assertThat(matched).isFalse();
  }
}
