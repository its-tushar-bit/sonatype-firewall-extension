/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerEditorDialog
{
  public static SelenideElement root() {
    return $("#owner-editor");
  }

  public static SelenideElement title() {
    return root().find(".clm-modal-header h2");
  }

  public static SelenideElement name() {
    return root().find(".clm-modal-body input[name=name]");
  }

  public static SelenideElement publicId() {
    return root().find(".clm-modal-body input[name=publicId]");
  }

  public static SelenideElement saveButton() {
    return root().find(".clm-modal-footer .btn-primary");
  }

  public static SelenideElement cancelButton() {
    return root().find(".clm-modal-footer button[type=button]");
  }
}
