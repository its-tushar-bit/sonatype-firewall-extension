/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

public class ErrorBox
{
  private final SelenideElement root;

  public ErrorBox(SelenideElement root) {
    this.root = root;
  }

  public SelenideElement message() {
    return root.find("div:first-child");
  }

  public SelenideElement retryButton() {
    return root.find("button");
  }

  public SelenideElement root() {
    return root;
  }
}
