/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assertion helpers for {@link OwnerDetailSidebarComponent}.
 */
public class OwnerDetailSidebarComponentAssertions
{
  private final OwnerDetailSidebarComponent page;

  public OwnerDetailSidebarComponentAssertions(OwnerDetailSidebarComponent page) {
    this.page = page;
  }

  public void shouldBeVisible() {
    assertThat(page.container()).isVisible();
  }

  public void shouldShowAllRootOrgLabels() {
    assertThat(page.container()).isVisible();
    assertThat(page.applicationCategoryGroup())
        .containsText(OwnerDetailSidebarComponent.LABEL_APPLICATION_CATEGORIES);
    assertThat(page.policyGroup()).containsText(OwnerDetailSidebarComponent.LABEL_POLICIES);
    assertThat(page.legacyViolationsLink()).hasText(OwnerDetailSidebarComponent.LABEL_LEGACY_VIOLATIONS);
    assertThat(page.continuousMonitoringLink()).hasText(OwnerDetailSidebarComponent.LABEL_CONTINUOUS_MONITORING);
    assertThat(page.proprietaryComponentsLink()).hasText(OwnerDetailSidebarComponent.LABEL_PROPRIETARY_COMPONENTS);
    assertThat(page.labelGroup()).containsText(OwnerDetailSidebarComponent.LABEL_COMPONENT_LABELS);
    assertThat(page.licenseThreatGroupGroup()).containsText(OwnerDetailSidebarComponent.LABEL_LICENSE_THREAT_GROUPS);
    assertThat(page.sourceControlLink()).hasText(OwnerDetailSidebarComponent.LABEL_SOURCE_CONTROL);
    assertThat(page.accessGroup()).containsText(OwnerDetailSidebarComponent.LABEL_ACCESS);
    assertThat(page.autoWaiversLink()).hasText(OwnerDetailSidebarComponent.LABEL_AUTO_WAIVERS);
  }
}
