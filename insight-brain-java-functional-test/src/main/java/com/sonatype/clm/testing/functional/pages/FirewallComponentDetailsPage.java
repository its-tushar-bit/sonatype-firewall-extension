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
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.componentdetails.FirewallPolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.ManageLabelsContentTab;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.nexus.scm.api.common.JsonUtils.toJson;

public class FirewallComponentDetailsPage
    extends BasicElement<FirewallComponentDetailsPage>
{
  public static final String ROOT = "#firewall-component-details-page";

  public static final String FIREWALL_COMPONENT_DETAILS_PAGE_TITLE = "#component-details-title";

  private static final String NO_TAB_ID = "";

  private static final String OVERVIEW_TAB_ID = "overview";

  private static final String VIOLATIONS_TAB_ID = "violations";

  private static final String SECURITY_TAB_ID = "security";

  private static final String LEGAL_TAB_ID = "legal";

  private static final String LABELS_TAB_ID = "labels";

  public FirewallComponentDetailsPage() {
    super(ROOT);
  }

  private static String getBaseUrl(RepositoryComponent component, String tabId, boolean fromFirewallRoute) {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    try {
      String componentIdentifierJSONString =
          URLEncoder.encode(toJson(componentIdentifier), String.valueOf(StandardCharsets.UTF_8));
      String url = (fromFirewallRoute ? "/firewall" : "") + "/repository/" + component.getRepositoryId()
          + "/component/" + componentIdentifierJSONString
          + "/" + component.getHash() + "/" + component.getMatchStateId() + (tabId.isEmpty() ? NO_TAB_ID : "/" + tabId)
          + "?pathname=" + URLEncoder.encode(component.getPathname(), String.valueOf(StandardCharsets.UTF_8));
      return BaseUrl.resolvePageUrl(url);
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String defaultUrlFromRepositoryResultsView(RepositoryComponent component) {
    return getBaseUrl(component, NO_TAB_ID, false);
  }

  public static String defaultUrl(RepositoryComponent component) {
    return getBaseUrl(component, NO_TAB_ID, true);
  }

  public static String urlViolationsTabFromRepositoryResultsView(RepositoryComponent component) {
    return getBaseUrl(component, VIOLATIONS_TAB_ID, false);
  }

  public static String urlViolationsTab(RepositoryComponent component) {
    return getBaseUrl(component, VIOLATIONS_TAB_ID, true);
  }

  public static String overviewTab(RepositoryComponent component) {
    return getBaseUrl(component, OVERVIEW_TAB_ID, true);
  }

  public static String urlSecurityTab(RepositoryComponent component) {
    return getBaseUrl(component, SECURITY_TAB_ID, true);
  }

  public static String urlSecurityTabFromRepositoryResultsView(RepositoryComponent component) {
    return getBaseUrl(component, SECURITY_TAB_ID, false);
  }

  public static String urlLegalTab(RepositoryComponent component) {
    return getBaseUrl(component, LEGAL_TAB_ID, true);
  }

  public static String urlLegalTabFromRepositoryResultsView(RepositoryComponent component) {
    return getBaseUrl(component, LEGAL_TAB_ID, false);
  }

  public static String urlLabelsTab(RepositoryComponent component) {
    return getBaseUrl(component, LABELS_TAB_ID, true);
  }

  public static String urlLabelsTabFromRepositoryResultsView(RepositoryComponent component) {
    return getBaseUrl(component, LABELS_TAB_ID, false);
  }

  public static FirewallPolicyViolationsTable getFirewallPolicyViolationsTable() {
    return FirewallPolicyViolationsTable.getPolicyViolationsTableForParent(ROOT);
  }

  public static PolicyViolationsTable getPolicyViolationsTable() {
    return PolicyViolationsTable.getPolicyViolationsTableForParent(".component-waivers");
  }

  public SelenideElement title() {
    return child(FIREWALL_COMPONENT_DETAILS_PAGE_TITLE);
  }

  public SelenideElement reevaluateButton() {
    return child("#firewall-component-details-page__reevaluate-button");
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

  public SelenideElement getPolicyViolationsComponent() {
    return child("#component-details-violations-tab-content");
  }

  public SelenideElement getComponentPolicyViolationsTitle() {
    return child("#component-details-violations-tab-content .nx-tile-header__title");
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

  public ElementsCollection getClickableVersionsInVersionExplorer() {
    return children("#aiVersionChartViz").get(0).findAll("rect[pointer-events='all']");
  }

  public SelenideElement getSecurityTabContainer() {
    return child("#component-details-security-tab-content");
  }

  public SelenideElement getVulnerabilitiesTable() {
    return child(".iq-policy-vulnerability-table");
  }

  public ElementsCollection getVulnerabilitiesTableRows() {
    return children(".iq-policy-vulnerability-table .iq-vulnerabilities-row");
  }

  public ElementsCollection getVulnerabilitiesTableCellsFromRow(int row) {
    return getVulnerabilitiesTableRows().get(row).findAll(".nx-cell");
  }

  public String getProxyStateIconTypeFromViolationsTableRow(int row) {
    return getVulnerabilitiesTableRows().get(row).find(".iq-policy-violation-row__proxy-state-flag").getText();
  }

  public SelenideElement firewallWaiversButton() {
    return child("#firewall-details-view-waivers");
  }

  public SelenideElement getViewAllComponentWaiversButton() {
    return child("#firewall-details-view-waivers");
  }

  public SelenideElement getDeleteWaiverButton() {
    return child(".list-waivers-row__delete-btn");
  }

  public SelenideElement getDeleteWaiverModal() {
    return child("#delete-waiver-modal");
  }

  public SelenideElement getDeleteWaiverModalButton() {
    return child("#delete-waiver-modal-continue-button");
  }

  public ManageLabelsContentTab labelsContent() {
    return new ManageLabelsContentTab("#component-details-labels-tab-content");
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }
}
