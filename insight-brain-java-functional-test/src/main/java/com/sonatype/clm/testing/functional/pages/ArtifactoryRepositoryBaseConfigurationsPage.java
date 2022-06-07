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

public class ArtifactoryRepositoryBaseConfigurationsPage
    extends BasicElement<ArtifactoryRepositoryBaseConfigurationsPage>
{
  private static final String ROOT = "#artifactory-repository-base-configurations-page-container";

  public static String url(String ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/artifactoryRepositoryBaseConfigurations",
        ownerType,
        ownerId);
  }

  public ArtifactoryRepositoryBaseConfigurationsPage() {
    super(ROOT);
  }

  public SelenideElement status() {
    return child(".nx-read-only__data");
  }

  public NxCheckbox allowOverride() {
    return new NxCheckbox(child("#artifactory-repository-base-configurations-allow-override"));
  }

  public NxRadio inherit() {
    return new NxRadio(child("#artifactory-repository-base-configurations-inherit-radio"));
  }

  public NxRadio disable() {
    return new NxRadio(child("#artifactory-repository-base-configurations-disable-radio"));
  }

  public NxRadio enable() {
    return new NxRadio(child("#artifactory-repository-base-configurations-enable-radio"));
  }

  public SelenideElement add() {
    return child("#artifactory-repository-base-configurations-add-button");
  }

  public ArtifactoryConnectionRow row(String artifactoryConnectionId) {
    return new ArtifactoryConnectionRow("#artifactory-repository-base-configurations-" + artifactoryConnectionId);
  }

  public static class ArtifactoryConnectionRow
      extends BasicElement<ArtifactoryConnectionRow>
  {
    public ArtifactoryConnectionRow(String selector) {
      super(selector);
    }

    public SelenideElement text() {
      return child(".nx-list__text");
    }

    public SelenideElement edit() {
      return child(".artifactory-repository-base-configurations-edit-button");
    }
    
    public SelenideElement delete() {
      return child(".artifactory-repository-base-configurations-delete-button");
    }
  }

  public SelenideElement save() {
    return child("#artifactory-repository-base-configurations-form", ".nx-form__submit-btn");
  }

  public NxBackButton back() {
    return new NxBackButton("#menu-bar__back-button-container");
  }

  public SelenideElement alert() {
    return child(".nx-alert--info");
  }

  public DeleteModal deleteModal() {
    return new DeleteModal();
  }

  public static class DeleteModal
      extends BasicElement<DeleteModal>
  {
    public static final String ROOT = "#artifactory-repository-configuration-delete-modal";

    public DeleteModal() {
      super(ROOT);
    }

    public SelenideElement ok() {
      return child(".nx-form__submit-btn");
    }

    public SelenideElement cancel() {
      return child(".nx-form__cancel-btn");
    }
  }
}
