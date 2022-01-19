/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxTextInput;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class InnerSourceRepositoryConfigurationPage
    extends BasicElement<InnerSourceRepositoryConfigurationPage>
{
  private static final String ROOT = "#innersource-repository-configuration-page-container";

  public static String url(String ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl("/management/edit/{ownerType}/{ownerId}/repositoryConfiguration", ownerType, ownerId);
  }

  public static String url(String ownerType, String ownerId, String repositoryConnectionId) {
    return BaseUrl.resolvePageUrl(
        "/management/edit/{ownerType}/{ownerId}/repositoryConfiguration/{repositoryConnectionId}", ownerType, ownerId,
        repositoryConnectionId);
  }

  public InnerSourceRepositoryConfigurationPage() {
    super(ROOT);
  }

  public NxFormSelect format() {
    return new NxFormSelect(this.selector, ".nx-form-select");
  }
  
  public Option generic() {
    return new Option(0, "generic (all formats)");
  }
  
  public Option maven() {
    return new Option(1, "maven");
  }
  
  public Option npm() {
    return new Option(2, "npm");
  }

  public NxTextInput baseUrl() {
    return new NxTextInput(child("#innersource-repository-configuration-base-url"));
  }

  public NxRadio allowAnonymousAccess() {
    return new NxRadio(child("#innersource-repository-configuration-anonymous-radio"));
  }

  public NxRadio enterUsernameAndPassword() {
    return new NxRadio(child("#innersource-repository-configuration-credentials-radio"));
  }

  public Authentication authentication() {
    return new Authentication();
  }

  public static class Authentication
      extends BasicElement<DeleteModal>
  {
    public static final String ROOT = "#innersource-repository-configuration-authentication";

    public Authentication() {
      super(ROOT);
    }

    public NxTextInput username() {
      return new NxTextInput(child("#innersource-repository-configuration-username"));
    }

    public NxTextInput password() {
      return new NxTextInput(child("#innersource-repository-configuration-password"));
    }
  }

  public SelenideElement test() {
    return child("#innersource-repository-configuration-test-button");
  }

  public SelenideElement testSuccess() {
    return child("#innersource-repository-configuration-test-success");
  }

  public SelenideElement cancel() {
    return child("#innersource-repository-configuration-cancel-button");
  }

  public SelenideElement save() {
    return child("#innersource-repository-configuration-form", ".nx-form__submit-btn");
  }

  public SelenideElement delete() {
    return child("#innersource-repository-configuration-delete-button");
  }

  public DeleteModal deleteModal() {
    return new DeleteModal();
  }

  public static class DeleteModal
      extends BasicElement<DeleteModal>
  {
    public static final String ROOT = "#innersource-repository-configuration-delete-modal";

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
