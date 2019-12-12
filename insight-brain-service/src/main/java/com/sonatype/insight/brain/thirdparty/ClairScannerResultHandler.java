/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.file.clair.ClairScannerResult;
import com.sonatype.insight.scan.file.clair.ClairScannerVulnerability;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

public class ClairScannerResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Gson GSON = new Gson();

  private final ThirdPartyFileDAO thirdPartyFileDAO = new ThirdPartyFileDAO();

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = new ThirdPartyFileCoordinateDAO();

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO = new ThirdPartyCoordinateSecurityDAO();

  @Override
  public String handleAndFilterContents(ThirdPartyScanContent content, ThirdPartyFile thirdPartyFile) {
    ClairScannerResult clairScannerResult = GSON.fromJson(content.getContent(), ClairScannerResult.class);

    if (clairScannerResult != null) {
      try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
        tx.begin();
        ClairScannerResult filteredClairScannerResult = new ClairScannerResult();

        if (clairScannerResult.getVulnerabilities() != null) {
          Map<String, String> hashFileCoordinateIdMap = new HashMap<>();

          Set<ClairScannerVulnerability> filteredVulnerabilities = clairScannerResult.getVulnerabilities().stream()
              .map(vulnerability -> saveVulnerability(vulnerability, thirdPartyFile, hashFileCoordinateIdMap, tx))
              .map(this::filterIdentities).collect(Collectors.toSet());

          filteredClairScannerResult.setVulnerabilities(filteredVulnerabilities);
        }

        tx.commit();
        return GSON.toJson(filteredClairScannerResult);
      }
    }

    return content.getContent();
  }

  private ClairScannerVulnerability saveVulnerability(
      ClairScannerVulnerability vulnerability,
      ThirdPartyFile thirdPartyFile,
      Map<String, String> hashFileCoordinateIdMap,
      TransactionContext tx)
  {
    String format = ThirdPartyScanResultUtils.getValidFormat(vulnerability.getNamespace());
    vulnerability.setNamespace(format);

    String fakeHash = ThirdPartyScanResultUtils
        .hash(format + ":" + vulnerability.getFeatureName() + ":" + vulnerability.getFeatureVersion());

    String fileCoordinateId = hashFileCoordinateIdMap.get(fakeHash);

    if (fileCoordinateId == null) {
      ThirdPartyFileCoordinate fileCoordinate =
          new ThirdPartyFileCoordinate(fakeHash, IdentificationSource.CLAIR.getId(), format,
              vulnerability.getFeatureName(), vulnerability.getFeatureVersion(), thirdPartyFile.getId());
      thirdPartyFileCoordinateDAO.insert(tx, fileCoordinate);

      fileCoordinateId = fileCoordinate.getId();
      hashFileCoordinateIdMap.put(fakeHash, fileCoordinateId);
    }

    float severity = getSeverity(vulnerability.getSeverity());

    ThirdPartyCoordinateSecurity coordinateSecurity =
        new ThirdPartyCoordinateSecurity(fileCoordinateId, vulnerability.getVulnerability(),
            vulnerability.getDescription(), vulnerability.getLink(), severity, vulnerability.getFixedBy());
    coordinateSecurity.setVulnerabilitySource(
        ThirdPartyScanResultUtils.getVulnerabilitySourceFromReference(vulnerability.getVulnerability()));
    coordinateSecurity.setSeverityDescription(vulnerability.getSeverity());
    thirdPartyCoordinateSecurityDAO.insert(tx, coordinateSecurity);

    return vulnerability;
  }

  float getSeverity(final String severity) {
    // approximation based on the mapping range made by Clair:
    // https://github.com/coreos/clair/blob/master/database/severity.go#L31-L69
    // https://github.com/coreos/clair/blob/master/ext/vulnmdsrc/nvd/nvd.go#L266-L280
    switch (StringUtils.trimToEmpty(severity)) {
      case "Negligible":
        return 0.5f;
      case "Low":
        return 3f;
      case "Medium":
        return 6f;
      case "High":
        return 8f;
      case "Critical":
      case "Defcon1":
        return 10;
      default:
        return 0f;
    }
  }

  private ClairScannerVulnerability filterIdentities(ClairScannerVulnerability vulnerability) {
    ClairScannerVulnerability filteredVulnerability = new ClairScannerVulnerability();
    filteredVulnerability.setFeatureName(vulnerability.getFeatureName());
    filteredVulnerability.setFeatureVersion(vulnerability.getFeatureVersion());
    filteredVulnerability.setNamespace(vulnerability.getNamespace());
    return filteredVulnerability;
  }
}
