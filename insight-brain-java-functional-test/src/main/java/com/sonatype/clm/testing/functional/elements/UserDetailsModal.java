/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class UserDetailsModal
    extends BasicElement<UserDetailsModal>
{
  public UserDetailsModal() {
    super("#user-details-modal");
  }

  public SelenideElement username() {
    return $("#user-details-modal-username");
  }

  public SelenideElement displayName() {
    return $("#user-details-modal-display-name");
  }

  public SelenideElement groups() {
    return $("#user-details-modal-groups");
  }

  public SelenideElement closeButton() {
    return $("#user-details-modal-close");
  }
}
