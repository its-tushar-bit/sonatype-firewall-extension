/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional;

import com.codeborne.selenide.Condition;

import static com.codeborne.selenide.Selenide.$;

@SuppressWarnings("unchecked")
public abstract class BasicElement<T extends BasicElement<T>>
{
  protected final String selector;

  protected BasicElement(String selector) {
    this.selector = selector;
  }

  public T should(Condition... conditions) {
    $(selector).should(conditions);
    return (T) this;
  }

  public T shouldNot(Condition... conditions) {
    $(selector).shouldNot(conditions);
    return (T) this;
  }

  public T shouldBe(Condition... conditions) {
    $(selector).shouldBe(conditions);
    return (T) this;
  }

  public T shouldNotBe(Condition... conditions) {
    $(selector).shouldNotBe(conditions);
    return (T) this;
  }

  public T shouldHave(Condition... conditions) {
    $(selector).shouldHave(conditions);
    return (T) this;
  }

  public T shouldNotHave(Condition... conditions) {
    $(selector).shouldNotHave(conditions);
    return (T) this;
  }
}
