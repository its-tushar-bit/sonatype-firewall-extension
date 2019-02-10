/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Selenide;

public class ThreatLevelSlider extends BasicElement<ThreatLevelSlider>
{
  public ThreatLevelSlider(String selector) {
    super(selector);
  }

  public void setValues(int min, int max) {
    Selenide.executeJavaScript("$('body').find('" + this.selector +
        " div[slider]').trigger({type:'slide',value: [" + min + ", " + max + "]});");
  }
}
