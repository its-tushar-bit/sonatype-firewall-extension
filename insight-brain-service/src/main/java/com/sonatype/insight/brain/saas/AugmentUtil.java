/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentUtil
{
  public static ArrayNode getSVData(InsightWork work, String applicationId, ComponentIdentifier componentIdentifier,
      List<SecurityVulnerability> securityVulnerabilities) throws IOException
  {
    if (securityVulnerabilities == null || securityVulnerabilities.isEmpty()) {
      return null;
    }
    if (componentIdentifier == null) {
      return null;
    }

    ArrayNode svData = new ArrayNode(JsonNodeFactory.instance);
    for (SecurityVulnerability securityVulnerability : securityVulnerabilities) {
      ObjectNode svNode = svData.objectNode();
      svData.add(svNode);

      // Add groupId, artifactId and version so that the node will merge successfully with old versions of security.json
      if (componentIdentifier.isMaven()) {
        svNode.put("groupId", componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
        svNode.put("artifactId", componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
        svNode.put("version", componentIdentifier.get(ComponentIdentifier.VERSION));
      }

      svNode.put("componentIdentifier", JsonUtils.asTree(componentIdentifier));
      svNode.put("reference", securityVulnerability.getRefId());
      svNode.put("source", securityVulnerability.getSource());
    }
    File auditDir = work.getAuditDir(applicationId);
    JsonUtils.fileStore(auditDir).augment(svData, "security.json");
    return svData;
  }
}
