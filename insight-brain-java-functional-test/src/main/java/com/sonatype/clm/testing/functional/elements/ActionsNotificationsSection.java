/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import static com.codeborne.selenide.Selenide.$;

public class ActionsNotificationsSection
    extends PolicyEditorSection
{
  public static final String ROOT_SELECTOR = "#policy-edit-actions";

  public ActionsNotificationsSection() {
    super($(ROOT_SELECTOR));
  }

  public ActionItemList actionItemList() {
    return new ActionItemList(ROOT_SELECTOR + " table");
  }
}
