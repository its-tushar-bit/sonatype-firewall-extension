/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * Utility class for extracting and processing constraint fact data.
 *
 * @since 1.200.0
 */
public final class ConstraintFactsUtil
{
  private ConstraintFactsUtil() {
    // Utility class
  }

  /**
   * Extracts CVE data from constraint facts.
   *
   * @param constraintFacts the constraint facts to extract CVE data from
   * @return CveData containing extracted CVE information, or empty CveData if no CVE data found
   */
  public static CveData extractCveData(final List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null) {
      return new CveData(null, null, null, null);
    }

    for (ConstraintFact constraintFact : constraintFacts) {
      if (constraintFact.getConditionFacts() != null) {
        for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
          String triggerJson = conditionFact.getTriggerJson();

          if (triggerJson != null) {
            try {
              ConditionTrigger trigger = JsonUtils.parse(triggerJson, ConditionTrigger.class);
              Map<String, Object> triggerData = (Map<String, Object>) trigger.getTrigger();

              CveData cveData = extractFromTriggerData(triggerData);
              if (cveData.hasCveData()) {
                return cveData;
              }
            }
            catch (IOException e) { // NOPMD - Empty catch block intentional, continue to next fact
              // Unable to parse CVE data from this trigger, try next constraint fact
            }
          }
        }
      }
    }

    return new CveData(null, null, null, null);
  }

  private static CveData extractFromTriggerData(final Map<String, Object> triggerData) {
    String cveNumber = null;
    Object cvssScore = null;
    String vulnerabilityCategory = null;
    String attackVector = null;

    if (triggerData.containsKey("refId")) {
      cveNumber = (String) triggerData.get("refId");
    }

    if (triggerData.containsKey("severity")) {
      cvssScore = triggerData.get("severity");
    }

    if (triggerData.containsKey("category")) {
      vulnerabilityCategory = (String) triggerData.get("category");
    }

    if (triggerData.containsKey("attackVector")) {
      attackVector = (String) triggerData.get("attackVector");
    }

    return new CveData(cveNumber, cvssScore, vulnerabilityCategory, attackVector);
  }

  /**
   * Holder for CVE data extracted from constraint facts.
   *
   * @param cveNumber CVE identifier (e.g., "CVE-2023-12345")
   * @param cvssScore CVSS severity score
   * @param vulnerabilityCategory Vulnerability categorization
   * @param attackVector Security attack vector classification
   */
  public record CveData(
      String cveNumber,
      Object cvssScore,
      String vulnerabilityCategory,
      String attackVector)
  {
    public boolean hasCveData() {
      return cveNumber != null;
    }
  }
}
