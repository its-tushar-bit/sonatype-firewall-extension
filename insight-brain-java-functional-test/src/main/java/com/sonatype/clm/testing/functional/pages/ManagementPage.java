/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class ManagementPage
{

  public static String URL = BaseUrl.uriBuilder().fragment("/management/view").build().toString();

  public static final String ROOT_ORG_URL = "assets/index.html#/management/view/organization/ROOT_ORGANIZATION_ID";

}
