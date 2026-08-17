/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethod;

/**
 * Architecture (lint-style) checks that keep Playwright tests from reintroducing
 * known flake sources ({@link Thread#sleep}, {@code Page.waitForTimeout}).
 *
 * <p>
 * Named {@code *Check} (not {@code *Test}) so it does NOT match the Failsafe IT include pattern
 * {@code **&#47;*Test.*}. Instead it runs automatically via a dedicated Surefire
 * {@code architecture-checks} execution in this module's {@code pom.xml} — no browser needed.
 */
public class PlaywrightStabilityRulesCheck
{
  private static final String SLEEP_REASON =
      "use Playwright waits / PlaywrightWaitUtils; Thread.sleep is a known flake source";

  private static final String WAIT_FOR_TIMEOUT_REASON =
      "page.waitForTimeout is a fixed sleep; use PlaywrightWaitUtils.waitForCondition (Awaitility-backed) or Playwright locator waits instead";

  @Test
  public void playwrightPackagesMustNotCallThreadSleep() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("com.sonatype.clm.testing.playwright");

    ArchRule banSleepLong = ArchRuleDefinition.noClasses()
        .should(callMethod(Thread.class, "sleep", long.class))
        .because(SLEEP_REASON);

    ArchRule banSleepLongInt = ArchRuleDefinition.noClasses()
        .should(callMethod(Thread.class, "sleep", long.class, int.class))
        .because(SLEEP_REASON);

    banSleepLong.check(classes);
    banSleepLongInt.check(classes);
  }

  @Test
  public void playwrightPackagesMustNotCallPageWaitForTimeout() {
    JavaClasses classes =
        new ClassFileImporter().importPackages("com.sonatype.clm.testing.playwright");

    ArchRule banWaitForTimeout = ArchRuleDefinition.noClasses()
        .should(callMethod(com.microsoft.playwright.Page.class, "waitForTimeout", double.class))
        .because(WAIT_FOR_TIMEOUT_REASON);

    banWaitForTimeout.check(classes);
  }
}
