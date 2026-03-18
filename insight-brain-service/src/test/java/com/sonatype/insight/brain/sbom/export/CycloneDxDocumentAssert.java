/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.apache.commons.lang3.StringUtils;

import static org.assertj.core.api.Assertions.within;

public class CycloneDxDocumentAssert
    extends AbstractAssert<CycloneDxDocumentAssert, Bom>
{
  public CycloneDxDocumentAssert(final Bom bom, final Class<?> selfType) {
    super(bom, selfType);
  }

  public static CycloneDxDocumentAssert assertThatCycloneDx(Bom bom) {
    return new CycloneDxDocumentAssert(bom, CycloneDxDocumentAssert.class);
  }

  public CycloneDxDocumentAssert hasToolCreationInformation(String expectedToolName, String expectedToolVersion) {
    isNotNull();
    if (actual.getMetadata().getToolChoice() == null) {
      failWithMessage("Expected document tool metadata to be present");
    }
    if (CollectionUtils.isEmpty(actual.getMetadata().getToolChoice().getComponents())) {
      failWithMessage("Expected document tool choice components to not be empty");
    }
    Component actualTool = actual.getMetadata().getToolChoice().getComponents().stream().findFirst().get();
    if (!actualTool.getName().equals(expectedToolName) || !actualTool.getVersion().equals(expectedToolVersion)) {
      failWithMessage("Expected document tool choice component to be %s : %s but was %s : %s",
          expectedToolName, expectedToolVersion, actualTool.getName(), actualTool.getVersion());
    }
    return this;
  }

  public CycloneDxDocumentAssert hasComponentDocumentDescribes(String name) {
    isNotNull();
    if (actual.getMetadata().getComponent() == null) {
      failWithMessage("Expected component document describes in metadata to be present");
    }
    if (!actual.getMetadata().getComponent().getName().equals(name)) {
      failWithMessage("Expected component document describes in metadata to be %s but was %s", name,
          actual.getMetadata().getComponent().getName());
    }
    return this;
  }

  public CycloneDxDocumentAssert hasComponentCount(int expectedComponents) {
    isNotNull();
    List<Component> components = actual.getComponents();
    int actualSize = CollectionUtils.size(components);
    if (actualSize != expectedComponents) {
      failWithMessage("Expected document to have %s number of components but was %s", expectedComponents,
          actualSize);
    }
    return this;
  }

  public CycloneDxDocumentAssert hasVulnerabilityCount(int expectedVulns) {
    isNotNull();
    List<Vulnerability> vulnerabilities = actual.getVulnerabilities();
    if (vulnerabilities.size() != expectedVulns) {
      failWithMessage("Expected document to have %s number of vulnerabilities but was %s", expectedVulns,
          vulnerabilities.size());
    }
    return this;
  }

  public CycloneDxDocumentAssert equalsSpecVersion(String expectedVersion) {
    isNotNull();
    String specVersion = actual.getSpecVersion();
    if (!StringUtils.contains(specVersion, expectedVersion)) {
      failWithMessage("Expected document version to be %s but was %s", expectedVersion, specVersion);
    }
    return this;
  }

  public CycloneDxDocumentAssert creationDateCloseTo(LocalDateTime other) {
    isNotNull();
    LocalDateTime actualDate = actual.getMetadata()
        .getTimestamp()
        .toInstant()
        .atZone(ZoneOffset.UTC)
        .toLocalDateTime();
    Assertions.assertThat(actualDate).isCloseTo(other, within(1, ChronoUnit.MINUTES));
    return this;
  }

  public CycloneDxDocumentAssert hasPackagesWithPurls(final String... purls) {
    isNotNull();
    Set<String> actualPurls = getAllComponentPurls();
    List<String> expected = Arrays.asList(purls);
    if (!CollectionUtils.isSubCollection(expected, actualPurls)) {
      failWithMessage("Expected %s to be sub-set of %s", expected, actualPurls);
    }
    return this;
  }

  public CycloneDxComponentAssert hasPackageWithPurl(final String purl) {
    List<Component> components = actual.getComponents();
    Component found = null;
    for (Component component : components) {
      if (StringUtils.equals(component.getPurl(), purl)) {
        found = component;
        break;
      }
    }
    if (found == null) {
      failWithMessage("Component with purl %s not found", purl);
    }
    return new CycloneDxComponentAssert(found, CycloneDxComponentAssert.class);
  }

  private Set<String> getAllComponentPurls() {
    List<Component> components = actual.getComponents();
    Set<String> actualPurls = new HashSet<>();
    for (Component component : components) {
      String purl = component.getPurl();
      if (purl != null) {
        actualPurls.add(purl);
      }
    }
    return actualPurls;
  }
}
