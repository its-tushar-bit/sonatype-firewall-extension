/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.Collections;

import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class CIComponentInfoResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetComponentDetailsList() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(Collections.<ComponentDetails> emptyList());
    setSaasResponseForURI("rest/ide/component/details/list?groupId=" + groupId + "&artifactId=" + artifactId
        + "&version=" + version, toJson(componentDetailsList), 200);

    grantReadPermission(app.getId());

    String url = getRestUrl(CIComponentInfoResource.SERVICE_PATH + "/list/{applicationPublicId}", app.getPublicId())
        + "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version;
    testAuthzGet(url);
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantReadPermission(app.getId());

    String url = getRestUrl(CIComponentInfoResource.SERVICE_PATH + "/{applicationPublicId}", app.getPublicId())
        + "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version;
    testAuthzGet(url);
  }

  @Test
  public void testGetSelectableLicenses() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantReadPermission(app.getId());

    String url = getRestUrl(CIComponentInfoResource.SERVICE_PATH + "/selectableLicenses/{applicationPublicId}",
        app.getPublicId())
        + "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version;
    testAuthzGet(url);
  }

  @Test
  public void testGetLicenses() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantReadPermission(app.getId());

    String url = getRestUrl(CIComponentInfoResource.SERVICE_PATH + "/licenses/{applicationPublicId}", app.getPublicId())
        + "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version;
    testAuthzGet(url);
  }
}
