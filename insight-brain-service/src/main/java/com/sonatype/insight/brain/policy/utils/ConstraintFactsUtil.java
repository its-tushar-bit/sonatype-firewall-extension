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
      return new CveData(null, null, null, null, null, null, null);
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

    return new CveData(null, null, null, null, null, null, null);
  }

  private static CveData extractFromTriggerData(final Map<String, Object> triggerData) {
    String cveNumber = null;
    Object cvssScore = null;
    String vulnerabilityCategory = null;
    String malwareSeverity = null;
    String malwareAttackVector = null;
    String cvssAttackVector = null;

    if (triggerData.containsKey("refId")) {
      cveNumber = (String) triggerData.get("refId");
    }

    if (triggerData.containsKey("severity")) {
      cvssScore = triggerData.get("severity");
    }

    // CVE vulnerability category (e.g., "ARBITRARY_CODE_EXECUTION", "DENIAL_OF_SERVICE")
    if (triggerData.containsKey("vulnerabilityCategoryId")) {
      vulnerabilityCategory = (String) triggerData.get("vulnerabilityCategoryId");
    }

    // Malware severity description (e.g., "Malicious", "Moderate") from ThirdPartyVulnerability
    if (triggerData.containsKey("severityDescription")) {
      malwareSeverity = (String) triggerData.get("severityDescription");
    }

    // Malware attack vector (e.g., "Trojan", "Brandjack", "Hijack") from ThirdPartyVulnerability
    if (triggerData.containsKey("attackVector")) {
      malwareAttackVector = (String) triggerData.get("attackVector");
    }

    // CVSS attack vector (e.g., "Network", "Adjacent", "Local", "Physical") from CVSS vectorString
    if (triggerData.containsKey("vectorString")) {
      String vectorString = (String) triggerData.get("vectorString");
      cvssAttackVector = parseAttackVectorFromCvssVector(vectorString);
    }

    // Threat types are not available in constraint facts - they must be enriched from HDS
    return new CveData(cveNumber, cvssScore, vulnerabilityCategory, malwareSeverity,
        malwareAttackVector, cvssAttackVector, null);
  }

  /**
   * Parses the CVSS Attack Vector (AV) component from a CVSS vector string.
   *
   * This extracts the CVSS attack vector (Network, Adjacent, Local, Physical) which describes
   * how a vulnerability is exploited, NOT the malware attack vector (trojan, brandjack, hijack)
   * which describes how malware reaches a system.
   *
   * Note: This method is defensive against malformed or custom CVSS vectors that may not follow
   * the standard format, as customers can enter custom vulnerability attributes. Returns null
   * if the attack vector cannot be parsed.
   *
   * @param vectorString CVSS vector string (e.g., "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H")
   * @return CVSS Attack Vector value (e.g., "Network", "Local", "Adjacent", "Physical"), or null if not found
   */
  public static String parseAttackVectorFromCvssVector(final String vectorString) {
    if (vectorString == null || vectorString.isEmpty()) {
      return null;
    }

    try {
      // CVSS vector format: CVSS:3.0/AV:N/AC:L/... or AV:N/AC:L/... (v2)
      // We need to extract the AV (Attack Vector) component
      // Handle malformed custom vectors gracefully by catching any exceptions
      String[] components = vectorString.split("/");
      for (String component : components) {
        if (component.startsWith("AV:") && component.length() > 3) {
          String avCode = component.substring(3).trim(); // Extract value after "AV:"
          if (!avCode.isEmpty()) {
            return mapAttackVectorCode(avCode);
          }
        }
      }
    }
    catch (RuntimeException e) {
      // Malformed custom CVSS vector - return null instead of breaking
      return null;
    }

    return null;
  }

  /**
   * Maps CVSS Attack Vector codes to human-readable names.
   *
   * @param code CVSS AV code (N, A, L, P)
   * @return Human-readable attack vector name
   */
  private static String mapAttackVectorCode(final String code) {
    switch (code) {
      case "N":
        return "Network";
      case "A":
        return "Adjacent";
      case "L":
        return "Local";
      case "P":
        return "Physical";
      default:
        return code; // Return original code if unknown
    }
  }

  /**
   * Holder for CVE and malware data extracted from constraint facts.
   *
   * @param cveNumber CVE identifier (e.g., "CVE-2023-12345" or "sonatype-...")
   * @param cvssScore CVSS severity score (for CVEs)
   * @param vulnerabilityCategory CVE vulnerability category
   *        (e.g., "ARBITRARY_CODE_EXECUTION", "DENIAL_OF_SERVICE")
   * @param malwareSeverity Malware severity description (e.g., "Malicious", "Moderate")
   *        from ThirdPartyVulnerability
   * @param malwareAttackVector Malware attack vector (e.g., "Trojan", "Brandjack", "Hijack")
   *        from ThirdPartyVulnerability
   * @param cvssAttackVector CVSS attack vector (e.g., "Network", "Adjacent", "Local", "Physical")
   *        parsed from CVSS vectorString
   * @param threatTypes List of threat type classifications
   *        (e.g., ["secrets_exfiltration", "backdoor", "crypto_miner"]) - populated from HDS
   */
  public record CveData(
      String cveNumber,
      Object cvssScore,
      String vulnerabilityCategory,
      String malwareSeverity,
      String malwareAttackVector,
      String cvssAttackVector,
      List<String> threatTypes)
  {
    public boolean hasCveData() {
      return cveNumber != null;
    }
  }
}
