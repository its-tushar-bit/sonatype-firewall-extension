/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.scan.file.clair.ClairScannerResult;
import com.sonatype.insight.scan.file.clair.ClairScannerVulnerability;

import com.google.gson.Gson;

public class ClairScannerResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Gson GSON = new Gson();

  @Override
  public String handleAndFilterContents(final ThirdPartyScanContent content) {
    return filterContent(content.getContent());
  }

  private String filterContent(String content) {
    ClairScannerResult clairScannerResult = GSON.fromJson(content, ClairScannerResult.class);
    Set<ClairScannerVulnerability> filteredVulnerabilities = new HashSet<>();
    if (clairScannerResult != null && clairScannerResult.getVulnerabilities() != null
        && !clairScannerResult.getVulnerabilities().isEmpty()) {

      clairScannerResult.getVulnerabilities().stream().map(vulnerability -> {
        ClairScannerVulnerability filteredVulnerability = new ClairScannerVulnerability();
        filteredVulnerability.setFeatureName(vulnerability.getFeatureName());
        filteredVulnerability.setFeatureVersion(vulnerability.getFeatureVersion());
        filteredVulnerability.setNamespace(vulnerability.getNamespace());
        return filteredVulnerability;
      }).forEach(filteredVulnerability -> {
        filteredVulnerabilities.add(filteredVulnerability);
      });

      ClairScannerResult filteredClairScannerResult = new ClairScannerResult();
      filteredClairScannerResult.setVulnerabilities(filteredVulnerabilities);
      return GSON.toJson(filteredClairScannerResult);
    }
    return content;
  }
}
