/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.AbstractAssert;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.ModelObject;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.license.AnyLicenseInfo;
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
      if (!StringUtils.equals(license.getId(), expectedLicenseId)) {
        failWithMessage("Expected concluded license to be %s but  was %s", expectedLicenseId, license.getId());
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
      if (!StringUtils.equals(license.getId(), expectedLicenseId)) {
        failWithMessage("Expected declared license to be %s but  was %s", expectedLicenseId, license.getId());
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
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

  public SpdxPackageAssert hasAllLicensesCount(final Integer expectedCount) {
    isNotNull();
    try {
      Collection<AnyLicenseInfo> licenses = actual.getLicenseInfoFromFiles();
      if (!expectedCount.equals(licenses.size())) {
        failWithMessage("Expected license count to be %s but was %s", expectedCount, licenses.size());
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
      Collection<AnyLicenseInfo> licenses = actual.getLicenseInfoFromFiles();
      Set<String> actuals = licenses.stream().map(ModelObject::getId).collect(Collectors.toSet());
      List<String> expected = Arrays.asList(licenseIds);
      if (!CollectionUtils.isSubCollection(expected, actuals)) {
        failWithMessage("Expected license %s to be a sub-set of %s", expected, actuals);
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }
}
