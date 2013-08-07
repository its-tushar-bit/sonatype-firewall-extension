/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentUtil
{

  public static ObjectNode getLicenseData(InsightWork work, String applicationId, String groupId, String artifactId,
      String version) throws IOException
  {
    ArrayNode licenseData = new ArrayNode(JsonNodeFactory.instance);
    ObjectNode gavNode = licenseData.objectNode();
    licenseData.add(gavNode);
    gavNode.put("groupId", groupId);
    gavNode.put("artifactId", artifactId);
    gavNode.put("version", version);
    File auditDir = work.getAuditDir(applicationId);
    JsonUtils.fileStore(auditDir).augment(licenseData, "licenses.json");
    return (ObjectNode) licenseData.get(0);
  }

  public static ArrayNode getSVData(InsightWork work, String applicationId, String groupId, String artifactId,
      String version, List<SecurityVulnerability> securityVulnerabilities) throws IOException
  {
    if (securityVulnerabilities == null || securityVulnerabilities.isEmpty()) {
      return null;
    }
    ArrayNode svData = new ArrayNode(JsonNodeFactory.instance);
    for (SecurityVulnerability securityVulnerability : securityVulnerabilities) {
      ObjectNode svNode = svData.objectNode();
      svData.add(svNode);
      svNode.put("groupId", groupId);
      svNode.put("artifactId", artifactId);
      svNode.put("version", version);
      svNode.put("reference", securityVulnerability.getRefId());
      svNode.put("source", securityVulnerability.getSource());
    }
    File auditDir = work.getAuditDir(applicationId);
    JsonUtils.fileStore(auditDir).augment(svData, "security.json");
    return svData;
  }
}