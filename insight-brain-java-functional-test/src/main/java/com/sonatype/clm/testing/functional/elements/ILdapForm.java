/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.List;

import com.codeborne.selenide.SelenideElement;

public interface ILdapForm
{
  SelenideElement saveButton();

  SelenideElement cancelButton();

  List<SelenideElement> requiredFields();
}
