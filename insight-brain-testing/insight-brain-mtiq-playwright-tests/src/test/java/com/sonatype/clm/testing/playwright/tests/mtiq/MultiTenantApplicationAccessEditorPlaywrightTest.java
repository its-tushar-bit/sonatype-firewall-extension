/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.pages.AccessEditorPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.model.Application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

@Tag("mtiq")
public class MultiTenantApplicationAccessEditorPlaywrightTest
    extends AbstractMtiqAccessEditorPlaywrightTest
{
  private String appPublicId;

  @BeforeEach
  public void init() {
    // The "ȧpp" suffix intentionally forces a non-ASCII character into the public id.
    Application application = tempEntity.newApplicationWithParent("test_ȧpp_id", "ApplicationAccessEditorTest app");
    appPublicId = application.getPublicId();
    super.init(application);
  }

  @Override
  protected String newRoleEditorUrl() {
    return OwnerSummaryPage.editApplicationUrl(appPublicId, AccessEditorPage.ADD_ACCESS_URL_FRAGMENT);
  }
}
