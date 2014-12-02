/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.ning.http.client.Response;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HashComponentIdentifierResourceTest
    extends AbstractResourceTest
{

  private static final String hash = "ab1234ab1234ab";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g1",
      "a1", "v1", "c1", "e1");

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
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));

    // create
    Response response = AuthedRestAccess.post(getServiceURL(), toJson(hashComponentIdentifier));
    assertResponseStatus(200, response);
    HashComponentIdentifierDTO serverResponse = fromJson(response, HashComponentIdentifierDTO.class);
    assertHashComponentIdentifierDTO(hash, COMPONENT_IDENTIFIER, comment, createTime, serverResponse);

    // read - no GET use case for this resource - use DAO to verify
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash());
    assertHashComponentIdentifier(hash, COMPONENT_IDENTIFIER, comment, createTime, hashComponentIdentifier);

    // update
    ComponentIdentifier updatedComponentIdentifier = COMPONENT_IDENTIFIER.createAlternativeVersion("updated-version");
    hashComponentIdentifier.setComponentIdentifier(updatedComponentIdentifier);
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));
    response = AuthedRestAccess.put(getServiceURL(), toJson(hashComponentIdentifier));
    assertResponseStatus(200, response);
    serverResponse = fromJson(response, HashComponentIdentifierDTO.class);
    assertHashComponentIdentifierDTO(hash, updatedComponentIdentifier, comment, createTime, serverResponse);

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
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(true));

    Response response = AuthedRestAccess.post(getServiceURL(), toJson(hashComponentIdentifier));
    assertResponseStatus(400, response);
    assertEquals("The 'g1 : a1 : e1 : c1 : v1' coordinates are already in use.", response.getResponseBody());
  }

  @Test
  public void testSet_NullComponentIdentifier() throws Exception {
    hashComponentIdentifier.setComponentIdentifier(null);
    Response response = AuthedRestAccess.post(getServiceURL(), toJson(hashComponentIdentifier));
    assertResponseStatus(400, response);
    assertEquals("The component identifier cannot be null.", response.getResponseBody());
  }

  @Test
  public void testSet_InvalidComponentIdentifier() throws Exception {
    hashComponentIdentifier.setComponentIdentifier(JsonUtils.parse("{\"format\":\"maven\",\"coordinates\":null}",
        ComponentIdentifier.class));
    Response response = AuthedRestAccess.post(getServiceURL(), toJson(hashComponentIdentifier));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("A component identifier must have at least one coordinate."));
  }

  private void assertHashComponentIdentifier(String hash, ComponentIdentifier componentIdentifier, String comment,
      Date createTime, HashComponentIdentifier hashComponentIdentifier)
  {
    assertEquals(hash, hashComponentIdentifier.getHash());
    assertEquals(componentIdentifier, hashComponentIdentifier.getComponentIdentifier());
    assertEquals(comment, hashComponentIdentifier.getComment());
    assertEquals(createTime, hashComponentIdentifier.getCreateTime());
  }

  private void assertHashComponentIdentifierDTO(String hash, ComponentIdentifier componentIdentifier, String comment,
      Date createTime, HashComponentIdentifierDTO hashComponentIdentifier)
  {
    assertEquals(hash, hashComponentIdentifier.hash);
    assertEquals(componentIdentifier, hashComponentIdentifier.componentIdentifier);
    assertEquals(comment, hashComponentIdentifier.comment);
    assertEquals(createTime, hashComponentIdentifier.createTime);

    ComponentDisplayName componentDisplayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    assertThat(hashComponentIdentifier.displayName.parts, hasSize(componentDisplayName.parts.size()));
    for (int i = 0; i < componentDisplayName.parts.size(); i++) {
      ComponentDisplayNamePart expected = componentDisplayName.parts.get(i);
      ComponentDisplayNamePart actual = hashComponentIdentifier.displayName.parts.get(i);
      assertThat(actual.field, is(expected.field));
      assertThat(actual.value, is(expected.value));
    }
    assertThat(hashComponentIdentifier.coordinates, is(componentDisplayName.toString()));
  }


  private String getServiceURL() {
    return getRestBaseUrl() + HashComponentIdentifierResource.SERVICE_PATH;
  }
}
