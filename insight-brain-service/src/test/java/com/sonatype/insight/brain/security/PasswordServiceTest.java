/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DefaultTestInsightBrainService;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.crypto.support.hashes.argon2.Argon2HashProvider;
import org.apache.shiro.lang.util.SimpleByteSource;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_HASH_ALGORITHM;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FIPSConfig.HASH_ITERATIONS;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static com.sonatype.insight.brain.security.PasswordService.ITERATIONS_PARAM;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class PasswordServiceTest
    extends AbstractComponentTest
{
  private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

  private static final String DEFAULT_ADMIN_HASHED_PASSWORD =
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=";

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Inject
  private PasswordService passwordService;

  @After
  @Override
  public void afterTest() {
    super.afterTest();

    // Ensure that the Bouncy Castle FIPS provider is removed after the tests as
    // some providers are accessed in the afterTest parent method.
    removeBouncyCastleFipsProvider();
  }

  @Test
  public void testHashPassword() {
    assertThat(passwordService.hashPassword(DEFAULT_ADMIN_PASSWORD)).isNotBlank()
        .isNotEqualTo(DEFAULT_ADMIN_HASHED_PASSWORD);
  }

  @Test
  public void testHashPassword_Null() {
    assertThat(passwordService.hashPassword(null)).isNull();
  }

  @Test
  public void testHashPassword_Empty() {
    assertThat(passwordService.hashPassword("")).isNull();
  }

  @Test
  public void testHashPassword_Blank() {
    assertThat(passwordService.hashPassword(" ")).isNull();
  }

  @Test
  public void testPasswordsMatch() {
    assertThat(passwordService.passwordsMatch(DEFAULT_ADMIN_PASSWORD,
        passwordService.hashPassword(DEFAULT_ADMIN_PASSWORD))).isTrue();
  }

  @Test
  public void testPasswordsMatch_Old() {
    assertThat(passwordService.passwordsMatch(DEFAULT_ADMIN_PASSWORD, DEFAULT_ADMIN_HASHED_PASSWORD)).isTrue();
  }

  @Test
  public void testUseWeakHashIterationForTestsOnly_NotCalledInProductionCode() {
    JavaClasses importedClasses = new ClassFileImporter().importPackages("com.sonatype.insight.brain");

    ArchRule rule = ArchRuleDefinition.noClasses()
        .that().areNotAssignableTo(DefaultTestInsightBrainService.class)
        .should().callMethod(PasswordService.class, "useWeakHashIterationForTestsOnly");

    rule.check(importedClasses);
  }

  @Test
  public void testCreateHashRequest_FIPSMode() {
    insertBouncyCastleFipsProvider();

    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");

    byte[] plaintext = "test".getBytes();
    HashRequest hashRequest = passwordService.createHashRequest(new SimpleByteSource(plaintext));
    assertThat(hashRequest.getAlgorithmName())
        .isPresent()
        .get()
        .isEqualTo(FIPS_HASH_ALGORITHM);

    if (passwordService.isUsingWeakIterationsForTests()) {
      assertThat(hashRequest.getParameters())
          .containsEntry(ITERATIONS_PARAM, 10);
    }
    else {
      assertThat(hashRequest.getParameters())
          .containsEntry(ITERATIONS_PARAM, HASH_ITERATIONS);
    }
  }

  @Test
  public void testCreateHashRequest_NotFIPSMode() {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");

    byte[] plaintext = "test".getBytes();
    HashRequest hashRequest = passwordService.createHashRequest(new SimpleByteSource(plaintext));

    if (passwordService.isUsingWeakIterationsForTests()) {
      assertThat(hashRequest.getAlgorithmName())
          .isPresent()
          .get()
          .isEqualTo(Sha256Hash.ALGORITHM_NAME);
    }
    else {
      assertThat(hashRequest.getAlgorithmName())
          .isPresent()
          .get()
          .isEqualTo(Argon2HashProvider.Parameters.DEFAULT_ALGORITHM_NAME);
    }
  }
}
