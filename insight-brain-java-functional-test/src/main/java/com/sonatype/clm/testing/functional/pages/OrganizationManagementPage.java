/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class OrganizationManagementPage
{
  public static final String URL = BaseUrl.resolvePageUrl("/management/view");

  public static final String ROOT_ORG_URL =
      BaseUrl.resolvePageUrl("/management/view/organization/ROOT_ORGANIZATION_ID");
}
