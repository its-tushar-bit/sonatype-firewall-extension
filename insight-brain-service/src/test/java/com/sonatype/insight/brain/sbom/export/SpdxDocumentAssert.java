/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomValidationException;
import com.sonatype.insight.scan.file.UnsupportedSbomException;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.ExternalRef;
import org.spdx.library.model.SpdxCreatorInformation;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.apache.commons.lang3.StringUtils;

import static org.assertj.core.api.Assertions.within;
import static org.xmlunit.assertj.error.ShouldNotHaveThrown.shouldNotHaveThrown;

public class SpdxDocumentAssert
    extends AbstractAssert<SpdxDocumentAssert, SpdxDocument>
{
  private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd")
      .appendLiteral('T')
      .appendPattern("HH:mm:ss")
      .appendLiteral('Z')
      .toFormatter();

  private final SbomFormat sbomFormat;

  public SpdxDocumentAssert(final SpdxDocument document, final Class<?> selfType) {
    super(document, selfType);
    this.sbomFormat = SbomSpdxUtils.determineSbomFormat(document);
  }

  public static SpdxDocumentAssert assertThatSpdx(SpdxDocument document) {
    return new SpdxDocumentAssert(document, SpdxDocumentAssert.class);
  }

  public SpdxDocumentAssert hasComponentCount(int expectedComponents) {
    isNotNull();
    try {
      List<SpdxPackage> components = getAllComponents();
      int actualSize = CollectionUtils.size(components);
      if (actualSize != expectedComponents) {
        failWithMessage("Expected document to have %s number of components but was %s", expectedComponents,
            actualSize);
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert hasVulnerabilityCount(int expectedVulns) {
    isNotNull();
    try {
      List<ExternalRef> allVulns = SbomSpdxUtils.getAllVulnerabilities(actual);
      if (allVulns.size() != expectedVulns) {
        failWithMessage("Expected document to have %s number of vulnerabilities but was %s", expectedVulns,
            allVulns.size());
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert hasFormat(SbomFormat format) {
    isNotNull();
    if (!this.sbomFormat.equals(format)) {
      failWithMessage("Expected SBOM format to be %s but was %s", format.toString(), this.sbomFormat);
    }
    return this;
  }

  public SpdxDocumentAssert isValid() {
    isNotNull();
    try {
      SbomSpdxUtils.validateDocument(this.sbomFormat, actual);
      return this;
    }
    catch (UnsupportedSbomException | SbomValidationException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert nameContains(String expectedName) {
    isNotNull();
    try {
      Optional<String> actualName = actual.getName();
      if (!actualName.isPresent() || !StringUtils.contains(actualName.get(), expectedName)) {
        failWithMessage("Expected document name to be %s but was %s", expectedName, actual.getName().get());
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert equalsSpecVersion(String expectedVersion) {
    isNotNull();
    try {
      String specVersion = actual.getSpecVersion();
      if (!StringUtils.contains(specVersion, expectedVersion)) {
        failWithMessage("Expected document version to be %s but was %s", expectedVersion, specVersion);
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert equalsDataLicense(String expectedDataLicense) {
    isNotNull();
    try {
      AnyLicenseInfo dataLicense = actual.getDataLicense();
      if (!StringUtils.contains(dataLicense.getId(), expectedDataLicense)) {
        failWithMessage("Expected document version to be %s but was %s", expectedDataLicense, dataLicense.getId());
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert creationDateCloseTo(LocalDateTime other) {
    isNotNull();
    try {
      SpdxCreatorInformation creationInfo = actual.getCreationInfo();
      LocalDateTime actualDate = LocalDateTime.parse(Objects.requireNonNull(creationInfo).getCreated(), DATE_FORMATTER);
      Assertions.assertThat(actualDate).isCloseTo(other, within(1, ChronoUnit.MINUTES));
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert hasPackagesWithPurls(final String... purls) {
    isNotNull();
    try {
      Set<String> actualPurls = getAllComponentPurls();
      List<String> expected = Arrays.asList(purls);
      if (!CollectionUtils.isSubCollection(expected, actualPurls)) {
        failWithMessage("Expected %s to be sub-set of %s", expected, actualPurls);
      }
      return this;
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxPackageAssert hasPackageWithPurl(final String purl) {
    try {
      List<SpdxPackage> components = getAllComponents();
      SpdxPackage found = null;
      for (SpdxPackage component : components) {
        if (StringUtils.equals(SbomSpdxUtils.getPurl(component), purl)) {
          found = component;
          break;
        }
      }
      if (found == null) {
        failWithMessage("Package with purl %s not found", purl);
      }
      return new SpdxPackageAssert(found, SpdxPackageAssert.class);
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
  }

  public SpdxDocumentAssert creatorsContaining(final String expected) {
    try {
      SpdxCreatorInformation creationInfo = actual.getCreationInfo();
      boolean found = false;
      Collection<String> creators = creationInfo.getCreators();
      for (String creator : creators) {
        if (StringUtils.contains(creator, expected)) {
          found = true;
          break;
        }
      }
      if (!found) {
        failWithMessage("Expected creator name %s to be in creators list %s", expected, creators);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      throw assertionError(shouldNotHaveThrown(e));
    }
    return this;
  }

  private Set<String> getAllComponentPurls() throws InvalidSPDXAnalysisException {
    List<SpdxPackage> allPackages = getAllComponents();
    Set<String> actualPurls = new HashSet<>();
    for (SpdxPackage pkg : allPackages) {
      String purl = SbomSpdxUtils.getPurl(pkg);
      if (purl != null) {
        actualPurls.add(purl);
      }
    }
    return actualPurls;
  }

  @Nullable
  private List<SpdxPackage> getAllComponents() throws InvalidSPDXAnalysisException {
    return SbomSpdxUtils.getAllPackages(actual);
  }
}
