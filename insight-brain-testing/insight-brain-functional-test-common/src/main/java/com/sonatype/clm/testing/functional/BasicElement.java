/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.$x;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public abstract class BasicElement<T extends BasicElement<T>>
{
  protected final String selector;

  protected SelenideElement element;

  protected BasicElement(String... selectors) {
    selector = createSelector(selectors);
  }

  @SuppressWarnings("unchecked")
  protected final T me() {
    return (T) this;
  }

  public SelenideElement parent() {
    return getElement().parent();
  }

  public T should(WebElementCondition... conditions) {
    getElement().should(conditions);
    return me();
  }

  public T shouldNot(WebElementCondition... conditions) {
    getElement().shouldNot(conditions);
    return me();
  }

  public T shouldBe(WebElementCondition... conditions) {
    getElement().shouldBe(conditions);
    return me();
  }

  public T shouldNotBe(WebElementCondition... conditions) {
    getElement().shouldNotBe(conditions);
    return me();
  }

  public T shouldHave(WebElementCondition... conditions) {
    getElement().shouldHave(conditions);
    return me();
  }

  public T shouldNotHave(WebElementCondition... conditions) {
    getElement().shouldNotHave(conditions);
    return me();
  }

  public T hover() {
    getElement().hover();
    return me();
  }

  public T click() {
    getElement().click();
    return me();
  }

  protected String childSelector(String... selectors) {
    return createSelector(selector, createSelector(selectors));
  }

  protected SelenideElement child(String... selectors) {
    return $(childSelector(selectors));
  }

  protected SelenideElement childXpath(String xpath) {
    return $x(xpath);
  }

  protected ElementsCollection children(String... selectors) {
    return $$(childSelector(selectors));
  }

  public SelenideElement getElement() {
    if (element == null) {
      element = $(selector);
    }
    return element;
  }
}
