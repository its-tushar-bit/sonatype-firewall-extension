/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

public class ActionsNotificationsSection
{
  public static final String ROOT_SELECTOR = "#policy-edit-actions";

  public ActionItemList actionItemList() {
    return new ActionItemList(ROOT_SELECTOR + " table tbody");
  }
}
