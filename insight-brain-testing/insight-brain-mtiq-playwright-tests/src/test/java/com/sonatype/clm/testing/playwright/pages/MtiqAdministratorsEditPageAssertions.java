/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MtiqAdministratorsEditPageAssertions
{
  private final MtiqAdministratorsEditPage page;

  public MtiqAdministratorsEditPageAssertions(MtiqAdministratorsEditPage page) {
    this.page = page;
  }

  public void ldapGroupSearchAlertShouldBeHidden() {
    assertThat(page.ldapGroupSearchAlert())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }
}
