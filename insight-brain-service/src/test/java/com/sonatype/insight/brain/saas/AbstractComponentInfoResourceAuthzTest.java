/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

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
    setSaasResponseForURI("rest/" + getTool() + "/componentDetails/list?componentIdentifier="
        + getComponentIdentifierParam(groupId, artifactId, version), toJson(componentDetailsList), 200);

    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    String url = getRestUrl(getResourcePath() + "/{applicationPublicId}/list", app.getPublicId())
        + "?componentIdentifier=" + getComponentIdentifierParam(groupId, artifactId, version);
    testAuthzGet(url);
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    String url = getRestUrl(getResourcePath() + "/{applicationPublicId}", app.getPublicId()) + "?componentIdentifier="
        + getComponentIdentifierParam(groupId, artifactId, version);
    testAuthzGet(url);
  }

  @Test
  public void testGetLicenses() throws Exception {
    String groupId = "gid";
    String artifactId = "aid";
    String version = "1.0";

    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    String url = getRestUrl(getResourcePath() + "/licenses/{applicationPublicId}", app.getPublicId())
        + "?componentIdentifier=" + getComponentIdentifierParam(groupId, artifactId, version);
    testAuthzGet(url);
  }

  private String getComponentIdentifierParam(String g, String a, String v) {
    return UrlEncoded.encodeString(toJson(ComponentIdentifier.createMavenCoordinates(g, a, v)));
  }

  protected abstract String getTool();
}
