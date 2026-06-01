/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import org.junit.Test;

public class UserTokenHashServiceTest
    extends AbstractComponentTest
{
  @Inject
  private UserTokenHashService userTokenHashService;

  @Test
  public void testHashPassCode_ProducesCorrectFormat() {
    String passCode = "abcdefghijklmnopqrstuvwxyz012345678901234567";
    String hashed = userTokenHashService.hashPassCode(passCode.toCharArray());

    assertThat(hashed).startsWith("$sha256$");
    // SHA-256 produces a 64-character hex string
    assertThat(hashed.substring("$sha256$".length())).hasSize(64);
    assertThat(hashed.substring("$sha256$".length())).matches("[0-9a-f]{64}");
  }

  @Test
  public void testHashPassCode_Deterministic() {
    String passCode = "TestPassCode12345678901234567890123456789012";
    String hash1 = userTokenHashService.hashPassCode(passCode.toCharArray());
    String hash2 = userTokenHashService.hashPassCode(passCode.toCharArray());

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  public void testHashPassCode_DifferentInputsProduceDifferentHashes() {
    String hash1 = userTokenHashService.hashPassCode("PassCodeA1234567890123456789012345678901234".toCharArray());
    String hash2 = userTokenHashService.hashPassCode("PassCodeB1234567890123456789012345678901234".toCharArray());

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  public void testVerifyPassCode_CorrectPassword() {
    String passCode = "TestPassCode12345678901234567890123456789012";
    String hashed = userTokenHashService.hashPassCode(passCode.toCharArray());

    assertThat(userTokenHashService.verifyPassCode(passCode.toCharArray(), hashed)).isTrue();
  }

  @Test
  public void testVerifyPassCode_WrongPassword() {
    String passCode = "TestPassCode12345678901234567890123456789012";
    String hashed = userTokenHashService.hashPassCode(passCode.toCharArray());

    assertThat(userTokenHashService.verifyPassCode("WrongPassCode000000000000000000000000000000".toCharArray(), hashed))
        .isFalse();
  }

  @Test
  public void testSupports_Sha256Hash() {
    String hashed = userTokenHashService.hashPassCode("SomePassCode1234567890123456789012345678901".toCharArray());
    assertThat(userTokenHashService.supports(hashed)).isTrue();
  }

  @Test
  public void testSupports_LegacyHash() {
    assertThat(userTokenHashService.supports("$argon2id$v=19$m=65536,t=1,p=4$abc$def")).isFalse();
    assertThat(userTokenHashService.supports("$shiro1$SHA-256$400000$salt$hash")).isFalse();
  }

  @Test
  public void testSupports_Null() {
    assertThat(userTokenHashService.supports(null)).isFalse();
  }
}
