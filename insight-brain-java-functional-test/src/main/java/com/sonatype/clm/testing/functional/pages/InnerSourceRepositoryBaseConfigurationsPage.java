/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class InnerSourceRepositoryBaseConfigurationsPage
    extends BasicElement<InnerSourceRepositoryBaseConfigurationsPage>
{
  private static final String ROOT = "#innersource-repository-base-configurations-page-container";

  public static String url(String ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/repositoryBaseConfigurations", ownerType,
        ownerId);
  }

  public InnerSourceRepositoryBaseConfigurationsPage() {
    super(ROOT);
  }

  public SelenideElement status() {
    return child(".nx-read-only__data");
  }

  public NxCheckbox allowOverride() {
    return new NxCheckbox(child("#innersource-repository-base-configurations-allow-override"));
  }

  public NxRadio inherit() {
    return new NxRadio(child("#innersource-repository-base-configurations-inherit-radio"));
  }

  public NxRadio disable() {
    return new NxRadio(child("#innersource-repository-base-configurations-disable-radio"));
  }

  public NxRadio enable() {
    return new NxRadio(child("#innersource-repository-base-configurations-enable-radio"));
  }

  public SelenideElement add() {
    return child("#innersource-repository-base-configurations-add-button");
  }

  public RepositoryConnectionRow row(String repositoryConnectionId) {
    return new RepositoryConnectionRow("#innersource-repository-base-configurations-" + repositoryConnectionId);
  }

  public static class RepositoryConnectionRow
      extends BasicElement<RepositoryConnectionRow>
  {
    public RepositoryConnectionRow(String selector) {
      super(selector);
    }

    public SelenideElement text() {
      return child(".nx-list__text");
    }

    public SelenideElement edit() {
      return child(".innersource-repository-base-configurations-edit-button");
    }
  }

  public SelenideElement cancel() {
    return child("#innersource-repository-base-configurations-cancel-button");
  }

  public SelenideElement save() {
    return child("#innersource-repository-base-configurations-form", ".nx-form__submit-btn");
  }

  public NxBackButton back() {
    return new NxBackButton("#menu-bar__back-button-container");
  }

  public SelenideElement alert() {
    return child(".nx-alert--info");
  }
}
