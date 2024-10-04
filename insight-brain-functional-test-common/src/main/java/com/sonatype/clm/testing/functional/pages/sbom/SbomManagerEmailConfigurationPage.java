/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages.sbom;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.pages.EmailConfigurationPage;

public class SbomManagerEmailConfigurationPage
    extends EmailConfigurationPage
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/sbomManager/mailConfig");
  }
}
