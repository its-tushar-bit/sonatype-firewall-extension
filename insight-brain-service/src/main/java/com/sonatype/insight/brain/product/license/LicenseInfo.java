/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

public class LicenseInfo
    extends LicenseSummary
{
  public String fingerprint;

  public long expiryTimestamp;

  /*
   * NOTE: The next two fields aren't necessarily the real limits, they're just the limits that we want
   * to show to users in the License info page. In particular, Lifecycle licenses aren't sold by application limit,
   * so we don't want to display it for those licenses. However, they do still technically have an application
   * limit, which will not be reflected in the value of this property. Similarly, Auditor licenses don't really use
   * the licensedUsers field, but it still has a value in the license simply because it isn't nullable.
   */
  public Integer licensedUsersToDisplay;

  public Integer applicationLimitToDisplay;

  public Integer firewallUsersToDisplay;

  public Integer firewallForArtifactoryServersToDisplay;

  public String contactName;

  public String contactCompany;

  public String contactEmail;

  public String[] products;

  public LicenseInfo() {
  }

  public LicenseInfo(String fingerprint,
                     long expiryTimestamp,
                     Integer licensedUsersToDisplay,
                     Integer firewallUsersToDisplay,
                     Integer firewallForArtifactoryServersToDisplay,
                     Integer applicationLimitToDisplay,
                     String contactName,
                     String contactCompany,
                     String contactEmail,
                     String[] products,
                     String productEdition)
  {
    super(productEdition);

    this.fingerprint = fingerprint;
    this.expiryTimestamp = expiryTimestamp;
    this.licensedUsersToDisplay = licensedUsersToDisplay;
    this.firewallUsersToDisplay = firewallUsersToDisplay;
    this.firewallForArtifactoryServersToDisplay = firewallForArtifactoryServersToDisplay;
    this.applicationLimitToDisplay = applicationLimitToDisplay;
    this.contactName = contactName;
    this.contactCompany = contactCompany;
    this.contactEmail = contactEmail;
    this.products = products;
  }
}
