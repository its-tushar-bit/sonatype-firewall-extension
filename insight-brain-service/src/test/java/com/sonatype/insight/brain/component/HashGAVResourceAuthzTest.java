/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class HashGAVResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  /**
   * Only checks existence of authc for now, to be revised once CLM-1140 gets implemented.
   */
  @Test
  public void testSetHashGAV() throws Exception {
    HashGAV hgav = new HashGAV("test-abcdef", "gid", "aid", "1.0", "jar", "jdk15");
    String json = toJson(hgav);
    String url = getRestUrl(HashGAVResource.SERVICE_PATH);

    setSaasResponseForURI(
        "rest/ide/component?groupId=" + hgav.getGroupId() + "&artifactId=" + hgav.getArtifactId() + "&version="
            + hgav.getVersion() + "&extension=" + hgav.getExtension() + "&classifier=" + hgav.getClassifier(),
        toJson(ComponentSummary.create(false)), 200);

    Response response = RestAccess.post(url, json);
    assertResponseStatus(401, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(200, response);
    hgav = fromJson(response, HashGAV.class);
    new HashGAVDAO().delete(hgav);
  }
}
