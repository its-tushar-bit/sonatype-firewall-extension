/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;

import org.apache.commons.codec.digest.DigestUtils;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.manifest.ClairScannerResult;
import com.sonatype.insight.scan.manifest.ClairScannerVulnerability;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.*;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.OTHER;

public class ClairScannerResultHandler
    implements ThirdPartyScanResultHandler
{
  private static final Gson GSON = new Gson();

  private static final Logger log = LoggerFactory.getLogger(ClairScannerResultHandler.class);

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  public ClairScannerResultHandler(
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO)
  {
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.fileCoordinatePersister = fileCoordinatePersister;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
  }

  @Override
  public FilteredThirdPartyContent handleAndFilterContents(
      ThirdPartyScanContent content,
      ThirdPartyFile thirdPartyFile)
  {
    ClairScannerResult clairScannerResult = GSON.fromJson(content.getContent(), ClairScannerResult.class);

    if (clairScannerResult != null) {
      try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
        tx.begin();
        ClairScannerResult filteredClairScannerResult = new ClairScannerResult();

        if (clairScannerResult.getVulnerabilities() != null) {
          Map<String, String> hashFileCoordinateIdMap = new HashMap<>();

          Set<ClairScannerVulnerability> filteredVulnerabilities = clairScannerResult.getVulnerabilities()
              .stream()
              .map(vulnerability -> saveVulnerability(vulnerability, thirdPartyFile, hashFileCoordinateIdMap, tx))
              .filter(Objects::nonNull)
              .map(this::filterIdentities)
              .collect(Collectors.toSet());

          filteredClairScannerResult.setVulnerabilities(filteredVulnerabilities);
        }

        tx.commit();
        return new FilteredThirdPartyContent(GSON.toJson(filteredClairScannerResult));
      }
    }

    return new FilteredThirdPartyContent(content.getContent());
  }

  private ClairScannerVulnerability saveVulnerability(
      ClairScannerVulnerability vulnerability,
      ThirdPartyFile thirdPartyFile,
      Map<String, String> hashFileCoordinateIdMap,
      TransactionContext tx)
  {
    String format = getValidFormat(vulnerability.getNamespace());
    vulnerability.setNamespace(format);

    String name = getTruncatedName(vulnerability.getFeatureName());
    vulnerability.setFeatureName(name);

    String version = getTruncatedVersion(vulnerability.getFeatureVersion());
    vulnerability.setFeatureVersion(version);

    // format, name and version are mandatory, if they are null/blank it should be skipped
    if (StringUtils.isAnyBlank(format, name, version)) {
      log.debug("Skipping clair vulnerability with missing mandatory coordinates. format={}, name={}, version={}. ",
          format, name, version);
      return null;
    }

    String fakeHash = ThirdPartyScanResultUtils
        .hash(format + ":" + vulnerability.getFeatureName() + ":" + vulnerability.getFeatureVersion());

    String fileCoordinateId = hashFileCoordinateIdMap.get(fakeHash);

    if (fileCoordinateId == null) {
      ThirdPartyFileCoordinate fileCoordinate =
          new ThirdPartyFileCoordinate(fakeHash, IdentificationSource.CLAIR.getId(), format, name, version,
              thirdPartyFile.getId());
      fileCoordinate.setComponentRef(
          DigestUtils.sha1Hex(format + ":" + name + ":" + version));
      fileCoordinate = fileCoordinatePersister.persist(tx, fileCoordinate);

      fileCoordinateId = fileCoordinate.getId();
      hashFileCoordinateIdMap.put(fakeHash, fileCoordinateId);
    }
    saveVulnerabilityInfo(vulnerability, fileCoordinateId, tx);
    return vulnerability;
  }

  private void saveVulnerabilityInfo(
      final ClairScannerVulnerability vulnerability,
      final String fileCoordinateId,
      final TransactionContext tx)
  {
    float severity = getSeverity(vulnerability.getSeverity());

    String link = getTruncatedLink(vulnerability.getLink());

    String fixedBy = getTruncatedFixedBy(vulnerability.getFixedBy());

    String refId = getTruncatedRefId(vulnerability.getVulnerability());

    String vulnerabilitySource =
        getTruncatedVulnerabilitySource(getVulnerabilitySourceFromReference(refId));

    String severityDescription = getTruncatedSeverityDescription(vulnerability.getSeverity());

    ThirdPartyCoordinateSecurity coordinateSecurity =
        new ThirdPartyCoordinateSecurity(fileCoordinateId, refId, null,
            vulnerability.getDescription(), link, severity, fixedBy);
    coordinateSecurity.setVulnerabilitySource(vulnerabilitySource);
    coordinateSecurity.setResearchType(getResearchTypeForThirdPartyVulnerability(
        coordinateSecurity.getVulnerabilitySource(), coordinateSecurity.getRefId()));
    coordinateSecurity.setDetectionType(OTHER.getId());
    coordinateSecurity.setSeverityDescription(severityDescription);
    coordinateSecurity.setIdentificationSources(IdentificationSource.SBOM.getId());
    thirdPartyCoordinateSecurityDAO.insertSafely(tx, coordinateSecurity);
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
