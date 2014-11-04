/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.IOException;
import java.util.Date;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HashComponentIdentifierResourceTest
    extends AbstractResourceTest
{

  private static final String hash = "ab1234ab1234ab";

  private static final String groupId = "g1";

  private static final String artifactId = "a1";

  private static final String version = "v1";

  private static final String extension = "e1";

  private static final String classifier = "c1";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates(groupId,
      artifactId, version, classifier, extension);

  private static final String comment = "my comment";

  private HashComponentIdentifier hashComponentIdentifier;

  @Before
  public void setup() {
    hashComponentIdentifier = new HashComponentIdentifier(hash, COMPONENT_IDENTIFIER);
    tempEntity.register(hashComponentIdentifier);
  }

  @Test
  public void testCRUD() throws Exception {
    Date createTime = new Date();
    hashComponentIdentifier.setComment(comment);
    hashComponentIdentifier.setCreateTime(createTime);

    // component must be unknown or we cannot claim it
    setSaasResponse(groupId);

    // create
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(hashComponentIdentifier));
    assertResponseStatus(200, response);
    hashComponentIdentifier = JsonHelpers.fromJson(response.getResponseBody(), HashComponentIdentifier.class);
    assertHashComponentIdentifier(hash, COMPONENT_IDENTIFIER, comment, createTime, hashComponentIdentifier);

    // read - no GET use case for this resource - use DAO to verify
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash());
    assertHashComponentIdentifier(hash, COMPONENT_IDENTIFIER, comment, createTime, hashComponentIdentifier);

    // update
    String updatedGroupId = groupId + "_updated";
    ComponentIdentifier updatedComponentIdentifier = ComponentIdentifier.createMavenCoordinates(updatedGroupId,
        artifactId, version, classifier, extension);
    hashComponentIdentifier.setComponentIdentifier(updatedComponentIdentifier);
    setSaasResponse(updatedGroupId);
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(hashComponentIdentifier));
    assertResponseStatus(200, response);
    hashComponentIdentifier = JsonHelpers.fromJson(response.getResponseBody(), HashComponentIdentifier.class);
    assertHashComponentIdentifier(hash, updatedComponentIdentifier, comment, createTime, hashComponentIdentifier);

    // read - no GET use case for this resource - use DAO to verify
    hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash());
    assertHashComponentIdentifier(hash, updatedComponentIdentifier, comment, createTime, hashComponentIdentifier);

    // delete
    response = AuthedRestAccess.delete(getServiceURL() + "/" + hashComponentIdentifier.getHash());
    assertResponseStatus(204, response);

    // resource has no use case for GET so look directly in DB to ensure that record is deleted
    assertThat(hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash()), nullValue());
  }

  @Test
  public void testSet_KnownToSaaS() throws Exception {
    setSaasResponseForURI("rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
            + "&extension=" + extension + "&classifier=" + classifier,
        JsonHelpers.asJson(ComponentSummary.create(true)),
        200
    );

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(hashComponentIdentifier));
    assertResponseStatus(400, response);
    assertEquals(
        "The 'maven: {groupId=g1, artifactId=a1, version=v1, classifier=c1, extension=e1}' coordinates are already in use.",
        response.getResponseBody()
    );
  }

  private void assertHashComponentIdentifier(String hash, ComponentIdentifier componentIdentifier, String comment,
      Date createTime, HashComponentIdentifier hashComponentIdentifier)
  {
    assertEquals(hash, hashComponentIdentifier.getHash());
    assertEquals(componentIdentifier, hashComponentIdentifier.getComponentIdentifier());
    assertEquals(comment, hashComponentIdentifier.getComment());
    assertEquals(createTime, hashComponentIdentifier.getCreateTime());
  }

  private String getServiceURL() {
    return getRestBaseUrl() + HashComponentIdentifierResource.SERVICE_PATH;
  }

  private void setSaasResponse(final String groupId) throws IOException {
    setSaasResponseForURI(
        "rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
            + "&extension=" + extension + "&classifier=" + classifier,
        JsonHelpers.asJson(ComponentSummary.create(false)),
        200
    );
  }
}
