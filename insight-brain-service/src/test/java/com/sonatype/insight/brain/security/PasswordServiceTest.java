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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PasswordServiceTest
    extends AbstractComponentTest
{
  private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

  private static final String DEFAULT_ADMIN_HASHED_PASSWORD =
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=";

  @Inject
  private PasswordService passwordService;

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
}
