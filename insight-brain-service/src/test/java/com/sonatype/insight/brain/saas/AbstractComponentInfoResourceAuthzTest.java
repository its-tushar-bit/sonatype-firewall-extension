/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.Collections;

import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.mock.UriParamRequestMatcher;

import org.eclipse.jetty.util.UrlEncoded;
import org.junit.Test;

public abstract class AbstractComponentInfoResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  protected abstract String getResourcePath();

  @Test
  public void testGetComponentDetailsList() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    componentDetailsList.setList(Collections.<ComponentDetails> emptyList());
    setSaasResponse(new UriParamRequestMatcher("rest/" + getTool() + "/componentDetails/list?componentIdentifier="
        + getComponentIdentifierParam(groupId, artifactId, version), toJson(componentDetailsList), 200));

    grantReadPermission(app.getId());

    String url = getRestUrl(getResourcePath() + "/{applicationPublicId}/list", app.getPublicId())
        + "?componentIdentifier=" + getComponentIdentifierParam(groupId, artifactId, version);
    testAuthzGet(url);
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantReadPermission(app.getId());

    String url = getRestUrl(getResourcePath() + "/{applicationPublicId}", app.getPublicId()) + "?componentIdentifier="
        + getComponentIdentifierParam(groupId, artifactId, version);
    testAuthzGet(url);
  }

  @Test
  public void testGetSelectableLicenses() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantReadPermission(app.getId());

    String url = getRestUrl(getResourcePath() + "/selectableLicenses/{applicationPublicId}", app.getPublicId())
        + "?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version;
    testAuthzGet(url);
  }

  @Test
  public void testGetLicenses() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantReadPermission(app.getId());

    String url = getRestUrl(getResourcePath() + "/licenses/{applicationPublicId}", app.getPublicId()) + "?groupId="
        + groupId + "&artifactId=" + artifactId + "&version=" + version;
    testAuthzGet(url);
  }

  private String getComponentIdentifierParam(String g, String a, String v) {
    return UrlEncoded.encodeString(toJson(ComponentIdentifier.createMavenCoordinates(g, a, v)));
  }

  protected abstract String getTool();
}
