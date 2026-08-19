/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.AbstractAssert;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.vulnerability.Vulnerability;

public class CycloneDxComponentAssert
    extends AbstractAssert<CycloneDxComponentAssert, Component>
{
  protected CycloneDxComponentAssert(final Component component, final Class<?> selfType) {
    super(component, selfType);
  }

  public CycloneDxComponentAssert hasVulnerabilityCount(Bom bom, Integer expected) {
    isNotNull();
    List<Vulnerability> vulnerabilities = bom.getVulnerabilities()
        .stream()
        .filter(it -> it.getAffects()
            .stream()
            .anyMatch(affect -> affect.getRef().equals(actual.getBomRef())))
        .collect(Collectors.toList());
    if (!expected.equals(vulnerabilities.size())) {
      failWithMessage("Expected vulnerability count to be %s but was %s", expected, vulnerabilities.size());
    }
    return this;
  }

  public CycloneDxComponentAssert hasLicenseCount(Integer expected) {
    isNotNull();
    if (actual.getLicenses() == null) {
      failWithMessage("Expected license choice to be non-null");
    }
    if (CollectionUtils.isEmpty(actual.getLicenses().getLicenses())) {
      failWithMessage("Expected license choice to be not empty");
    }
    if (!expected.equals(actual.getLicenses().getLicenses().size())) {
      failWithMessage("Expected license choice count to be %s but was %s", expected,
          actual.getLicenses().getLicenses().size());
    }
    return this;
  }

  public CycloneDxComponentAssert containsVulnerabilities(Bom bom, final String... refIds) {
    isNotNull();
    List<String> vulnerabilities = bom.getVulnerabilities()
        .stream()
        .filter(it -> it.getAffects()
            .stream()
            .anyMatch(affect -> affect.getRef().equals(actual.getBomRef())))
        .map(Vulnerability::getId)
        .collect(Collectors.toList());
    List<String> expectedRefs = Arrays.asList(refIds);
    if (!CollectionUtils.isSubCollection(expectedRefs, vulnerabilities)) {
      failWithMessage("Expected vulnerabilities %s to be a sub set of %s", expectedRefs, vulnerabilities);
    }
    return this;
  }

  public CycloneDxComponentAssert containsLicenses(final String... licenseIds) {
    isNotNull();
    List<String> actuals = actual.getLicenses()
        .getLicenses()
        .stream()
        .map(License::getId)
        .collect(Collectors.toList());
    List<String> expected = Arrays.asList(licenseIds);
    if (!CollectionUtils.isSubCollection(expected, actuals)) {
      failWithMessage("Expected licenses %s to be a sub set of %s", expected, actuals);
    }
    return this;
  }

  public CycloneDxComponentAssert containsNotListedLicenses(final String... licenseNames) {
    isNotNull();
    List<String> actuals = actual.getLicenses()
        .getLicenses()
        .stream()
        .map(License::getName)
        .collect(Collectors.toList());
    List<String> expected = Arrays.asList(licenseNames);
    if (!CollectionUtils.isSubCollection(expected, actuals)) {
      failWithMessage("Expected licenses %s to be a sub set of %s", expected, actuals);
    }
    return this;
  }
}
