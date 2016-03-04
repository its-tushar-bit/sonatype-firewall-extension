/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@SuppressWarnings("unchecked")
public abstract class BasicElement<T extends BasicElement<T>>
{
  protected final String selector;

  private SelenideElement element;

  protected BasicElement(String... selectors) {
    if (selectors.length == 1) {
      this.selector = selectors[0];
    }
    else {
      this.selector = SelectorUtils.selector(selectors);
    }
  }

  public T should(Condition... conditions) {
    getElement().should(conditions);
    return (T) this;
  }

  public T shouldNot(Condition... conditions) {
    getElement().shouldNot(conditions);
    return (T) this;
  }

  public T shouldBe(Condition... conditions) {
    getElement().shouldBe(conditions);
    return (T) this;
  }

  public T shouldNotBe(Condition... conditions) {
    getElement().shouldNotBe(conditions);
    return (T) this;
  }

  public T shouldHave(Condition... conditions) {
    getElement().shouldHave(conditions);
    return (T) this;
  }

  public T shouldNotHave(Condition... conditions) {
    getElement().shouldNotHave(conditions);
    return (T) this;
  }

  public void click() {
    getElement().click();
  }

  protected SelenideElement child(String... selectors) {
    return $(SelectorUtils.selector(selector, SelectorUtils.selector(selectors)));
  }

  protected ElementsCollection children(String... selectors) {
    return $$(SelectorUtils.selector(selector, SelectorUtils.selector(selectors)));
  }

  private SelenideElement getElement() {
    if (element == null) {
      element = $(selector);
    }
    return element;
  }
}
