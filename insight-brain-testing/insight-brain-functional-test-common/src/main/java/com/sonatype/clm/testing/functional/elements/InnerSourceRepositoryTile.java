/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class InnerSourceRepositoryTile
    extends OwnerTile
{
  private static final String INNERSOURCE_REPOSITORY_OWNER_ELEMENT_ID = "#owner-pill-innersource-repository";

  public InnerSourceRepositoryTile() {
    super(INNERSOURCE_REPOSITORY_OWNER_ELEMENT_ID);
  }

  public ElementsCollection rows() {
    return children(".nx-list__item");
  }

  public SelenideElement listTitle() {
    return child(".nx-h3");
  }

  public SelenideElement editButton() {
    return child("#innersource-repositories-edit");
  }
}
