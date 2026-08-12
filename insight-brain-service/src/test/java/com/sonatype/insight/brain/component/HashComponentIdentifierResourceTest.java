/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HashComponentIdentifierResourceTest
    extends AbstractResourceTest
{
  private static final String hash = "ab1234ab1234ab";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g1",
      "a1", "v1", "c1", "e1");

  private static final String comment = "my comment";

  private HashComponentIdentifier hashComponentIdentifier;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(HashComponentIdentifierResource.RESOURCE_PATH);
  }

  @Before
  public void setup() {
    hashComponentIdentifier = new HashComponentIdentifier(hash, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testCRUD() throws Exception {
    Date createTime = new Date();
    hashComponentIdentifier.setComment(comment);
    hashComponentIdentifier.setCreateTime(createTime);

    // component must be unknown or we cannot claim it
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));

    // create
    HttpResponse response = restRequest().body(hashComponentIdentifier).post();
    assertResponseStatus(200, response);
    HashComponentIdentifierDTO serverResponse = response.getBody(HashComponentIdentifierDTO.class);
    assertHashComponentIdentifierDTO(hash, COMPONENT_IDENTIFIER, comment, createTime, serverResponse);

    // read
    response = restRequest().path(hashComponentIdentifier.getHash()).get();
    assertHashComponentIdentifierDTO(hash, COMPONENT_IDENTIFIER, comment, createTime, serverResponse);

    // update
    ComponentIdentifier updatedComponentIdentifier = COMPONENT_IDENTIFIER.createAlternativeVersion("updated-version");
    hashComponentIdentifier.setComponentIdentifier(updatedComponentIdentifier);
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));
    response = restRequest().body(hashComponentIdentifier).put();
    assertResponseStatus(200, response);
    serverResponse = response.getBody(HashComponentIdentifierDTO.class);

    assertHashComponentIdentifierDTO(hash, updatedComponentIdentifier, comment, createTime, serverResponse);

    // verify using GET
    response = restRequest().path(hashComponentIdentifier.getHash()).get();
    assertHashComponentIdentifierDTO(hash, updatedComponentIdentifier, comment, createTime, serverResponse);

    // delete
    response = restRequest().path(hashComponentIdentifier.getHash()).delete();
    assertResponseStatus(204, response);

    // verify using GET
    response = restRequest().path(hashComponentIdentifier.getHash()).get();
    assertResponseStatus(404, response);
  }

  private void assertHashComponentIdentifierDTO(
      String hash,
      ComponentIdentifier componentIdentifier,
      String comment,
      Date createTime,
      HashComponentIdentifierDTO hashComponentIdentifier)
  {
    assertThat(hashComponentIdentifier.hash).isEqualTo(hash);
    assertThat(hashComponentIdentifier.componentIdentifier).isEqualTo(componentIdentifier);
    assertThat(hashComponentIdentifier.comment).isEqualTo(comment);
    assertThat(hashComponentIdentifier.createTime).isEqualTo(createTime);

    ComponentDisplayName componentDisplayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    assertThat(hashComponentIdentifier.displayName.parts).hasSameSizeAs(componentDisplayName.parts);
    for (int i = 0; i < componentDisplayName.parts.size(); i++) {
      ComponentDisplayNamePart expected = componentDisplayName.parts.get(i);
      ComponentDisplayNamePart actual = hashComponentIdentifier.displayName.parts.get(i);
      assertThat(actual.field).isEqualTo(expected.field);
      assertThat(actual.value).isEqualTo(expected.value);
    }
    assertThat(hashComponentIdentifier.coordinates).isEqualTo(componentDisplayName.toString());
  }
}
