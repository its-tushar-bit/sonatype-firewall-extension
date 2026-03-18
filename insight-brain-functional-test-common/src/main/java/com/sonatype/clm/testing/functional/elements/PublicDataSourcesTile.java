/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class PublicDataSourcesTile
    extends BasicElement<PublicDataSourcesTile>
{
  public PublicDataSourcesTile() {
    super("#owner-pill-public-data-sources");
  }

  public SelenideElement title() {
    return child(".nx-tile-header__title");
  }

  public SelenideElement content() {
    return child(".nx-tile-content");
  }
}
