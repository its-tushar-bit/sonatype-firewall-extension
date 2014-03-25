/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyEvaluationLog
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final File auditDir;

  public PolicyEvaluationLog(final File auditDir) {
    this.auditDir = auditDir;
  }

  // TODO Move to the policy evaluation migrator - CLM-2084
  private void migrate() throws IOException {
    synchronized (getClass()) {
      File legacy = new File(auditDir, "policyevaluations.json");
      if (legacy.exists() && !new File(auditDir, filename(Stage.ID_BUILD)).exists()) {
        final Map<String, ArrayNode> perStageLogs = new HashMap<String, ArrayNode>();
        perStageLogs.put(Stage.ID_BUILD, JsonNodeFactory.instance.arrayNode());
        final ContainerNode<?> auditContainer = JsonUtils.read(legacy);
        for (final JsonNode auditEntry : auditContainer) {
          final String stageId = stageId(auditEntry);
          ArrayNode perStageLog = perStageLogs.get(stageId);
          if (perStageLog == null) {
            perStageLog = JsonNodeFactory.instance.arrayNode();
            perStageLogs.put(stageId, perStageLog);
          }
          perStageLog.add(auditEntry);
        }
        for (final Map.Entry<String, ArrayNode> entry : perStageLogs.entrySet()) {
          JsonUtils.write(new File(auditDir, filename(entry.getKey())), entry.getValue());
        }
        try {
          new FileCleaner().delete(legacy);
        }
        catch (FileDeletionException e) {
          log.error("Failed to delete old policy evaluation log after data migration: {}", legacy, e);
        }
      }
    }
  }

  private String stageId(JsonNode stampedLogEntry) {
    return stampedLogEntry.get("data").get("stage").get("stageTypeId").asText();
  }

  private String filename(String stageId) {
    return "policy-evaluations-" + stageId + ".json";
  }
}
