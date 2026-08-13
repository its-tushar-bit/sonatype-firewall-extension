/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.Test;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;

/**
 * Verifies that the private SpringClassRule.getTestContextManager(Class) method used by
 * SpringInjectedTest's fixture-refresh logic still exists. If this test fails after a Spring
 * upgrade, the reflection in SpringInjectedTest.getTestContextManager() needs updating.
 */
public class SpringClassRuleReflectionTest
{
  private static boolean isPackagePrivate(int modifiers) {
    return !Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPrivate(modifiers);
  }

  @Test
  public void testGetTestContextManagerMethodExists() throws Exception {
    Method method = SpringClassRule.class.getDeclaredMethod("getTestContextManager", Class.class);
    assertThat(method.getReturnType()).isEqualTo(TestContextManager.class);
    assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    assertThat(Modifier.isPrivate(method.getModifiers()) || isPackagePrivate(method.getModifiers()))
        .describedAs(
            "Expected private or package-private - if visibility changed to public/protected, the setAccessible workaround needs review")
        .isTrue();
  }
}
