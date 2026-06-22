/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.tools;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.guide.mcp.model.McpBatchItem;
import com.sonatype.insight.brain.guide.mcp.model.McpCve;
import com.sonatype.insight.brain.guide.mcp.model.McpMethodSignature;
import com.sonatype.insight.brain.guide.mcp.model.McpPolicyCompliance;
import com.sonatype.insight.brain.guide.mcp.model.McpRecommendationItem;
import com.sonatype.insight.brain.guide.mcp.model.McpRecommendedVersion;
import com.sonatype.insight.brain.guide.mcp.model.McpVersionDetails;
import com.sonatype.insight.brain.guide.mcp.model.McpVulnerabilities;
import com.sonatype.insight.brain.guide.mcp.model.McpVulnerableMethod;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpResponseFormatter
{
  private static final Logger log = LoggerFactory.getLogger(McpResponseFormatter.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String FALLBACK_FAILURE_JSON = McpBatchItem.FALLBACK_FAILURE_JSON;

  private McpResponseFormatter() {
  }

  public static String formatComponentVersion(String purl, String rawJson, McpPolicyCompliance policyResult) {
    try {
      Map<String, Object> response = parseJson(rawJson);
      McpVersionDetails details = convertToVersionDetails(response, policyResult);
      return mapper.writeValueAsString(List.of(McpBatchItem.success(purl, details)));
    }
    catch (Exception e) {
      log.warn("Failed to format component version for {}: {}", purl, e.getMessage(), e);
      return toFailureJson(purl, "Failed to process component data");
    }
  }

  // Same transformation as formatComponentVersion — search-server returns an identical response shape for both.
  public static String formatLatestVersion(String purl, String rawJson, McpPolicyCompliance policyResult) {
    try {
      Map<String, Object> response = parseJson(rawJson);
      McpVersionDetails details = convertToVersionDetails(response, policyResult);
      return mapper.writeValueAsString(List.of(McpBatchItem.success(purl, details)));
    }
    catch (Exception e) {
      log.warn("Failed to format latest version for {}: {}", purl, e.getMessage(), e);
      return toFailureJson(purl, "Failed to process component data");
    }
  }

  public static String formatRecommendations(String purl, String rawJson) {
    try {
      Map<String, Object> response = parseJson(rawJson);
      String outcome = (String) response.get("outcome");
      McpRecommendedVersion fromVersion = convertRecommendedVersion(asMap(response.get("fromVersion")));

      List<McpRecommendedVersion> toVersions = new ArrayList<>();
      Object toList = response.get("toVersions");
      if (toList instanceof List<?> list) {
        for (Object item : list) {
          McpRecommendedVersion v = convertRecommendedVersion(asMap(item));
          if (v != null) {
            toVersions.add(v);
          }
        }
      }

      McpRecommendationItem result = McpRecommendationItem.success(
          purl, outcome, fromVersion, toVersions.isEmpty() ? null : toVersions);
      return mapper.writeValueAsString(List.of(result));
    }
    catch (Exception e) {
      log.warn("Failed to format recommendations for {}: {}", purl, e.getMessage(), e);
      return toRecommendationFailureJson(purl, "Failed to process recommendation data");
    }
  }

  private static McpVersionDetails convertToVersionDetails(
      Map<String, Object> response,
      McpPolicyCompliance policyResult)
  {
    String version = (String) response.get("version");
    Set<String> licenses = extractLicenseNames(response);
    Long catalogDate = extractCatalogDate(response);
    boolean malicious = extractMalwareFlag(response);
    boolean endOfLife = Boolean.TRUE.equals(response.get("endOfLife"));
    McpVulnerabilities vulnerabilities = extractVulnerabilities(response);

    return new McpVersionDetails(
        version, endOfLife, vulnerabilities, licenses, catalogDate, malicious, policyResult);
  }

  private static Set<String> extractLicenseNames(Map<String, Object> response) {
    Set<String> licenses = new LinkedHashSet<>();
    List<?> licensesList = (List<?>) response.get("licenses");
    if (licensesList != null) {
      for (Object lic : licensesList) {
        if (lic instanceof Map<?, ?> licMap) {
          Object name = licMap.get("licenseName");
          if (name instanceof String s && !s.isEmpty()) {
            licenses.add(s);
          }
        }
        else if (lic instanceof String s && !s.isEmpty()) {
          licenses.add(s);
        }
      }
    }
    return licenses.isEmpty() ? null : licenses;
  }

  private static Long extractCatalogDate(Map<String, Object> response) {
    Object publishedDate = response.get("publishedDate");
    if (publishedDate == null) {
      publishedDate = response.get("catalogDate");
    }
    if (publishedDate instanceof Number n) {
      return n.longValue();
    }
    if (publishedDate instanceof String s) {
      try {
        return Instant.parse(s).toEpochMilli();
      }
      catch (Exception e) {
        log.debug("Failed to parse date: {}", s);
      }
    }
    return null;
  }

  private static boolean extractMalwareFlag(Map<String, Object> response) {
    if (response.get("isMalware") instanceof Boolean b && b) {
      return true;
    }
    if (response.get("malicious") instanceof Boolean b && b) {
      return true;
    }

    List<?> componentsList = (List<?>) response.get("components");
    if (componentsList != null) {
      for (Object comp : componentsList) {
        if (comp instanceof Map<?, ?> compMap) {
          List<?> refids = (List<?>) compMap.get("refids");
          if (refids != null) {
            for (Object refid : refids) {
              if (refid instanceof Map<?, ?> refMap) {
                if (refMap.get("isMalware") instanceof Boolean m && m) {
                  return true;
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  private static McpVulnerabilities extractVulnerabilities(Map<String, Object> response) {
    List<McpCve> cves = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    List<?> componentsList = (List<?>) response.get("components");
    if (componentsList != null) {
      for (Object comp : componentsList) {
        if (comp instanceof Map<?, ?> compMap) {
          List<?> refids = (List<?>) compMap.get("refids");
          if (refids != null) {
            for (Object refid : refids) {
              if (refid instanceof Map<?, ?> refMap) {
                String id = (String) refMap.get("refid");
                if (id != null && !seen.contains(id)) {
                  seen.add(id);
                  boolean isMalware = refMap.get("isMalware") instanceof Boolean m && m;
                  if (!isMalware) {
                    Float cvss = null;
                    Object severity = refMap.get("severity");
                    if (severity instanceof Number n) {
                      cvss = n.floatValue();
                    }
                    cves.add(new McpCve(id, cvss));
                  }
                }
              }
            }
          }
        }
      }
    }

    Object vulnerabilities = response.get("vulnerabilities");
    if (vulnerabilities instanceof Map<?, ?> vulnMap) {
      List<?> cveList = (List<?>) vulnMap.get("cves");
      if (cveList != null) {
        for (Object cve : cveList) {
          if (cve instanceof Map<?, ?> cveMap) {
            String id = (String) cveMap.get("id");
            Float cvss = null;
            Object score = cveMap.get("cvssScore");
            if (score instanceof Number n) {
              cvss = n.floatValue();
            }
            if (id != null && !seen.contains(id)) {
              seen.add(id);
              cves.add(new McpCve(id, cvss));
            }
          }
        }
      }
    }

    return cves.isEmpty() ? null : new McpVulnerabilities(cves);
  }

  private static McpRecommendedVersion convertRecommendedVersion(Map<String, Object> versionMap) {
    if (versionMap == null) {
      return null;
    }

    String version = (String) versionMap.get("version");
    String breakingChangesCount = versionMap.get("breakingChangesCount") != null
        ? String.valueOf(versionMap.get("breakingChangesCount"))
        : null;

    Map<String, Double> directVulns = toDoubleMap(versionMap.get("directVulnerabilities"));
    Map<String, Double> transitiveVulns = toDoubleMap(versionMap.get("transitiveVulnerabilities"));
    Map<String, Integer> licenseThreatLevels = toIntegerMap(versionMap.get("licenseThreatLevels"));

    Integer dts = null;
    Object dtsObj = versionMap.get("developerTrustScore");
    if (dtsObj instanceof Number n) {
      dts = n.intValue();
    }

    Object rawMethods = versionMap.get("vulnerableMethods");
    List<McpVulnerableMethod> vulnerableMethods = extractVulnerableMethods(
        rawMethods instanceof List<?> list ? list : null);

    return new McpRecommendedVersion(
        version, breakingChangesCount, directVulns, transitiveVulns,
        licenseThreatLevels, vulnerableMethods, dts);
  }

  private static List<McpVulnerableMethod> extractVulnerableMethods(List<?> raw) {
    if (raw == null || raw.isEmpty()) {
      return null;
    }
    List<McpVulnerableMethod> result = new ArrayList<>();
    for (Object item : raw) {
      if (item instanceof Map<?, ?> map) {
        String refId = (String) map.get("refid");
        if (refId == null) {
          refId = (String) map.get("refId");
        }
        List<?> sigs = (List<?>) map.get("methodSignatures");
        List<McpMethodSignature> signatures = new ArrayList<>();
        if (sigs != null) {
          for (Object sig : sigs) {
            if (sig instanceof Map<?, ?> sigMap) {
              String type = (String) sigMap.get("type");
              String signature = (String) sigMap.get("signature");
              List<Integer> params = toIntegerList(sigMap.get("vulnerableParameters"));
              signatures.add(new McpMethodSignature(type, signature, params));
            }
          }
        }
        result.add(new McpVulnerableMethod(refId, signatures.isEmpty() ? null : signatures));
      }
    }
    return result.isEmpty() ? null : result;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object obj) {
    if (obj instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return null;
  }

  private static Map<String, Double> toDoubleMap(Object obj) {
    if (!(obj instanceof Map<?, ?> raw)) {
      return null;
    }
    Map<String, Double> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : raw.entrySet()) {
      if (entry.getKey() instanceof String key && entry.getValue() instanceof Number num) {
        result.put(key, num.doubleValue());
      }
    }
    return result.isEmpty() ? null : result;
  }

  private static Map<String, Integer> toIntegerMap(Object obj) {
    if (!(obj instanceof Map<?, ?> raw)) {
      return null;
    }
    Map<String, Integer> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : raw.entrySet()) {
      if (entry.getKey() instanceof String key && entry.getValue() instanceof Number num) {
        result.put(key, num.intValue());
      }
    }
    return result.isEmpty() ? null : result;
  }

  private static List<Integer> toIntegerList(Object obj) {
    if (!(obj instanceof List<?> raw) || raw.isEmpty()) {
      return null;
    }
    List<Integer> result = new ArrayList<>();
    for (Object item : raw) {
      if (item instanceof Number n) {
        result.add(n.intValue());
      }
    }
    return result.isEmpty() ? null : result;
  }

  private static Map<String, Object> parseJson(String json) throws Exception {
    return mapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>()
    {
    });
  }

  private static String toFailureJson(String purl, String errorMessage) {
    try {
      return mapper.writeValueAsString(List.of(McpBatchItem.failure(purl, errorMessage)));
    }
    catch (Exception e) {
      return FALLBACK_FAILURE_JSON;
    }
  }

  private static String toRecommendationFailureJson(String purl, String errorMessage) {
    try {
      return mapper.writeValueAsString(List.of(McpRecommendationItem.failure(purl, errorMessage)));
    }
    catch (Exception e) {
      return FALLBACK_FAILURE_JSON;
    }
  }
}
