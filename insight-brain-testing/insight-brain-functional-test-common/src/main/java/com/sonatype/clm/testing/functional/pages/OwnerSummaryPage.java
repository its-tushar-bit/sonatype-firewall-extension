/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.ArtifactoryRepositoryTile;
import com.sonatype.clm.testing.functional.elements.CategoryTile;
import com.sonatype.clm.testing.functional.elements.DataRetentionTile;
import com.sonatype.clm.testing.functional.elements.InnerSourceRepositoryTile;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile;
import com.sonatype.clm.testing.functional.elements.NavPills;
import com.sonatype.clm.testing.functional.elements.NxAlert;
import com.sonatype.clm.testing.functional.elements.OrgsAndPoliciesSidebar;
import com.sonatype.clm.testing.functional.elements.OwnerSummaryTile;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PublicDataSourcesTile;
import com.sonatype.clm.testing.functional.elements.SourceControlTile;
import com.sonatype.clm.testing.functional.elements.AutoWaiversTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerSummaryPage
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/management/view/organization/ROOT_ORGANIZATION_ID");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/firewall/management/view/organization/ROOT_ORGANIZATION_ID");
  }

  public static String urlToRootOrg() {
    return url(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  public static String url(Owner owner) {
    String ownerId = owner.getType().equals(OwnerType.REPOSITORY) ? owner.getId() : owner.getPublicId();
    return url(owner.getType(), ownerId);
  }

  public static String url(OwnerType ownerType, String id) {
    return BaseUrl.resolvePageUrl("/management/view/{ownerType}/{ownerId}", ownerType, id);
  }

  public static String sbomManagerUrl(OwnerType ownerType, String id) {
    return BaseUrl.resolvePageUrl("/sbomManager/management/view/{ownerType}/{ownerId}", ownerType, id);
  }

  public static OrgsAndPoliciesSidebar sidebar() {
    return new OrgsAndPoliciesSidebar();
  }

  public static OwnerSummaryTile summaryTile() {
    return new OwnerSummaryTile();
  }

  public static OwnerSummaryTile summaryTile(String rootSelector) {
    return new OwnerSummaryTile(rootSelector);
  }

  public static CategoryTile categoryTile() {
    return new CategoryTile();
  }

  public static PolicyTile policyTile() {
    return new PolicyTile();
  }

  public static SelenideElement legacyViolations() {
    return $("#legacy-violations");
  }

  public static SelenideElement monitoredStage() {
    return $("#continuous-monitoring");
  }

  public static NxAlert loadErrorMessage() {
    return NxAlert.getErrorAlert();
  }

  public static SelenideElement proprietaryComponentMatchers() {
    return $("#proprietary-component-matchers");
  }

  public static LabelTile labelTile() {
    return new LabelTile();
  }

  public static LicenseThreatGroupSummaryTile licenseThreatGroupSummaryTile() {
    return new LicenseThreatGroupSummaryTile();
  }

  public static DataRetentionTile dataRetentionTile() {
    return new DataRetentionTile();
  }

  public static SourceControlTile sourceControlTile() {
    return new SourceControlTile();
  }

  public static InnerSourceRepositoryTile innerSourceRepositoryTile() {
    return new InnerSourceRepositoryTile();
  }

  public static ArtifactoryRepositoryTile artifactoryRepositoryTile() {
    return new ArtifactoryRepositoryTile();
  }

  public static AccessTile accessTile() {
    return new AccessTile("#access-tile-pill-access");
  }

  public static AutoWaiversTile autoWaiversTile() {
    return new AutoWaiversTile();
  }

  public static PublicDataSourcesTile publicDataSourcesTile() {
    return new PublicDataSourcesTile();
  }

  public static NavPills navigationPills() {
    return new NavPills();
  }

  public static SelenideElement repositoryUrlAnchor() {
    return $(".page-repository-url a");
  }

  public static SelenideElement repositoryUrlIcon() {
    return $(".page-repository-url .nx-icon");
  }
}
