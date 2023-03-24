/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class WaivedComponentUpgradeConfigurationPage
    extends BasicElement<WaivedComponentUpgradeConfigurationPage>
{
  private static final String ROOT = "#owner-manager-main";

  public static String url() {
    return BaseUrl.resolvePageUrl("management/edit/organization/{ROOT_ORGANIZATION_ID}/waivedComponentUpgrades",
        ROOT_ORGANIZATION_ID);
  }

  public static String rootOrgMonitoredStageText(String monitoredStage) {
    return "Inherited by all organizations and applications (" + monitoredStage + ")";
  }

  public WaivedComponentUpgradeConfigurationPage() {
    super(ROOT);
  }

  public List<NxRadio> stages() {
    return children("#form-waived-component-upgrades .nx-radio").stream().map(NxRadio::new)
        .collect(Collectors.toList());
  }

  public Map<String, NxRadio> stagesByLabel() {
    return stages().stream().collect(Collectors.toMap(nxRadio -> nxRadio.label().text(), Function.identity()));
  }

  public Button updateButton() {
    return new Button("#form-waived-component-upgrades .nx-form__submit-btn");
  }

  public SelenideElement configurationForm() {
    return $("#form-waived-component-upgrades");
  }
}
