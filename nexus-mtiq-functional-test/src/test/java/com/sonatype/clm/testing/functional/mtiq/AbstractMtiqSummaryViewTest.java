/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.elements.NavPills;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.model.Owner;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public abstract class AbstractMtiqSummaryViewTest
    extends AbstractMtiqFunctionalTest
{
  protected Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  @Before
  public void boot() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testNavigationPills() {
    NavPills navPills = OwnerSummaryPage.navigationPills();

    navPills.pills().shouldHaveSize(10);

    navPills.appCategory().click();
    OwnerSummaryPage.categoryTile().shouldBe(visible);

    navPills.policy().click();
    OwnerSummaryPage.policyTile().shouldBe(visible);

    navPills.legacyViolations().click();
    OwnerSummaryPage.legacyViolations().shouldBe(visible);

    navPills.continuousMonitoring().click();
    OwnerSummaryPage.monitoredStage().shouldBe(visible);

    navPills.proprietaryComponents().click();
    OwnerSummaryPage.proprietaryComponentMatchers().shouldBe(visible);

    navPills.labels().click();
    OwnerSummaryPage.labelTile().shouldBe(visible);

    navPills.sourceControl().click();
    OwnerSummaryPage.sourceControlTile().shouldBe(visible);

    navPills.ltg().click();
    OwnerSummaryPage.licenseThreatGroupSummaryTile().shouldBe(visible);

    navPills.retention().shouldNot(exist);
    OwnerSummaryPage.dataRetentionTile().shouldNot(exist);

    navPills.innerSource().should(exist);
    OwnerSummaryPage.innerSourceRepositoryTile().should(exist);
  }
}
