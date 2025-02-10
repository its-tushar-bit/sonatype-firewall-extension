/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxTextInput;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class EditRoiConfigurationPage
    extends BasicElement<EditRoiConfigurationPage>
{
  public static final String ROOT = "#edit-roi-configuration-page";

  public EditRoiConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/roiConfiguration/edit");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/malware-defense/roiConfiguration/edit");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement developerHourlyRate() {
    return child("#edit-roi-configuration-page__input__developer-hourly-rate");
  }

  public SelenideElement fixRate() {
    return child("#edit-roi-configuration-page__input__fix-rate");
  }

  public SelenideElement supplyChainAttacksBlocked() {
    return child("#edit-roi-configuration-page__input__supply-chain-attacks-blocked");
  }

  public SelenideElement namespaceAttacksBlocked() {
    return child("#edit-roi-configuration-page__input__namespace-attacks-blocked");
  }

  public SelenideElement safeComponentsAutoSelected() {
    return child("#edit-roi-configuration-page__input__safe-components-auto-selected");
  }

  public class SecurityViolationContent
  {
    public NxCheckbox criticalCheckbox() {
      return new NxCheckbox(child("#edit-roi-configuration-page__security-violation-checkbox__critical"));
    }

    public NxTextInput criticalInput() {
      return new NxTextInput(child("#edit-roi-configuration-page__input__critical"));
    }

    public NxCheckbox highCheckbox() {
      return new NxCheckbox(child("#edit-roi-configuration-page__security-violation-checkbox__high"));
    }

    public NxTextInput highInput() {
      return new NxTextInput(child("#edit-roi-configuration-page__input__high"));
    }

    public NxCheckbox mediumCheckbox() {
      return new NxCheckbox(child("#edit-roi-configuration-page__security-violation-checkbox__medium"));
    }

    public NxTextInput mediumInput() {
      return new NxTextInput(child("#edit-roi-configuration-page__input__medium"));
    }

    public NxCheckbox lowCheckbox() {
      return new NxCheckbox(child("#edit-roi-configuration-page__security-violation-checkbox__low"));
    }

    public NxTextInput lowInput() {
      return new NxTextInput(child("#edit-roi-configuration-page__input__low"));
    }
  }

  public NxCheckbox waivedViolationsCheckbox() {
    return new NxCheckbox(child("#edit-roi-configuration-page__checkbox__waived-violations"));
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }
}
