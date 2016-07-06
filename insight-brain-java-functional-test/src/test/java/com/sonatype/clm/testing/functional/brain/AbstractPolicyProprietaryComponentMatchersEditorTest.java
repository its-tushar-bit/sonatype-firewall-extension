/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.clm.testing.functional.pages.ProprietaryComponentMatchersEditorPage;
import com.sonatype.insight.brain.model.Owner;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Selenide.open;

public abstract class AbstractPolicyProprietaryComponentMatchersEditorTest
    extends AbstractFunctionalTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    List<String> packages = new ArrayList<>();
    packages.add("com.sonatype");
    List<String> regexes = new ArrayList<>();
    regexes.add(".*/test\\.zip");
    tempEntity.newProprietaryConfig(currentOwner.getParentOwnerId(), packages, regexes);
    packages.add("com.local");
    tempEntity.newProprietaryConfig(currentOwner.getId(), packages, regexes);

    open(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
  }

  @Test
  public void testEditProprietaryComponentMatchers() {
    SummaryTile.proprietaryComponentMatchers().shouldHave(ProprietaryComponentMatchersEditorPage.summaryText(3, 2));
    // TODO CLM-6644 remove previous line and uncomment the following line
    //SummaryTile.proprietaryComponentMatchers().shouldHave(text(inheritOptionText)).click();
  }

  private void assertEditProprietaryComponentMatchersStateIsCorrect() {
    // TODO CLM-6644
  }
}
