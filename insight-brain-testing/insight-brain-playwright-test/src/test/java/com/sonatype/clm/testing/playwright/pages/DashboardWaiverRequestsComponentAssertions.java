/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.sonatype.clm.testing.playwright.pages.DashboardWaiverRequestsComponent.ExpectedRow;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link DashboardWaiverRequestsComponent}.
 */
public class DashboardWaiverRequestsComponentAssertions
{
  private final DashboardWaiverRequestsComponent page;

  public DashboardWaiverRequestsComponentAssertions(DashboardWaiverRequestsComponent page) {
    this.page = page;
  }

  public void shouldShowNoDataMessage(String expectedMessage) {
    assertThat(page.noDataMessage()).isVisible();
    assertThat(page.noDataMessage()).hasText(expectedMessage);
  }

  public void shouldHaveRequestCount(int expected) {
    assertThat(page.waiverRequests()).hasCount(expected);
  }

  public void shouldShowRequestRow(int index, ExpectedRow expected) {
    if (expected.threatNumber() != null) {
      assertThat(page.threatNumber(index)).hasText(expected.threatNumber());
    }
    if (expected.createTime() != null) {
      assertThat(page.createTime(index)).containsText(expected.createTime());
    }
    if (expected.requester() != null) {
      assertThat(page.requester(index)).containsText(expected.requester());
    }
    if (expected.policy() != null) {
      assertThat(page.policy(index)).hasText(expected.policy());
    }
    if (expected.scope() != null) {
      assertThat(page.scope(index)).hasText(expected.scope());
    }
    if (expected.component() != null) {
      assertThat(page.component(index)).hasText(expected.component());
    }
    if (expected.status() != null) {
      assertThat(page.status(index)).hasText(expected.status());
    }
  }
}
