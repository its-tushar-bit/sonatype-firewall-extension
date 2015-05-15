/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class HashComponentIdentifierResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private final String HASH = "test-abcdef";

  private final ComponentIdentifier COMPONENT_IDENTIFIER =
      ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.0", "jdk15", "jar");

  @Test
  public void testSet() throws Exception {
    grantClaimComponentPermission();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));
    // Test creating claimed component
    String url = getRestUrl(HashComponentIdentifierResource.SERVICE_PATH);
    Response response = testAuthzPost(url, toJson(hashComponentIdentifier));
    HashComponentIdentifierDTO hashComponentIdentifierDTO = fromJson(response, HashComponentIdentifierDTO.class);
    hashComponentIdentifier.setId(hashComponentIdentifierDTO.id);
    tempEntity.register(hashComponentIdentifier);
  }

  @Test
  public void testSet_unauthenticated() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    String url = getRestUrl(HashComponentIdentifierResource.SERVICE_PATH);
    Response response = RestAccess.post(url, toJson(hashComponentIdentifier));
    assertResponseStatus(401, response);
  }

  @Test
  public void testUpdate() throws Exception {
    grantClaimComponentPermission();
    // Create new claimed component
    HashComponentIdentifier hashComponentIdentifier = tempEntity.newClaimedComponent(HASH, COMPONENT_IDENTIFIER);
    // Test updating
    hashComponentIdentifier.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid2", "aid2", "2.0",
        "jdk16", "jar"));
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));
    String url = getRestUrl(HashComponentIdentifierResource.SERVICE_PATH);
    testAuthzPut(url, toJson(hashComponentIdentifier));
  }

  @Test
  public void testUpdate_unauthenticated() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(HASH, COMPONENT_IDENTIFIER);
    String url = getRestUrl(HashComponentIdentifierResource.SERVICE_PATH);
    Response response = RestAccess.put(url, toJson(hashComponentIdentifier));
    assertResponseStatus(401, response);
  }

  @Test
  public void testDelete() throws Exception {
    grantClaimComponentPermission();
    // Create new claimed component
    tempEntity.newClaimedComponent(HASH, COMPONENT_IDENTIFIER);
    // Test delete
    String url = getRestUrl(HashComponentIdentifierResource.SERVICE_PATH) + "/" + HASH;
    testAuthzDelete(url);
  }

  @Test
  public void testDelete_unauthenticated() throws Exception {
    String url = getRestUrl(HashComponentIdentifierResource.SERVICE_PATH) + "/" + HASH;
    Response response = RestAccess.delete(url);
    assertResponseStatus(401, response);
  }
}
