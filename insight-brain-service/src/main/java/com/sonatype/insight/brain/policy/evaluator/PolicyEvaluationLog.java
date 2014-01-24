/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyEvaluationLog
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final File auditDir;

  private final JsonStore auditStore;

  public PolicyEvaluationLog(final File auditDir) {
    this.auditDir = auditDir;
    auditStore = JsonUtils.fileStore(auditDir);
  }

  public PolicyEvaluation lastByStage(final String stageId) throws IOException {
    migrate();
    final ContainerNode<?> auditContainer = auditStore.history(null, filename(stageId));
    if (auditContainer != null) {
      return JsonUtils.asPojo(auditContainer.get("aaData").get(0), PolicyEvaluation.class);
    }
    return null;
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given stage.
   */
  public PolicyEvaluation lastPrimaryByStage(final String stageId) throws IOException {
    migrate();
    final ContainerNode<?> auditContainer = auditStore.history(null, filename(stageId));
    if (auditContainer != null) {
      JsonNode auditData = auditContainer.get("aaData");
      for (JsonNode audit : auditData) {
        PolicyEvaluation policyEvaluation = JsonUtils.asPojo(audit, PolicyEvaluation.class);
        if (!policyEvaluation.isReevaluation()) {
          return policyEvaluation;
        }
      }
    }
    return null;
  }

  /**
   * Returns all evaluations for the given stage. Returns empty list if no such evaluations.
   */
  public List<PolicyEvaluation> allByStage(final String stageId) throws IOException {
    migrate();
    final ContainerNode<?> auditContainer = auditStore.history(null, filename(stageId));
    if (auditContainer != null) {
      JsonNode auditData = auditContainer.get("aaData");
      ArrayList<PolicyEvaluation> result = new ArrayList<PolicyEvaluation>();
      for (JsonNode audit : auditData) {
        result.add(JsonUtils.asPojo(audit, PolicyEvaluation.class));
      }
      return result;
    }
    return Collections.emptyList();
  }

  public PolicyEvaluation lastByScan(final String scanId) throws IOException {
    for (StageType stageType : StageTypes.getAll()) {
      final ContainerNode<?> auditContainer = auditStore.history(null, filename(stageType.getId()));
      if (auditContainer != null) {
        JsonNode auditData = auditContainer.get("aaData");
        for (JsonNode audit : auditData) {
          if (audit.get("scanId").asText().equals(scanId)) {
            return JsonUtils.asPojo(audit, PolicyEvaluation.class);
          }
        }
      }
    }
    return null;
  }

  private ObjectNode createJsonNode(PolicyEvaluation policyEvaluation) {
    final ObjectNode logEntry = JsonUtils.asTree(Collections.singletonMap("stage", policyEvaluation.getStage()));
    logEntry.put("scanId", policyEvaluation.getScanId());
    logEntry.put("reevaluation", policyEvaluation.isReevaluation());
    return logEntry;
  }

  public void add(PolicyEvaluation policyEvaluation, String user, String ip) throws IOException {
    add(JsonUtils.stamp(user, ip, "", createJsonNode(policyEvaluation)));
  }

  /**
   * For testing purposes, don't use
   */
  public void add(PolicyEvaluation policyEvaluation, String user, String ip, final long time) throws IOException
  {
    ObjectNode stamped = JsonUtils.stamp(user, ip, "", createJsonNode(policyEvaluation));
    stamped.put("time", time);
    add(stamped);
  }

  private void add(final ObjectNode stampedLogEntry) throws IOException {
    migrate();
    auditStore.commit(filename(stageId(stampedLogEntry)), stampedLogEntry);
  }

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
