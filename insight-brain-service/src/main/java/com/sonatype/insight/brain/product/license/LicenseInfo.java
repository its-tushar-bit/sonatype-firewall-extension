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
   * NOTE: Depending on the licensing model, the product could be priced based on application or user count. The counts
   * that aren't applicable to the licensing model are left at null, i.e. not to be shown to end users.
   */
  public Integer licensedUsersToDisplay;

  public Integer applicationLimitToDisplay;

  public Integer applicationCountToDisplay;

  public Integer firewallUsersToDisplay;

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
                     Integer applicationLimitToDisplay,
                     Integer applicationCountToDisplay,
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
    this.applicationLimitToDisplay = applicationLimitToDisplay;
    this.applicationCountToDisplay = applicationCountToDisplay;
    this.contactName = contactName;
    this.contactCompany = contactCompany;
    this.contactEmail = contactEmail;
    this.products = products;
  }
}
