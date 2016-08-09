/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;

public class LdapNameEditor
    extends BasicElement<LdapNameEditor>
{
  public LdapNameEditor(String... selectors) {
    super(selectors);
  }

  public NameEditor nameEditor() {
    return new NameEditor(childSelector(".inline-editor"));
  }

  public SelenideElement cancelButton() {
    return child("button.btn:first-child");
  }

  public SelenideElement saveButton() {
    return child(".btn-primary");
  }

  public static class NameEditor
      extends BasicElement<NameEditor>
  {
    public NameEditor(String... selectors) {
      super(selectors);
    }

    public String getValue() {
      return child(isEdit() ? "input" : "span").text();
    }

    public void setValue(String value) {
      if (!isEdit()) {
        // open editor
        child("span").click();
      }

      child("input").shouldBe(visible).setValue(value);
    }

    public boolean isEdit() {
      SelenideElement form = child("form");
      return form != null && form.isDisplayed();
    }
  }
}
