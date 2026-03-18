/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.pages.PublicDataSourcesEditorPage;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.List;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class PublicDataSourcesEditorCommonUITest
    extends AbstractPublicDataSourcesEditorTest
{
  @Test
  public void testEditPublicDataSourceConfiguration_errorLicense_noCpeMatchingFeature() {
    productLicenseManager.getFeatures().remove(LicensedFeature.CPE_MATCHING);
    refresh();
    assertThat(PublicDataSourcesEditorPage.errorMessage()
        .text()
        .contains("Your IQ Server license does not enable this feature")).isTrue();
  }

  @Test
  public void testEditPublicDataSourceConfiguration_checkUI() {
    List<String> radioExpectedTexts = List.of(
        "Enabled",
        "Disabled");
    PublicDataSourcesEditorPage.title().shouldHave(text("Public Data Sources"));
    PublicDataSourcesEditorPage.radioInputs().shouldHave(size(2));
    PublicDataSourcesEditorPage.radioInputs()
        .forEach(element -> assertThat(radioExpectedTexts.stream()
            .anyMatch(element.text()::contains)).isTrue());
    PublicDataSourcesEditorPage.allowOverridesCheckbox().shouldBe(visible);
    PublicDataSourcesEditorPage.submitButton().shouldHave(text("Update")).shouldBe(visible);
    OwnerDetailSidebar.publicDatasources()
        .shouldBe(visible)
        .shouldHave(text("Public Data Sources"))
        .shouldHave(cssClass("selected"));
  }
}
