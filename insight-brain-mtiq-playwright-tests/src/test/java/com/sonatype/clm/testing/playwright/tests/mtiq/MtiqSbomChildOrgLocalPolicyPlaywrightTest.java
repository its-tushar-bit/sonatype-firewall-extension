/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPageAssertions;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqSbomChildOrgLocalPolicyPlaywrightTest
    extends AbstractMtiqSbomChildOrgPolicyPlaywrightTest
{
  @Test
  public void testSbomPolicyEditor_childOrgLocalPolicy_rendersEditable() {
    String policyName = "Child Local Policy " + tempEntity.uuid();
    Policy policy = tempEntity.newPolicy(childOrg.getId(), policyName, THREAT_LEVEL);

    playwrightRefreshOrOpen(OwnerSummaryPage.url(childOrg.getId()));
    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    assertThat(ownerSummary.policiesTile()).isVisible();
    ownerSummary.policiesTileRowByName(policyName).click();

    PolicyEditorPage editor = new PolicyEditorPage();
    new PolicyEditorPageAssertions(editor).shouldBeInEditModeWithExpectedName(policy.getName());
  }
}
