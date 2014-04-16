/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.Date;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HashGAVResourceTest
    extends AbstractResourceTest
{

  private static final String hash = "ab1234ab1234ab";

  private static final String groupId = "HashGAVResourceTest_G";

  private static final String artifactId = "HashGAVResourceTest_A";

  private static final String version = "HashGAVResourceTest_V";

  private static final String extension = "HashGAVResourceTest_E";

  private static final String classifier = "HashGAVResourceTest_C";

  private static final String comment = "HashGAVResourceTest_Comment";

  HashGAV hashGAV;

  @Before
  public void setup() {
    hashGAV = new HashGAV(hash, groupId, artifactId, version, extension, classifier);
  }

  @After
  public void teardown() {
    new HashGAVDAO().delete(hashGAV);
  }

  @Test
  public void testSetHashGAV_UnknownToSaaS() throws Exception {
    Date createTime = new Date();

    setSaasResponseForURI("rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
            + "&extension=" + extension + "&classifier=" + classifier,
        JsonHelpers.asJson(ComponentSummary.create(false)),
        200
    );

    hashGAV.setComment(comment);
    hashGAV.setCreateTime(createTime);
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(hashGAV));
    assertResponseStatus(200, response);
    hashGAV = JsonHelpers.fromJson(response.getResponseBody(), HashGAV.class);
    assertHashGAV(hash, groupId, artifactId, version, extension, classifier, comment, createTime, hashGAV);
  }

  @Test
  public void testSetHashGAV_KnownToSaaS() throws Exception {
    setSaasResponseForURI("rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
            + "&extension=" + extension + "&classifier=" + classifier,
        JsonHelpers.asJson(ComponentSummary.create(true)),
        200
    );

    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(hashGAV));
    assertResponseStatus(400, response);
    assertEquals(
        "The 'HashGAVResourceTest_G:HashGAVResourceTest_A:HashGAVResourceTest_V:HashGAVResourceTest_E" +
            ":HashGAVResourceTest_C' coordinates are already in use",
        response.getResponseBody());
  }

  @Test
  public void testUnclaimComponent() throws Exception {
    Date createTime = new Date();

    setSaasResponseForURI("rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
            + "&extension=" + extension + "&classifier=" + classifier,
        JsonHelpers.asJson(ComponentSummary.create(false)),
        200
    );

    hashGAV.setComment(comment);
    hashGAV.setCreateTime(createTime);
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(hashGAV));
    assertResponseStatus(200, response);
    hashGAV = JsonHelpers.fromJson(response.getResponseBody(), HashGAV.class);

    response = AuthedRestAccess.delete(getServiceURL() + "/" + hashGAV.getHash());
    assertResponseStatus(204, response);
  }

  private void assertHashGAV(String hash, String groupId, String artifactId, String version, String extension,
      String classifier, String comment, Date createTime, HashGAV hashGAV) {
    assertEquals(hash, hashGAV.getHash());
    assertEquals(groupId, hashGAV.getGroupId());
    assertEquals(artifactId, hashGAV.getArtifactId());
    assertEquals(version, hashGAV.getVersion());
    assertEquals(extension, hashGAV.getExtension());
    assertEquals(classifier, hashGAV.getClassifier());
    assertEquals(comment, hashGAV.getComment());
    assertEquals(createTime, hashGAV.getCreateTime());
  }

  private String getServiceURL() {
    return getRestBaseUrl() + HashGAVResource.SERVICE_PATH;
  }
}
