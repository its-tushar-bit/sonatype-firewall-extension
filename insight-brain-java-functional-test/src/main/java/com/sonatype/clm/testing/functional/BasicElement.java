/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

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
      this.selector = createSelector(selectors);
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

  public T hover() {
    getElement().hover();
    return (T) this;
  }

  public void click() {
    getElement().click();
  }

  protected String childSelector(String... selectors) {
    return createSelector(selector, createSelector(selectors));
  }

  protected SelenideElement child(String... selectors) {
    return $(childSelector(selectors));
  }

  protected ElementsCollection children(String... selectors) {
    return $$(childSelector(selectors));
  }

  private SelenideElement getElement() {
    if (element == null) {
      element = $(selector);
    }
    return element;
  }
}
