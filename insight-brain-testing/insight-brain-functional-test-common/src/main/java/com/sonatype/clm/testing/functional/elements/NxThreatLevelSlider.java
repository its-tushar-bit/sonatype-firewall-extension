/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.interactions.Actions;

public class NxThreatLevelSlider
    extends BasicElement<NxThreatLevelSlider>
{
  public NxThreatLevelSlider(String selector) {
    super(selector);
  }

  private SelenideElement leftSlider() {
    return children(".nx-policy-threat-slider__value-label.MuiSlider-valueLabel > span").get(0);
  }

  private SelenideElement rightSlider() {
    return children(".nx-policy-threat-slider__value-label.MuiSlider-valueLabel > span").get(1);
  }

  public void setValues(int min, int max) {
    int sliderRailWidth = this.child(".MuiSlider-root").getSize().getWidth();
    // Calculate how many pixels-wide are necessary for one step
    int step = sliderRailWidth / 10;

    // X coord (in pixels) of leftSlider in relation to viewport
    int leftSliderCoord = leftSlider().getLocation().getX();
    // X coord (in pixels) of rightSlider in relation to viewport.
    int rightSliderCoord = rightSlider().getLocation().getX();

    Rectangle containerRectangle = getElement().getRect();
    int leftContainerCoord = containerRectangle.getX();

    SelenideElement leftSlider = this.leftSlider();
    SelenideElement rightSlider = this.rightSlider();

    Actions builder = new Actions(WebDriverRunner.getWebDriver());
    // Drag left slider *as far left as safely possible* to ensure that it gets to 0
    builder.moveToElement(leftSlider)
        .click()
        .dragAndDropBy(leftSlider, -(leftSliderCoord - leftContainerCoord), 0)
        .build()
        .perform();

    // Drag right slider *as far left as safely possible* to ensure that it gets to 0
    builder.moveToElement(rightSlider)
        .click()
        .dragAndDropBy(rightSlider, -(rightSliderCoord - leftContainerCoord), 0)
        .build()
        .perform();

    // Move the right slider as many steps as necessary to get to `max`
    builder.moveToElement(rightSlider)
        .click()
        .dragAndDropBy(rightSlider, step * max, 0)
        .build()
        .perform();
    // Move the left slider as many steps as necessary to get to `min`
    builder.moveToElement(leftSlider)
        .click()
        .dragAndDropBy(leftSlider, step * min, 0)
        .build()
        .perform();
  }
}
