/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.ProductLicensePage;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.CollectionCondition.texts;

public class ProductLicenseTest
    extends AbstractFunctionalTest
{
  private static final String FINGERPRINT_PATTERN = "[0-9a-fA-F]+";

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ProductLicensePage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    refreshOrOpen(ProductLicensePage.url());
  }

  @Test
  public void testLicenseInformation() {
    ProductLicensePage.expiryDate().shouldBe(visible).should(matchText("[a-zA-Z]+ [0-9]+, 2[0-9]{3}"));
    ProductLicensePage.daysToExpiration().shouldBe(visible).shouldHave(matchText("[0-1]"));
    ProductLicensePage.contactName().shouldBe(visible).shouldHave(text("Billy"));
    ProductLicensePage.contactCompany().shouldBe(visible).shouldHave(text("Acme"));
    ProductLicensePage.contactEmail().shouldBe(visible).shouldHave(text("billy@example.com"));
    ProductLicensePage.licensedDevelopers().shouldBe(visible).shouldHave(text("50"));
    ProductLicensePage.firewallLicensedDevelopers().shouldBe(visible).shouldHave(text("45"));
    ProductLicensePage.applicationLimit().shouldBe(hidden);
    ProductLicensePage.products().shouldHave(texts("Nexus Lifecycle", "Nexus Firewall"));
    ProductLicensePage.fingerprint().shouldBe(visible).should(matchText(FINGERPRINT_PATTERN));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testUpdateLicense() {
    ProductLicensePage.installLicenseBtn().shouldBe(visible).shouldHave(text("Update License"));

    installAndCheckLicense();
  }

  @Test
  public void testUninstallAndInstallLicense() {
    ProductLicensePage.uninstallLicenseBtn().shouldBe(visible).shouldHave(text("Uninstall License"));
    ProductLicensePage.uninstallLicenseBtn().click();

    ProductLicensePage.ProductLicenseUninstallModal uninstallModal = new ProductLicensePage.ProductLicenseUninstallModal();

    uninstallModal.shouldBe(visible);
    uninstallModal.uninstallBtn().shouldBe(visible);
    uninstallModal.uninstallBtn().click();

    FormMask.seeAndWaitForDismissal();

    ProductLicensePage.fingerprint().shouldBe(hidden);
    ProductLicensePage.uninstallLicenseBtn().shouldBe(hidden);
    ProductLicensePage.installLicenseBtn().shouldBe(visible).shouldHave(text("Install License"));

    eyesWatcher.eyesCheck();

    installAndCheckLicense();
  }

  private void installAndCheckLicense() {
    // NOTE: the contents of the license file don't matter for this test because the MockProductLicenseManager ignores
    // it anyway
    ProductLicensePage.installLicenseFileUpload()
        .uploadFromClasspath("com/sonatype/clm/testing/functional/brain/ProductLicenseTest/mockLicense");

    ProductLicensePage.ProductLicenseEulaModal eulaModal = new ProductLicensePage.ProductLicenseEulaModal();
    eulaModal.shouldBe(visible);
    eulaModal.header().shouldHave(text("End User License Agreement"));
    eulaModal.eula().shouldHave(text("READ THIS AGREEMENT CAREFULLY"));
    eulaModal.acceptBtn().shouldBe(visible).click();

    FormMask.seeAndWaitForDismissal();

    ProductLicensePage.fingerprint().shouldBe(visible).should(matchText(FINGERPRINT_PATTERN));
    ProductLicensePage.installLicenseBtn().shouldBe(visible).shouldHave(text("Update License"));
  }
}
