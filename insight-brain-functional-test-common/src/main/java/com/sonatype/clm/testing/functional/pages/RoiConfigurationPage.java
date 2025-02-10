/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class RoiConfigurationPage
    extends BasicElement<RoiConfigurationPage>
{
  public static final String ROOT = "#roi-configuration-page";

  public RoiConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/roiConfiguration");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/firewall/roiConfiguration");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public Button editButton() {
    return new Button(childSelector("#roi-configuration-page__button__edit"));
  }

  public SelenideElement developerHourlyRate() {
    return child("#roi-configuration-page__numeric-value__developer-hourly-rate");
  }

  public SelenideElement fixRate() {
    return child("#roi-configuration-page__numeric-value__fix-rate");
  }

  public SelenideElement supplyChainAttacksBlocked() {
    return child("#roi-configuration-page__numeric-value__supply-chain-attacks-blocked");
  }

  public SelenideElement namespaceAttacksBlocked() {
    return child("#roi-configuration-page__numeric-value__namespace-attacks-blocked");
  }

  public SelenideElement safeComponentsAutoSelected() {
    return child("#roi-configuration-page__numeric-value__safe-components-auto-selected");
  }

  public class SecurityViolationContent
  {
    public SelenideElement critical() {
      return child("#roi-configuration-page__security-violation-types-content__critical");
    }

    public SelenideElement high() {
      return child("#roi-configuration-page__security-violation-types-content__high");
    }

    public SelenideElement medium() {
      return child("#roi-configuration-page__security-violation-types-content__medium");
    }

    public SelenideElement low() {
      return child("#roi-configuration-page__security-violation-types-content__low");
    }
  }

  public NxCheckbox waivedViolationsCheckbox() {
    return new NxCheckbox(child("#roi-configuration-page__checkbox__waived-violations"));
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }
}
