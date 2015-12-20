/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

public abstract class PolicyEditorSection
{
  public SelenideElement root;

  public PolicyEditorSection(SelenideElement root) {
    this.root = root;
  }

  public SelenideElement header() {
    return root.$(".editor-section-header");
  }
}
