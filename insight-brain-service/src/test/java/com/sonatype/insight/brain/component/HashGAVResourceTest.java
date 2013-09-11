/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HashGAVResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testSetHashGAV_UnknownToSaaS() throws Exception {
    String hash = "ab1234ab1234ab";
    String groupId = "HashGAVResourceTest_G";
    String artifactId = "HashGAVResourceTest_A";
    String version = "HashGAVResourceTest_V";
    String extension = "HashGAVResourceTest_E";
    String classifier = "HashGAVResourceTest_C";
    String comment = "HashGAVResourceTest_Comment";
    Date createTime = new Date();

    setSaasResponseForURI("rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
        + "&extension=" + extension + "&classifier=" + classifier, JsonHelpers.asJson(ComponentSummary.create(false)),
        200);

    HashGAV hashGAV = new HashGAV(hash, groupId, artifactId, version, extension, classifier);
    hashGAV.setComment(comment);
    hashGAV.setCreateTime(createTime);
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(hashGAV));
    assertResponseStatus(200, response);
    hashGAV = JsonHelpers.fromJson(response.getResponseBody(), HashGAV.class);
    assertHashGAV(hash, groupId, artifactId, version, extension, classifier, comment, createTime, hashGAV);

    new HashGAVDAO().delete(hashGAV);
  }

  @Test
  public void testSetHashGAV_KnownToSaaS() throws Exception {
    String hash = "ab1234ab1234ab";
    String groupId = "HashGAVResourceTest_G";
    String artifactId = "HashGAVResourceTest_A";
    String version = "HashGAVResourceTest_V";
    String extension = "HashGAVResourceTest_E";
    String classifier = "HashGAVResourceTest_C";

    setSaasResponseForURI("rest/ide/component?groupId=" + groupId + "&artifactId=" + artifactId + "&version=" + version
        + "&extension=" + extension + "&classifier=" + classifier, JsonHelpers.asJson(ComponentSummary.create(true)),
        200);

    HashGAV hashGAV = new HashGAV(hash, groupId, artifactId, version, extension, classifier);
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(hashGAV));
    assertResponseStatus(400, response);
    assertEquals(
        "The 'HashGAVResourceTest_G:HashGAVResourceTest_A:HashGAVResourceTest_V:HashGAVResourceTest_E:HashGAVResourceTest_C' coordinates are already in use",
        response.getResponseBody());
  }

  private void assertHashGAV(String hash, String groupId, String artifactId, String version, String extension,
      String classifier, String comment, Date createTime, HashGAV hashGAV)
  {
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
