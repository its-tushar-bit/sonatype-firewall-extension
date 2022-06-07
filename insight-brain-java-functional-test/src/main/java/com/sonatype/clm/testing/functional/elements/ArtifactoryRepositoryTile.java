/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ArtifactoryRepositoryTile
    extends OwnerTile
{
  private static final String ARTIFACTORY_REPOSITORY_OWNER_ELEMENT_ID = "#owner-pill-artifactory-repository";

  public ArtifactoryRepositoryTile() {
    super(ARTIFACTORY_REPOSITORY_OWNER_ELEMENT_ID);
  }

  public ElementsCollection rows() {
    return children(".test-list-item-title");
  }

  public SelenideElement listTitle() {
    return child(".iq-list__title");
  }

  public SelenideElement editButton() {
    return child("#artifactory-repositories-edit");
  }
}
