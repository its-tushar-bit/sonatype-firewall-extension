/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class NxFormSelect
    extends BasicElement<NxFormSelect>
{
  private static final String ITEM_SELECTOR = "option";

  public NxFormSelect(String... selectors) {
    super(selectors);
  }

  public ElementsCollection listItems() {
    return children(ITEM_SELECTOR);
  }

  public SelenideElement listItem(int num) {
    return child(ITEM_SELECTOR, nthChild(num + 1));
  }

  public void chooseOption(Option option) {
    this.click();
    scrollIntoView(listItem(option.row), false).shouldBe(visible).shouldHave(text(option.value)).click();
  }

  public void chooseOption(String option) {
    new Select($(this.selector)).selectByVisibleText(option);
  }

  public WebElement selectedItem() {
    return new Select($(this.selector)).getFirstSelectedOption();
  }

  /*
   * Helps avoiding <option hidden> at the start of the select element
   */
  public SelenideElement listItemWithHidden(int num) {
    return child(ITEM_SELECTOR, nthChild(num + 2));
  }

  public void chooseOptionWithHidden(Option option) {
    this.click();
    scrollIntoView(listItemWithHidden(option.row), false).shouldBe(visible).shouldHave(text(option.value)).click();
  }

  public static class Option
  {
    private int row;

    private String value;

    public Option(final int row, final String value) {
      this.row = row;
      this.value = value;
    }
  }
}
