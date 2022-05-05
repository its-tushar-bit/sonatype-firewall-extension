/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.nexus.scm.api.common.JsonUtils.toJson;

public class FirewallCDPPage
    extends BasicElement<FirewallCDPPage>
{
  public static final String ROOT = "firewall-component-details-page";

  public static final String FIREWALL_CDP_TITLE = "#component-details-title";

  public FirewallCDPPage() {
    super(ROOT);
  }

  public static String url(RepositoryComponent component) throws UnsupportedEncodingException {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    String componentIdentifierJSONString =
        URLEncoder.encode(toJson(componentIdentifier), String.valueOf(StandardCharsets.UTF_8));
    String url = "/firewall/repository/" + component.getRepositoryId() + "/component/" +
        componentIdentifierJSONString + "/" + component.getHash() + "/" +
        component.getMatchStateId() + "?proprietary=false";
    return BaseUrl.resolvePageUrl(url);
  }

  public SelenideElement title() {
    return child(FIREWALL_CDP_TITLE);
  }

  public SelenideElement formatTag() {
    return child(".iq-component-format-tag");
  }

  public ElementsCollection tabs() {
    return children(".nx-tab");
  }

  public ElementsCollection getAllLoadingSpinners() {
    return children(".nx-loading-spinner");
  }
}
