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
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.FirewallPolicyViolationsTable;
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

  public static String url(RepositoryComponent component) {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    try {
      String componentIdentifierJSONString =
          URLEncoder.encode(toJson(componentIdentifier), String.valueOf(StandardCharsets.UTF_8));
      String url =
          "/firewall/repository/" + component.getRepositoryId() + "/component/" + componentIdentifierJSONString + "/" +
              component.getHash() + "/" + component.getMatchStateId() + "?proprietary=false";
      return BaseUrl.resolvePageUrl(url);
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
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

  public RiskRemediationTile getRiskRemediationTile() {
    return RiskRemediationTile.getOverviewTileForParent(ROOT);
  }

  public SelenideElement getComponentOverviewTile() {
    return child(".iq-component-information-tile");
  }

  public SelenideElement getComponentOverviewTileReadOnlyItemData(int index) {
    return children(".nx-read-only__item .nx-read-only__data").get(index);
  }

  public SelenideElement getViewCoordinatesButton() {
    return child(".component-coordinates-button");
  }

  public SelenideElement getComponentCoordinatesPopOver() {
    return child("#iq-component-coordinates-popover");
  }

  public SelenideElement getComponentCoordinatesPopOverData(int index) {
    return children(".nx-read-only__data").get(index);
  }

  public SelenideElement getComponentCoordinatesPopOverCloseBtn() {
    return child("#iq-component-coordinates-popover-close-btn");
  }

  public static String urlViolationsTab(RepositoryComponent component) {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    try {
      String componentIdentifierJSONString =
          URLEncoder.encode(toJson(componentIdentifier), String.valueOf(StandardCharsets.UTF_8));
      String url =
          "/firewall/repository/" + component.getRepositoryId() + "/component/" + componentIdentifierJSONString + "/" +
              component.getHash() + "/" + component.getMatchStateId() + "/violations" + "?proprietary=false";
      return BaseUrl.resolvePageUrl(url);
    }
    catch (UnsupportedEncodingException e) {
      return null;
    }
  }

  public SelenideElement getPolicyViolationsComponent() {
    return child("#component-details-violations-tab-content");
  }

  public SelenideElement getComponentPolicyViolationsTitle() {
    return child(".nx-tile-header__title");
  }

  public SelenideElement getComponentPolicyViolationsTable() {
    return child(".firewall-policy-violation-table");
  }

  public ElementsCollection getComponentPolicyViolationsTableCols() {
    return children("tbody > tr");
  }

  public SelenideElement getComponentPolicyViolationsTableHeaders(int index) {
    return children(".nx-cell--header").get(index);
  }

  public SelenideElement iqPolicyViolationCol(int index) {
    return children(".iq-policy-violation-row .nx-cell").get(index);
  }

  public FirewallPolicyViolationsTable getFirewallPolicyViolationsTable() {
    return FirewallPolicyViolationsTable.getPolicyViolationsTableForParent(ROOT);
  }

  public SelenideElement getNextVersionInVersionExplorer() {
    return child("#aiVersionChartViz > svg > g > g:nth-child(24) > g:nth-child(1) > rect:nth-child(2)");
  }
}
