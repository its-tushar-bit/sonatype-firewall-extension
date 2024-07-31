/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class IntegrationsPage extends BasicElement<IntegrationsPage>
{
  public static String urlOverview() {
    return BaseUrl.resolvePageUrl("/developer/dashboard/overview");
  }

  public static String urlCiCd() {
    return BaseUrl.resolvePageUrl("/developer/dashboard/ci-cd");
  }

  public static String urlScm() {
    return BaseUrl.resolvePageUrl("/developer/dashboard/scm");
  }

  public static String urlIssueTracking() {
    return BaseUrl.resolvePageUrl("/developer/dashboard/issue-tracking");
  }

  public static String urlIde() {
    return BaseUrl.resolvePageUrl("/developer/dashboard/ide");
  }
}
