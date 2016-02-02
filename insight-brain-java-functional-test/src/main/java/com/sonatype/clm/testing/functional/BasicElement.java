/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

@SuppressWarnings("unchecked")
public abstract class BasicElement<T extends BasicElement<T>>
{
  protected final String selector;

  private SelenideElement element;

  protected BasicElement(String selector) {
    this.selector = selector;
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

  private SelenideElement getElement() {
    if (element == null) {
      element = $(selector);
    }
    return element;
  }
}
