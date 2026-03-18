/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.license.MultiLicense;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.ConjunctiveLicenseSet;
import org.spdx.library.model.license.DisjunctiveLicenseSet;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.library.model.license.SpdxListedLicense;

public class SpdxLicenseExpressionUtil
{
  private static final Logger log = LoggerFactory.getLogger(SpdxLicenseExpressionUtil.class);

  protected final MultiLicenseDAO multiLicenseDAO;

  public SpdxLicenseExpressionUtil(final MultiLicenseDAO multiLicenseDAO) {
    this.multiLicenseDAO = multiLicenseDAO;
  }

  /**
   * Extracts license information from SPDX license expressions
   */
  public void parseLicenses(
      final AnyLicenseInfo license,
      final Map<String, String> processedLicenses,
      final String packageUrl) throws InvalidSPDXAnalysisException
  {
    if (license instanceof SpdxListedLicense) {
      processSpdxListedLicense((SpdxListedLicense) license, processedLicenses);
    }
    else if (license instanceof ExtractedLicenseInfo) {
      processExtractedLicenseInfo((ExtractedLicenseInfo) license, processedLicenses);
    }
    else if (license instanceof DisjunctiveLicenseSet) {
      processDisjunctiveLicenseSet((DisjunctiveLicenseSet) license, processedLicenses, packageUrl);
    }
    else if (license instanceof ConjunctiveLicenseSet) {
      processConjunctiveLicenseSet((ConjunctiveLicenseSet) license, processedLicenses, packageUrl);
    }
    else {
      log.debug("Component with packageUrl {} has unknown license with ID {}", packageUrl, license.getId());
    }
  }

  private void processConjunctiveLicenseSet(
      ConjunctiveLicenseSet licenseSet,
      Map<String, String> processedLicenses,
      String packageUrl) throws InvalidSPDXAnalysisException
  {
    // process the members as individual licenses
    for (AnyLicenseInfo licenseInfo : licenseSet.getMembers()) {
      parseLicenses(licenseInfo, processedLicenses, packageUrl);
    }
  }

  private void processDisjunctiveLicenseSet(
      DisjunctiveLicenseSet licenseSet,
      Map<String, String> processedLicenses,
      String packageUrl) throws InvalidSPDXAnalysisException
  {
    // check if the disjunctive license set is one of Sonatype's multi-licenses
    MultiLicense sonatypeLicense = getMultiLicenseIfAny(licenseSet);
    if (sonatypeLicense != null && !processedLicenses.containsKey(sonatypeLicense.getId())) {
      processedLicenses.put(sonatypeLicense.getId(), sonatypeLicense.getShortDisplayName());
    }
    else {
      // process the members as individual licenses
      for (AnyLicenseInfo licenseInfo : licenseSet.getMembers()) {
        parseLicenses(licenseInfo, processedLicenses, packageUrl);
      }
    }
  }

  private void processExtractedLicenseInfo(ExtractedLicenseInfo license, Map<String, String> processedLicenses) {
    if (StringUtils.isEmpty(license.getId())) {
      return;
    }
    String licenseId = StringUtils.remove(license.getId(), "LicenseRef-");
    MultiLicense multiLicense = multiLicenseDAO.getByIdNoReload(licenseId);
    if (multiLicense != null && !processedLicenses.containsKey(licenseId)) {
      processedLicenses.put(licenseId, multiLicense.getShortDisplayName());
    }
  }

  private void processSpdxListedLicense(SpdxListedLicense listedLicense, Map<String, String> processedLicenses) {
    MultiLicense sonatypeLicense = getSonatypeLicense(listedLicense.getLicenseId());
    if (sonatypeLicense != null && !processedLicenses.containsKey(sonatypeLicense.getId())) {
      processedLicenses.put(sonatypeLicense.getId(), sonatypeLicense.getShortDisplayName());
    }
    else if (!processedLicenses.containsKey(listedLicense.getId())) {
      processedLicenses.put(listedLicense.getId(), listedLicense.getLicenseId());
    }
  }

  private MultiLicense getMultiLicenseIfAny(
      final DisjunctiveLicenseSet licenseSet) throws InvalidSPDXAnalysisException
  {
    // build a possible license name by adding member license IDs (sorted) joined by "or" operator
    List<String> sortedLicenseIds =
        licenseSet.getMembers().stream().map(AnyLicenseInfo::getId).sorted().collect(Collectors.toList());
    String licenseName = StringUtils.join(sortedLicenseIds, " or ");
    return getSonatypeLicense(licenseName);
  }

  private MultiLicense getSonatypeLicense(String licenseId) {
    MultiLicense multiLicense = multiLicenseDAO.getByIdNoReload(licenseId);
    if (multiLicense == null) { // fallback, try the ID as name
      multiLicense = multiLicenseDAO.getByNameNoReload(licenseId);
    }
    return multiLicense;
  }
}
