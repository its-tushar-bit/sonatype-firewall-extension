/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxTextInput;

import com.codeborne.selenide.SelenideElement;

public class ArtifactoryRepositoryConfigurationModal
    extends BasicElement<ArtifactoryRepositoryConfigurationModal>
{
  private static final String ROOT = "#artifactory-repository-configuration-modal";

  public ArtifactoryRepositoryConfigurationModal() {
    super(ROOT);
  }

  public NxTextInput baseUrl() {
    return new NxTextInput(child("#artifactory-repository-configuration-modal-base-url"));
  }

  public NxRadio allowAnonymousAccess() {
    return new NxRadio(child("#artifactory-repository-configuration-modal-anonymous-radio"));
  }

  public NxRadio enterUsernameAndPassword() {
    return new NxRadio(child("#artifactory-repository-configuration-modal-credentials-radio"));
  }

  public NxTextInput username() {
    return new NxTextInput(child("#artifactory-repository-configuration-modal-username"));
  }

  public NxTextInput password() {
    return new NxTextInput(child("#artifactory-repository-configuration-modal-password"));
  }

  public SelenideElement authentication() {
    return child("#artifactory-repository-configuration-modal-authentication");
  }

  public SelenideElement test() {
    return child("#artifactory-repository-configuration-modal-test-button");
  }

  public SelenideElement testSuccess() {
    return child("#artifactory-repository-configuration-modal-test-success");
  }

  public SelenideElement cancel() {
    return child("#artifactory-repository-configuration-modal-form", ".nx-form__cancel-btn");
  }

  public SelenideElement save() {
    return child("#artifactory-repository-configuration-modal-form", ".nx-form__submit-btn");
  }
}
