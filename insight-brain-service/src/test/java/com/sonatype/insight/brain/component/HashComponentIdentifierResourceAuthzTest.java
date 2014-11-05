/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class HashComponentIdentifierResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  /**
   * Only checks existence of authc for now, to be revised once CLM-1140 gets implemented.
   */
  @Test
  public void testSet() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier("test-abcdef",
        ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.0", "jdk15", "jar"));
    String json = toJson(hashComponentIdentifier);
    String url = getRestUrl(HashComponentIdentifierResource.SERVICE_PATH);

    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));

    Response response = RestAccess.post(url, json);
    assertResponseStatus(401, response);

    response = RestAccess.post(url, authorized.getUsername(), authorized.getPassword(), json);
    assertResponseStatus(200, response);
    hashComponentIdentifier = fromJson(response, HashComponentIdentifier.class);
    new HashComponentIdentifierDAO().delete(hashComponentIdentifier);
  }
}
