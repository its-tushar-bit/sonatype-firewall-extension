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
    return BaseUrl.resolvePageUrl("/integrations/overview");
  }

  public static String urlCiCd() {
    return BaseUrl.resolvePageUrl("/integrations/ci-cd");
  }

  public static String urlScm() {
    return BaseUrl.resolvePageUrl("/integrations/scm");
  }

  public static String urlIssueTracking() {
    return BaseUrl.resolvePageUrl("/integrations/issue-tracking");
  }

  public static String urlIde() {
    return BaseUrl.resolvePageUrl("/integrations/ide");
  }

  public static String urlAppsWithoutCiIntegrations() {
    return BaseUrl.resolvePageUrl("/apps-without-ci-integrations");
  }
}
