/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class SystemNotice
    extends BasicElement<SystemNotice>
{
  public static final String ROOT_SELECTOR = "#system-notice";

  public SystemNotice() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement root() {
    return $(ROOT_SELECTOR);
  }
}
