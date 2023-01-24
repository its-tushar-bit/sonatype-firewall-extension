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

import com.codeborne.selenide.SelenideElement;

public class InnerSourceRepositoryConfigurationModal
    extends BasicElement<InnerSourceRepositoryConfigurationModal>
{
  private static final String ROOT = "#innersource-repository-configuration-modal";

  public InnerSourceRepositoryConfigurationModal() {
    super(ROOT);
  }

  public NxFormSelect format() {
    return new NxFormSelect(this.selector, ".nx-form-select", ".nx-form-select__select");
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
    return new NxTextInput(child("#innersource-repository-configuration-modal-base-url"));
  }

  public NxRadio allowAnonymousAccess() {
    return new NxRadio(child("#innersource-repository-configuration-modal-anonymous-radio"));
  }

  public NxRadio enterUsernameAndPassword() {
    return new NxRadio(child("#innersource-repository-configuration-modal-credentials-radio"));
  }

  public NxTextInput username() {
    return new NxTextInput(child("#innersource-repository-configuration-modal-username"));
  }

  public NxTextInput password() {
    return new NxTextInput(child("#innersource-repository-configuration-modal-password"));
  }

  public SelenideElement authentication() {
    return child("#innersource-repository-configuration-modal-authentication");
  }

  public SelenideElement test() {
    return child("#innersource-repository-configuration-modal-test-button");
  }

  public SelenideElement testSuccess() {
    return child("#innersource-repository-configuration-modal-test-success");
  }

  public SelenideElement cancel() {
    return child("#innersource-repository-configuration-modal-form", ".nx-form__cancel-btn");
  }

  public SelenideElement save() {
    return child("#innersource-repository-configuration-modal-form", ".nx-form__submit-btn");
  }
}
