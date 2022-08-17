/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class EclipseCIPPage
    extends BasicElement<EclipseCIPPage>
{
  public static String url() {
    return "assets/version-graph/ide/eclipse/index.html#/";
  }

  public SelenideElement defaultText() {
    return $("#select-component");
  }

  public SelenideElement errorText() {
    return $("#error-message");
  }

  public SelenideElement versionsCIPBase() {
    return $("#aiVersionChart");
  }

  public SelenideElement nugetComponentID() {
    return VersionsCIP.artifactTable().find("#artifactInfo-ID");
  }

  public SelenideElement websiteHref() {
    return $("#artifactWebsite td:last-child");
  }
}
