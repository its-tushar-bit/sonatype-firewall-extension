/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.AbstractAssert;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.thymeleaf.util.StringUtils;

import static org.xmlunit.assertj.error.ShouldNotHaveThrown.shouldNotHaveThrown;

public class SpdxPackageAssert
    extends AbstractAssert<SpdxPackageAssert, SpdxPackage>
{
  protected SpdxPackageAssert(final SpdxPackage spdxPackage, final Class<?> selfType) {
    super(spdxPackage, selfType);
  }

  public SpdxPackageAssert hasConcludedLicense(final String expectedLicenseId) {
    isNotNull();
    try {
      AnyLicenseInfo license = actual.getLicenseConcluded();
      if (!licenseExpressionsEqual(license.toString(), expectedLicenseId)) {
        failWithMessage("Expected concluded license to be %s but  was %s", expectedLicenseId, license);
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxPackageAssert hasDeclaredLicense(final String expectedLicenseId) {
    isNotNull();
    try {
      AnyLicenseInfo license = actual.getLicenseDeclared();
      if (!licenseExpressionsEqual(license.toString(), expectedLicenseId)) {
        failWithMessage("Expected declared license to be %s but  was %s", expectedLicenseId, license);
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  private static boolean licenseExpressionsEqual(String actual, String expected) {
    if (StringUtils.equals(actual, expected)) {
      return true;
    }
    Set<String> actualMembers = parseLicenseMembers(actual);
    Set<String> expectedMembers = parseLicenseMembers(expected);
    return actualMembers.equals(expectedMembers);
  }

  private static Set<String> parseLicenseMembers(String expression) {
    if (expression == null) {
      return Set.of();
    }
    String trimmed = expression.trim();
    if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    return new TreeSet<>(Arrays.asList(trimmed.split(" AND ")));
  }

  public SpdxPackageAssert hasVulnerabilityCount(Integer expected) {
    isNotNull();
    try {
      Map<String, ExternalRef> vulnMap = SbomSpdxUtils.getVulnerabilitiesForPackage(actual);
      if (!expected.equals(vulnMap.size())) {
        failWithMessage("Expected vulnerability count to be %s but was %s", expected, vulnMap.size());
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxPackageAssert containsVulnerabilities(final String... refIds) {
    isNotNull();
    try {
      Map<String, ExternalRef> vulnMap = SbomSpdxUtils.getVulnerabilitiesForPackage(actual);
      List<String> expectedRefs = Arrays.asList(refIds);
      if (!CollectionUtils.isSubCollection(expectedRefs, vulnMap.keySet())) {
        failWithMessage("Expected vulnerabilities %s to be a sub set of %s", expectedRefs, vulnMap.keySet());
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxPackageAssert containsLicenses(final String... licenseIds) {
    isNotNull();
    try {
      for (String licenseId : licenseIds) {
        String attributionText = actual.getAttributionText().iterator().next();
        if (!StringUtils.contains(attributionText, licenseId)) {
          failWithMessage("Expected license %s to be a sub-set of %s", licenseId, attributionText);
        }
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }
}
