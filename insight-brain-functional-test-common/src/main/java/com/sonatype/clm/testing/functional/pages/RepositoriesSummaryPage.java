/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.NamespaceConfusionProtectionTile;
import com.sonatype.clm.testing.functional.elements.NxAlert;
import com.sonatype.clm.testing.functional.elements.PillButton;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.RepositoriesSummaryTile;
import com.sonatype.clm.testing.functional.elements.RepositoryConfigurationTile;
import com.sonatype.clm.testing.functional.elements.firewall.FirewallInsufficientPermissionBanner;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class RepositoriesSummaryPage
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/firewall/management/view/repository_container/REPOSITORY_CONTAINER_ID");
  }

  public static String repositoryUrl(String repositoryId) {
    return BaseUrl.resolvePageUrl("/firewall/management/view/repository/" + repositoryId);
  }

  public static String repositoryManagerUrl(String repositoryManagerId) {
    return BaseUrl.resolvePageUrl("/firewall/management/view/repository_manager/" + repositoryManagerId);
  }

  public static RepositoriesSummaryTile summaryTile() {
    return new RepositoriesSummaryTile();
  }

  public static RepositoryConfigurationTile configTile() {
    return new RepositoryConfigurationTile();
  }

  public static AccessTile accessTile() {
    return new AccessTile("#access-tile-pill-access");
  }

  public static PolicyTile policyTile() {
    return new PolicyTile();
  }

  public static NamespaceConfusionProtectionTile namespaceConfusionProtectionTile() {
    return new NamespaceConfusionProtectionTile();
  }

  public static PillButton repositoriesPillConfigurationButton() {
    return new PillButton("#repositories-pill-configuration-button");
  }

  public static PillButton policyPillButton() {
    return new PillButton("#owner-pill-policy-button");
  }

  public static PillButton accessPillButton() {
    return new PillButton("#access-tile-pill-access-button");
  }

  public static PillButton namespaceConfusionProtectionPillButton() {
    return new PillButton("#namespace-confusion-protection-pill-configuration-button");
  }

  public static NxAlert getErrorAlert() {
    return new NxAlert(".nx-alert.nx-alert--error");
  }

  public static FirewallInsufficientPermissionBanner getFirewallPermissionBanner() {
    return new FirewallInsufficientPermissionBanner();
  }
}
