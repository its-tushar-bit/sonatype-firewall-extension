/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class ThreatLevelSlider extends BasicElement<ThreatLevelSlider>
{
  public ThreatLevelSlider(String selector) {
    super(selector);
  }

  public void setValues(int min, int max) {
    if (currentMin() == currentMax()) {
      if (currentMin() < min) {
        setThumbValue(maxThumb(), minThumb(), max);
        setThumbValue(minThumb(), maxThumb(), min);
      }
      else {
        setThumbValue(minThumb(), maxThumb(), min);
        setThumbValue(maxThumb(), minThumb(), max);
      }

    }
    else {
      setThumbValue(maxThumb(), minThumb(), max);
      setThumbValue(minThumb(), maxThumb(), min);
    }
  }

  private void setThumbValue(SelenideElement thumbToMove, SelenideElement secondThumb, int value) {
    if (currentValueFor(secondThumb) == value) {
      thumbToMove.dragAndDropTo(secondThumb);
    }
    else {
      thumbToMove.dragAndDropTo(".MuiSlider-mark[data-index='" + value + "']");
    }
  }

  private SelenideElement minThumb() {
    return child(".MuiSlider-thumb[data-index='0']");
  }

  private SelenideElement maxThumb() {
    return child(".MuiSlider-thumb[data-index='1']");
  }

  private int currentMin() {
    return currentValueFor(minThumb());
  }

  private int currentMax() {
    return currentValueFor(maxThumb());
  }

  private int currentValueFor(SelenideElement element) {
    return Integer.parseInt(element.attr("aria-valuenow"));
  }
}
