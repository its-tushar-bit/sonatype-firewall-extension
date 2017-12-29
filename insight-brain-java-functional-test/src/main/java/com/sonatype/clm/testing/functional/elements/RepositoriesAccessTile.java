/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;

public class RepositoriesAccessTile
    extends BaseAccessTile
{
  public RepositoriesAccessTile() {
    super("#repositories-pill-access");
  }

  public SelenideElement localAccessRole(String roleName) {
    return $$("#repositories-pill-access table td.role").findBy(text(roleName));
  }
}
