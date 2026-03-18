/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Properties;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.HelpMenu;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.pages.ReportListPage;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;

public class HelpTest
    extends AbstractFunctionalTest
{
  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  private String getProductMajorMinorVersion() throws Exception {
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("/com/sonatype/insight/brain/version/version.properties"));
    String[] version = props.getProperty("version").split("\\.");
    return version[0] + "." + version[1];
  }

  @Test
  public void testHelpLinks() throws Exception {
    HelpMenu help = MainHeader.helpMenu();

    help.dropdownToggle().shouldBe(visible).click();
    help.documentationLink()
        .shouldBe(visible)
        .shouldHave(attribute("target", "_blank"),
            attribute("href", "http://links.sonatype.com/products/clm/doc/" + getProductMajorMinorVersion()));
    help.supportLink()
        .shouldBe(visible)
        .shouldHave(attribute("target", "_blank"),
            attribute("href", "http://links.sonatype.com/products/clm/support"));
    help.gettingStartedLink().shouldBe(visible).shouldNotHave(attribute("target", "_blank"));
    eyesWatcher.eyesCheck();

    help.dropdownToggle().shouldBe(visible).click();
    help.documentationLink().shouldBe(hidden);
    help.supportLink().shouldBe(hidden);
  }
}
