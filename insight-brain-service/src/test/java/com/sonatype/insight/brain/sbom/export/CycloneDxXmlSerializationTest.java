/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.brain.sbom.spdx.ParsedSpdxResult;
import com.sonatype.insight.brain.sbom.spdx.Spdx3VersionHandler;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.license.Expression;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Response;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.State;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.cyclonedx.model.vulnerability.Vulnerability.Source;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CycloneDxXmlSerializationTest
{
  @Test
  public void testMinimalBomWithVulnerability_xml_succeeds() throws GeneratorException {
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    bom.setComponents(Collections.singletonList(comp));

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-1234");
    vuln.setBomRef("CVE-2024-1234-uuid1234");
    vuln.setDescription("Test vulnerability");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    Source ratingSource = new Source();
    ratingSource.setName("NVD");
    rating.setSource(ratingSource);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vulnSource.setUrl("https://nvd.nist.gov/vuln/detail/CVE-2024-1234");
    vuln.setSource(vulnSource);

    bom.setVulnerabilities(newArrayList(vuln));

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-1234");
    assertThat(xml).contains("<vulnerability");
  }

  @Test
  public void testBomWithVulnerabilityAndAnalysis_xml_succeeds() throws GeneratorException {
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    bom.setComponents(Collections.singletonList(comp));

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-1234");
    vuln.setBomRef("CVE-2024-1234-uuid1234");
    vuln.setDescription("Test vulnerability");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    Source ratingSource = new Source();
    ratingSource.setName("NVD");
    rating.setSource(ratingSource);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vulnSource.setUrl("https://nvd.nist.gov/vuln/detail/CVE-2024-1234");
    vuln.setSource(vulnSource);

    // Add Analysis with firstIssued/lastUpdated (the difference from scan-based path)
    Analysis analysis = new Analysis();
    analysis.setState(State.EXPLOITABLE);
    analysis.setDetail("Detailed analysis text");
    analysis.setFirstIssued(new Date());
    analysis.setLastUpdated(new Date());
    analysis.setResponses(Arrays.asList(Response.CAN_NOT_FIX));
    vuln.setAnalysis(analysis);

    bom.setVulnerabilities(newArrayList(vuln));

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-1234");
    assertThat(xml).contains("<analysis>");
  }

  @Test
  public void testBomWithVulnerabilityAndCwes_xml_succeeds() throws GeneratorException {
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    bom.setComponents(Collections.singletonList(comp));

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-1234");
    vuln.setBomRef("CVE-2024-1234-uuid1234");
    vuln.setDescription("Test vulnerability");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vuln.setSource(vulnSource);

    // CWEs
    vuln.setCwes(Arrays.asList(79, 89));

    // Properties
    vuln.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(
        vuln.getProperties(), "sonatype:identification-sources", "sonatype"));

    bom.setVulnerabilities(newArrayList(vuln));

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-1234");
    assertThat(xml).contains("<cwe>");
  }

  @Test
  public void testBomWithAllVulnerabilityFields_xml_succeeds() throws GeneratorException {
    // This reproduces what Spdx3ToCycloneDxExporter does: fresh Bom + mergeCurrentDatabaseState
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    comp.setPurl("pkg:maven/org.example/test-component@1.0.0");
    bom.setComponents(Collections.singletonList(comp));

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-1234");
    vuln.setBomRef("CVE-2024-1234-uuid1234");
    vuln.setDescription("Test vulnerability description");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    rating.setVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N");
    Source ratingSource = new Source();
    ratingSource.setName("NVD");
    ratingSource.setUrl("https://nvd.nist.gov/vuln/detail/CVE-2024-1234");
    rating.setSource(ratingSource);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vulnSource.setUrl("https://nvd.nist.gov/vuln/detail/CVE-2024-1234");
    vuln.setSource(vulnSource);

    vuln.setCwes(Arrays.asList(79, 89, 94));

    Analysis analysis = new Analysis();
    analysis.setState(State.EXPLOITABLE);
    analysis.setDetail("VEX detail text");
    analysis.setFirstIssued(new Date());
    analysis.setLastUpdated(new Date());
    analysis.setResponses(Arrays.asList(Response.CAN_NOT_FIX, Response.WORKAROUND_AVAILABLE));
    vuln.setAnalysis(analysis);

    vuln.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(
        vuln.getProperties(), "sonatype:identification-sources", "sonatype"));

    List<Vulnerability> vulnList = new ArrayList<>();
    vulnList.add(vuln);
    bom.setVulnerabilities(vulnList);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-1234");
    assertThat(xml).contains("<vulnerability");
    assertThat(xml).contains("<analysis>");
    assertThat(xml).contains("<cwe>");
  }

  @Test
  public void testFullMergeFlow_withMetadataAndToolInformation_xml_succeeds() throws GeneratorException {
    // Replicate what mergeCurrentDatabaseState creates: Metadata + ToolInformation + bomComponent
    Bom bom = new Bom();

    // Components with properties (like mergeCurrentDatabaseState adds)
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    comp.setPurl("pkg:maven/org.example/test-component@1.0.0");

    // Add properties like mergeCurrentDatabaseState does
    List<Property> compProps = new ArrayList<>();
    Property matchState = new Property();
    matchState.setName("cdx:sonatype:matchState");
    matchState.setValue("exact");
    compProps.add(matchState);
    Property idSources = new Property();
    idSources.setName("cdx:sonatype:identificationSources");
    idSources.setValue("sonatype");
    compProps.add(idSources);
    Property sha1 = new Property();
    sha1.setName("cdx:sonatype:sha1");
    sha1.setValue("abc123def456");
    compProps.add(sha1);
    Property origPurl = new Property();
    origPurl.setName("cdx:sonatype:originalPurl");
    origPurl.setValue("pkg:maven/org.example/test-component@1.0.0");
    compProps.add(origPurl);
    comp.setProperties(compProps);

    bom.setComponents(Collections.singletonList(comp));

    // Add Metadata with ToolInformation (as generateNewBomMetadata does)
    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());

    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);

    // BOM component in metadata
    Component bomComp = new Component();
    bomComp.setType(Component.Type.APPLICATION);
    bomComp.setName("test-app");
    bomComp.setVersion("v1");
    bomComp.setBomRef(UUID.randomUUID().toString());
    metadata.setComponent(bomComp);

    // Metadata properties
    Property metaProp = new Property();
    metaProp.setName("cdx:sonatype:originalFileName");
    metaProp.setValue("test.spdx.json");
    metadata.setProperties(Collections.singletonList(metaProp));

    bom.setMetadata(metadata);

    // Add dependencies
    Dependency rootDep = new Dependency(bomComp.getBomRef());
    Dependency childDep = new Dependency("comp-ref-1");
    rootDep.setDependencies(Collections.singletonList(childDep));
    bom.setDependencies(Arrays.asList(rootDep, childDep));

    // Vulnerabilities from DB (same as before)
    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-1234");
    vuln.setBomRef("CVE-2024-1234-" + UUID.randomUUID());
    vuln.setDescription("Test vulnerability description");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    rating.setVector("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N");
    Source ratingSource = new Source();
    ratingSource.setName("NVD");
    ratingSource.setUrl("https://nvd.nist.gov/vuln/detail/CVE-2024-1234");
    rating.setSource(ratingSource);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vulnSource.setUrl("https://nvd.nist.gov/vuln/detail/CVE-2024-1234");
    vuln.setSource(vulnSource);

    vuln.setCwes(Arrays.asList(79, 89));

    Analysis analysis = new Analysis();
    analysis.setState(State.EXPLOITABLE);
    analysis.setDetail("VEX detail text");
    analysis.setFirstIssued(new Date());
    analysis.setLastUpdated(new Date());
    analysis.setResponses(Arrays.asList(Response.CAN_NOT_FIX));
    vuln.setAnalysis(analysis);

    vuln.setProperties(SbomExportUtils.addOrUpdateBomElementProperty(
        vuln.getProperties(), "sonatype:identification-sources", "sonatype"));

    List<Vulnerability> vulnList = new ArrayList<>();
    vulnList.add(vuln);
    bom.setVulnerabilities(vulnList);

    // Ensure null bom-level properties don't break (as mergeCurrentDatabaseState sets to null if empty)
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-1234");
    assertThat(xml).contains("<metadata>");
    assertThat(xml).contains("<vulnerability");
    assertThat(xml).contains("<analysis>");
  }

  @Test
  public void testFullMergeFlow_withLicenseExpression_xml_succeeds() throws GeneratorException {
    // Test with LicenseChoice expression on components (as mergeCurrentDatabaseState can set)
    Bom bom = new Bom();

    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    comp.setPurl("pkg:maven/org.example/test-component@1.0.0");

    // Add license expression
    LicenseChoice licenseChoice = new LicenseChoice();
    Expression expression = new Expression();
    expression.setValue("Apache-2.0 OR MIT");
    licenseChoice.setExpression(expression);
    comp.setLicenses(licenseChoice);

    bom.setComponents(Collections.singletonList(comp));

    // Add Metadata
    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    Component bomComp = new Component();
    bomComp.setType(Component.Type.APPLICATION);
    bomComp.setName("test-app");
    bomComp.setVersion("v1");
    bomComp.setBomRef(UUID.randomUUID().toString());
    metadata.setComponent(bomComp);
    bom.setMetadata(metadata);

    // Vulnerabilities
    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-5678");
    vuln.setBomRef("CVE-2024-5678-" + UUID.randomUUID());
    vuln.setDescription("Another test vulnerability");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.CRITICAL);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(9.8);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vuln.setSource(vulnSource);

    List<Vulnerability> vulnList = new ArrayList<>();
    vulnList.add(vuln);
    bom.setVulnerabilities(vulnList);

    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-5678");
    assertThat(xml).contains("<metadata>");
    assertThat(xml).contains("<vulnerability");
  }

  @Test
  public void testFullMergeFlow_version15_xml_succeeds() throws GeneratorException {
    // Same as fullMergeFlow but with VERSION_15 (used when exporting CycloneDX 1.5)
    Bom bom = new Bom();

    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    comp.setPurl("pkg:maven/org.example/test-component@1.0.0");

    List<Property> compProps = new ArrayList<>();
    Property matchState = new Property();
    matchState.setName("cdx:sonatype:matchState");
    matchState.setValue("exact");
    compProps.add(matchState);
    comp.setProperties(compProps);

    bom.setComponents(Collections.singletonList(comp));

    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    Component bomComp = new Component();
    bomComp.setType(Component.Type.APPLICATION);
    bomComp.setName("test-app");
    bomComp.setVersion("v1");
    bomComp.setBomRef(UUID.randomUUID().toString());
    metadata.setComponent(bomComp);
    bom.setMetadata(metadata);

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-1234");
    vuln.setBomRef("CVE-2024-1234-" + UUID.randomUUID());
    vuln.setDescription("Test vulnerability");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vuln.setSource(vulnSource);

    Analysis analysis = new Analysis();
    analysis.setState(State.EXPLOITABLE);
    analysis.setDetail("Detail");
    analysis.setFirstIssued(new Date());
    analysis.setLastUpdated(new Date());
    analysis.setResponses(Arrays.asList(Response.CAN_NOT_FIX));
    vuln.setAnalysis(analysis);

    bom.setVulnerabilities(newArrayList(vuln));
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_15, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-1234");
    assertThat(xml).contains("<vulnerability");
  }

  @Test
  public void testVulnerabilityWithNullBomRef_xml_succeeds() throws GeneratorException {
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    bom.setComponents(Collections.singletonList(comp));

    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    bom.setMetadata(metadata);

    // Vulnerability WITHOUT bomRef (null)
    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-0001");
    // bomRef intentionally not set (null)
    vuln.setDescription("Vuln without bomRef");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    vuln.setRatings(Collections.singletonList(rating));

    bom.setVulnerabilities(newArrayList(vuln));
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-0001");
  }

  @Test
  public void testVulnerabilityWithEmptyResponses_xml_succeeds() throws GeneratorException {
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    bom.setComponents(Collections.singletonList(comp));

    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    bom.setMetadata(metadata);

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-0002");
    vuln.setBomRef("CVE-2024-0002-ref");

    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    vuln.setRatings(Collections.singletonList(rating));

    // Analysis with EMPTY responses list
    Analysis analysis = new Analysis();
    analysis.setState(State.EXPLOITABLE);
    analysis.setDetail("Detail");
    analysis.setResponses(new ArrayList<>());
    vuln.setAnalysis(analysis);

    bom.setVulnerabilities(newArrayList(vuln));
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-0002");
  }

  @Test
  public void testMultipleVulnerabilitiesWithMixedFields_xml_succeeds() throws GeneratorException {
    Bom bom = new Bom();
    Component comp1 = new Component();
    comp1.setType(Component.Type.LIBRARY);
    comp1.setName("comp-a");
    comp1.setVersion("1.0.0");
    comp1.setBomRef("comp-ref-a");
    comp1.setPurl("pkg:maven/org.example/comp-a@1.0.0");

    Component comp2 = new Component();
    comp2.setType(Component.Type.LIBRARY);
    comp2.setName("comp-b");
    comp2.setVersion("2.0.0");
    comp2.setBomRef("comp-ref-b");
    comp2.setPurl("pkg:maven/org.example/comp-b@2.0.0");

    bom.setComponents(Arrays.asList(comp1, comp2));

    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    Component bomComp = new Component();
    bomComp.setType(Component.Type.APPLICATION);
    bomComp.setName("app");
    bomComp.setVersion("v1");
    bomComp.setBomRef(UUID.randomUUID().toString());
    metadata.setComponent(bomComp);
    bom.setMetadata(metadata);

    // Multiple vulnerabilities affecting different components
    List<Vulnerability> vulns = new ArrayList<>();

    // Vuln 1: full data
    Vulnerability vuln1 = new Vulnerability();
    vuln1.setId("CVE-2024-1111");
    vuln1.setBomRef("CVE-2024-1111-" + UUID.randomUUID());
    vuln1.setDescription("First vuln");
    Affect affect1 = new Affect();
    affect1.setRef("comp-ref-a");
    vuln1.setAffects(newArrayList(affect1));
    Rating r1 = new Rating();
    r1.setSeverity(Severity.HIGH);
    r1.setMethod(Method.CVSSV3);
    r1.setScore(7.5);
    Source rs1 = new Source();
    rs1.setName("NVD");
    r1.setSource(rs1);
    vuln1.setRatings(Collections.singletonList(r1));
    Source vs1 = new Source();
    vs1.setName("NVD");
    vs1.setUrl("https://nvd.nist.gov/vuln/detail/CVE-2024-1111");
    vuln1.setSource(vs1);
    vuln1.setCwes(Arrays.asList(79));
    Analysis a1 = new Analysis();
    a1.setState(State.EXPLOITABLE);
    a1.setFirstIssued(new Date());
    a1.setLastUpdated(new Date());
    a1.setResponses(Arrays.asList(Response.CAN_NOT_FIX));
    vuln1.setAnalysis(a1);
    vulns.add(vuln1);

    // Vuln 2: minimal data - no analysis, no cwes, no source url
    Vulnerability vuln2 = new Vulnerability();
    vuln2.setId("CVE-2024-2222");
    vuln2.setBomRef("CVE-2024-2222-" + UUID.randomUUID());
    Affect affect2 = new Affect();
    affect2.setRef("comp-ref-b");
    vuln2.setAffects(newArrayList(affect2));
    Rating r2 = new Rating();
    r2.setSeverity(Severity.MEDIUM);
    r2.setMethod(Method.CVSSV3);
    r2.setScore(5.0);
    vuln2.setRatings(Collections.singletonList(r2));
    Source vs2 = new Source();
    vs2.setName("NVD");
    vuln2.setSource(vs2);
    vulns.add(vuln2);

    // Vuln 3: same component as vuln1 (multiple vulns per component)
    Vulnerability vuln3 = new Vulnerability();
    vuln3.setId("CVE-2024-3333");
    vuln3.setBomRef("CVE-2024-3333-" + UUID.randomUUID());
    vuln3.setDescription("Third vuln on same component");
    Affect affect3 = new Affect();
    affect3.setRef("comp-ref-a");
    vuln3.setAffects(newArrayList(affect3));
    Rating r3 = new Rating();
    r3.setSeverity(Severity.CRITICAL);
    r3.setMethod(Method.CVSSV3);
    r3.setScore(9.8);
    vuln3.setRatings(Collections.singletonList(r3));
    Source vs3 = new Source();
    vs3.setName("NVD");
    vuln3.setSource(vs3);
    Analysis a3 = new Analysis();
    a3.setState(State.IN_TRIAGE);
    vuln3.setAnalysis(a3);
    vulns.add(vuln3);

    bom.setVulnerabilities(vulns);
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-1111");
    assertThat(xml).contains("CVE-2024-2222");
    assertThat(xml).contains("CVE-2024-3333");
  }

  @Test
  public void testBomWithSpdxUriAsBomRef_xml_succeeds() throws GeneratorException {
    // SPDX 3.0 handler sets bomRef to the SPDX objectUri which is a full URI
    Bom bom = new Bom();

    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("https://spdx.org/spdxdocs/urn:uuid:12345#SPDXRef-Package-org.example-test-component");
    comp.setPurl("pkg:maven/org.example/test-component@1.0.0");
    bom.setComponents(Collections.singletonList(comp));

    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    Component bomComp = new Component();
    bomComp.setType(Component.Type.APPLICATION);
    bomComp.setName("test-app");
    bomComp.setVersion("v1");
    bomComp.setBomRef(UUID.randomUUID().toString());
    metadata.setComponent(bomComp);
    bom.setMetadata(metadata);

    // Vulnerability affecting the SPDX-URI component
    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-9999");
    vuln.setBomRef("CVE-2024-9999-" + UUID.randomUUID());
    vuln.setDescription("Vulnerability on SPDX-URI ref component");

    Affect affect = new Affect();
    affect.setRef("https://spdx.org/spdxdocs/urn:uuid:12345#SPDXRef-Package-org.example-test-component");
    vuln.setAffects(newArrayList(affect));

    Rating rating = new Rating();
    rating.setSeverity(Severity.MEDIUM);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(5.5);
    vuln.setRatings(Collections.singletonList(rating));

    Source vulnSource = new Source();
    vulnSource.setName("NVD");
    vuln.setSource(vulnSource);

    Analysis analysis = new Analysis();
    analysis.setState(State.EXPLOITABLE);
    analysis.setDetail("Detail");
    analysis.setFirstIssued(new Date());
    analysis.setLastUpdated(new Date());
    vuln.setAnalysis(analysis);

    bom.setVulnerabilities(newArrayList(vuln));
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-9999");
    assertThat(xml).contains("<vulnerability");
    assertThat(xml).contains("<analysis>");
  }

  @Test
  public void testParsedSpdx3File_toCycloneDxXml_succeeds() throws Exception {
    // Parse a real SPDX 3.0 file and build a CycloneDX BOM from it, then serialize to XML.
    // This replicates the Spdx3ToCycloneDxExporter flow without DB interaction.
    Spdx3VersionHandler handler = new Spdx3VersionHandler();
    String content;
    try (InputStream is = getClass().getResourceAsStream("/sbom/spdx3/software-profile-spdx-3.0.spdx.json")) {
      content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    ParsedSpdxResult parsed = handler.parse(content, SbomFormat.JSON);

    // Build CycloneDX BOM from parsed result (same as Spdx3ToCycloneDxExporter.buildCycloneDxBom)
    Bom bom = new Bom();
    List<Component> components = parsed.resolvedComponents()
        .stream()
        .map(Pair::getRight)
        .collect(Collectors.toList());
    bom.setComponents(components);
    bom.setDependencies(parsed.dependencies());

    // Add metadata like mergeCurrentDatabaseState does
    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    Component bomComp = new Component();
    bomComp.setType(Component.Type.APPLICATION);
    bomComp.setName("test-app");
    bomComp.setVersion("v1");
    bomComp.setBomRef(UUID.randomUUID().toString());
    metadata.setComponent(bomComp);
    bom.setMetadata(metadata);

    // Add a vulnerability (simulating what mergeCurrentDatabaseState adds from DB)
    if (!components.isEmpty()) {
      Vulnerability vuln = new Vulnerability();
      vuln.setId("CVE-2024-TEST");
      vuln.setBomRef("CVE-2024-TEST-" + UUID.randomUUID());
      vuln.setDescription("Test vuln on parsed component");

      Affect affect = new Affect();
      affect.setRef(components.get(0).getBomRef());
      vuln.setAffects(newArrayList(affect));

      Rating rating = new Rating();
      rating.setSeverity(Severity.HIGH);
      rating.setMethod(Method.CVSSV3);
      rating.setScore(7.5);
      vuln.setRatings(Collections.singletonList(rating));

      Source vulnSource = new Source();
      vulnSource.setName("NVD");
      vuln.setSource(vulnSource);

      Analysis analysis = new Analysis();
      analysis.setState(State.EXPLOITABLE);
      analysis.setFirstIssued(new Date());
      analysis.setLastUpdated(new Date());
      analysis.setResponses(Arrays.asList(Response.CAN_NOT_FIX));
      vuln.setAnalysis(analysis);

      bom.setVulnerabilities(newArrayList(vuln));
    }

    bom.setProperties(null);

    // This is the exact call that fails in production for SPDX 3.0 → CycloneDX XML
    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-TEST");
    assertThat(xml).contains("<vulnerability");

    // Also test VERSION_15
    String xml15 = BomGeneratorFactory.createXml(Version.VERSION_15, bom).toXmlString();
    assertThat(xml15).contains("CVE-2024-TEST");
  }

  @Test
  public void testParsedSpdx3VexFile_noVulnerabilities_xml_succeeds() throws Exception {
    // Parse the VEX file but DON'T add vulnerabilities - isolate parsed components
    Spdx3VersionHandler handler = new Spdx3VersionHandler();
    String content;
    try (InputStream is = getClass().getResourceAsStream("/sbom/spdx3/security-vex-spdx-3.0.spdx.json")) {
      content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    ParsedSpdxResult parsed = handler.parse(content, SbomFormat.JSON);

    Bom bom = new Bom();
    List<Component> components = parsed.resolvedComponents()
        .stream()
        .map(Pair::getRight)
        .collect(Collectors.toList());
    bom.setComponents(components);
    bom.setDependencies(parsed.dependencies());

    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    bom.setMetadata(metadata);

    // NO vulnerabilities
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("jackson-databind");
  }

  @Test
  public void testParsedSpdx3VexFile_minimalVuln_withGuard_succeeds() throws Exception {
    // Verifies the fix: skip setDependencies when parsed list is empty
    Spdx3VersionHandler handler = new Spdx3VersionHandler();
    String content;
    try (InputStream is = getClass().getResourceAsStream("/sbom/spdx3/security-vex-spdx-3.0.spdx.json")) {
      content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    ParsedSpdxResult parsed = handler.parse(content, SbomFormat.JSON);

    Bom bom = new Bom();
    List<Component> components = parsed.resolvedComponents()
        .stream()
        .map(Pair::getRight)
        .collect(Collectors.toList());
    bom.setComponents(components);
    // Apply same guard as Spdx3ToCycloneDxExporter.buildCycloneDxBom()
    List<Dependency> dependencies = parsed.dependencies();
    if (dependencies != null && !dependencies.isEmpty()) {
      bom.setDependencies(dependencies);
    }

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-TEST");
    vuln.setBomRef("CVE-TEST-ref");
    Affect affect = new Affect();
    affect.setRef(components.get(0).getBomRef());
    vuln.setAffects(newArrayList(affect));
    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    vuln.setRatings(Collections.singletonList(rating));
    Source s = new Source();
    s.setName("NVD");
    vuln.setSource(s);
    bom.setVulnerabilities(newArrayList(vuln));
    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-TEST");
  }

  @Test
  public void testEmptyDependenciesListWithVulnerability_xml_failsWithKnownLibraryBug() throws GeneratorException {
    // Known CycloneDX core-java bug: empty DependencyList corrupts XML writer state before vulnerabilities
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    bom.setComponents(Collections.singletonList(comp));

    // Set EMPTY dependencies list (not null!) — triggers the bug
    bom.setDependencies(new ArrayList<>());

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-EMPTY-DEPS");
    vuln.setBomRef("CVE-2024-EMPTY-DEPS-ref");
    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));
    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    vuln.setRatings(Collections.singletonList(rating));
    Source s = new Source();
    s.setName("NVD");
    vuln.setSource(s);
    bom.setVulnerabilities(newArrayList(vuln));

    assertThrows(GeneratorException.class, () -> BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString());
  }

  @Test
  public void testNullDependenciesWithVulnerability_xml_succeeds() throws GeneratorException {
    // Workaround: leave dependencies null (don't set empty list) when there are none
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("test-component");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    bom.setComponents(Collections.singletonList(comp));

    // dependencies left as null — XML serialization works fine

    Vulnerability vuln = new Vulnerability();
    vuln.setId("CVE-2024-NULL-DEPS");
    vuln.setBomRef("CVE-2024-NULL-DEPS-ref");
    Affect affect = new Affect();
    affect.setRef("comp-ref-1");
    vuln.setAffects(newArrayList(affect));
    Rating rating = new Rating();
    rating.setSeverity(Severity.HIGH);
    rating.setMethod(Method.CVSSV3);
    rating.setScore(7.5);
    vuln.setRatings(Collections.singletonList(rating));
    Source s = new Source();
    s.setName("NVD");
    vuln.setSource(s);
    bom.setVulnerabilities(newArrayList(vuln));

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).contains("CVE-2024-NULL-DEPS");
  }

  @Test
  public void testParsedSpdx3VexFile_toCycloneDxXml_succeeds() throws Exception {
    // Parse the security/VEX SPDX 3.0 file (has vulnerabilities embedded)
    Spdx3VersionHandler handler = new Spdx3VersionHandler();
    String content;
    try (InputStream is = getClass().getResourceAsStream("/sbom/spdx3/security-vex-spdx-3.0.spdx.json")) {
      content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    ParsedSpdxResult parsed = handler.parse(content, SbomFormat.JSON);

    Bom bom = new Bom();
    List<Component> components = parsed.resolvedComponents()
        .stream()
        .map(Pair::getRight)
        .collect(Collectors.toList());
    bom.setComponents(components);
    // Apply same guard as Spdx3ToCycloneDxExporter.buildCycloneDxBom()
    List<Dependency> dependencies = parsed.dependencies();
    if (dependencies != null && !dependencies.isEmpty()) {
      bom.setDependencies(dependencies);
    }

    Metadata metadata = new Metadata();
    metadata.setTimestamp(new Date());
    ToolInformation toolInfo = new ToolInformation();
    Component toolComponent = new Component();
    toolComponent.setType(Component.Type.APPLICATION);
    toolComponent.setName("Sonatype SBOM Manager");
    toolComponent.setVersion("1.0.0-test");
    toolInfo.setComponents(Collections.singletonList(toolComponent));
    metadata.setToolChoice(toolInfo);
    Component bomComp = new Component();
    bomComp.setType(Component.Type.APPLICATION);
    bomComp.setName("test-app");
    bomComp.setVersion("v1");
    bomComp.setBomRef(UUID.randomUUID().toString());
    metadata.setComponent(bomComp);
    bom.setMetadata(metadata);

    // Add vulnerabilities from DB sim
    if (!components.isEmpty()) {
      List<Vulnerability> vulns = new ArrayList<>();
      for (int i = 0; i < Math.min(3, components.size()); i++) {
        Vulnerability vuln = new Vulnerability();
        vuln.setId("CVE-2024-" + (1000 + i));
        vuln.setBomRef("CVE-2024-" + (1000 + i) + "-" + UUID.randomUUID());
        vuln.setDescription("Vuln " + i);
        Affect affect = new Affect();
        affect.setRef(components.get(i).getBomRef());
        vuln.setAffects(newArrayList(affect));
        Rating rating = new Rating();
        rating.setSeverity(Severity.HIGH);
        rating.setMethod(Method.CVSSV3);
        rating.setScore(7.5 + i * 0.5);
        vuln.setRatings(Collections.singletonList(rating));
        Source vs = new Source();
        vs.setName("NVD");
        vuln.setSource(vs);
        if (i == 0) {
          Analysis a = new Analysis();
          a.setState(State.EXPLOITABLE);
          a.setFirstIssued(new Date());
          a.setLastUpdated(new Date());
          a.setResponses(Arrays.asList(Response.CAN_NOT_FIX));
          vuln.setAnalysis(a);
        }
        vulns.add(vuln);
      }
      bom.setVulnerabilities(vulns);
    }

    bom.setProperties(null);

    String xml = BomGeneratorFactory.createXml(Version.VERSION_16, bom).toXmlString();
    assertThat(xml).isNotEmpty();
    assertThat(xml).contains("<vulnerability");
  }

  /**
   * Reproduces the round-trip failure via the real export path: parse a CycloneDX 1.7 JSON whose
   * spring-web license carries both a {@code url} and a {@code licensing} block, then serialize to
   * XML. The stock generator emits {@code <licensing>} before {@code <url>}, violating the 1.7 XSD
   * (licenseType sequence is id/name, text, url, licensing, properties) so the file cannot be
   * re-imported. {@link CycloneDxSchemaOrderedXmlGenerator} must emit schema-valid XML.
   */
  @Test
  public void testParsedBomWithUrlAndLicensing_schemaOrderedGeneratorProducesValidXml() throws Exception {
    Bom bom;
    try (InputStream is = getClass().getResourceAsStream("/SbomRegressionTest/originals/cyclonedx_1.7.json")) {
      bom = SbomCycloneDxUtils.parseContentStreamNoValidation(is);
    }

    // Stock generator reproduces the bug: the emitted XML fails validation against the 1.7 XSD
    // because the license <licensing> element is serialized before <url>, so it cannot be re-imported.
    String stockXml = BomGeneratorFactory.createXml(Version.VERSION_17, bom).toXmlString();
    List<org.cyclonedx.exception.ParseException> stockErrors =
        new org.cyclonedx.parsers.XmlParser().validate(stockXml.getBytes(StandardCharsets.UTF_8), Version.VERSION_17);
    assertThat(stockErrors).isNotEmpty();

    // Fixed generator: the license retains its <licensing> block and the document validates cleanly.
    String fixedXml = new CycloneDxSchemaOrderedXmlGenerator(bom, Version.VERSION_17).toXmlString();
    assertThat(fixedXml).contains("<licensing>");
    List<org.cyclonedx.exception.ParseException> fixedErrors =
        new org.cyclonedx.parsers.XmlParser().validate(fixedXml.getBytes(StandardCharsets.UTF_8), Version.VERSION_17);
    assertThat(fixedErrors).isEmpty();
  }

  /**
   * A license carrying {@code text} (attachment) plus a {@code licensing} block must also be
   * reordered: the XSD requires {@code text} before {@code licensing}.
   */
  @Test
  public void testLicenseWithTextAndLicensing_schemaOrderedGeneratorProducesValidXml() throws Exception {
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("lib-with-text-license");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    comp.setPurl("pkg:maven/org.example/lib-with-text-license@1.0.0");

    org.cyclonedx.model.License license = new org.cyclonedx.model.License();
    license.setName("Custom-EULA");
    org.cyclonedx.model.AttachmentText text = new org.cyclonedx.model.AttachmentText();
    text.setContentType("text/plain");
    text.setText("Full custom license text.");
    license.setLicenseText(text);
    org.cyclonedx.model.Licensing licensing = new org.cyclonedx.model.Licensing();
    licensing.setExpiration(new Date());
    license.setLicensing(licensing);

    LicenseChoice licenseChoice = new LicenseChoice();
    licenseChoice.addLicense(license);
    comp.setLicenses(licenseChoice);
    bom.setComponents(Collections.singletonList(comp));

    String fixedXml = new CycloneDxSchemaOrderedXmlGenerator(bom, Version.VERSION_17).toXmlString();
    assertThat(fixedXml).contains("<licensing>");
    assertThat(fixedXml).contains("Custom-EULA");
    List<org.cyclonedx.exception.ParseException> errors =
        new org.cyclonedx.parsers.XmlParser().validate(fixedXml.getBytes(StandardCharsets.UTF_8), Version.VERSION_17);
    assertThat(errors).isEmpty();
  }

  /**
   * Safety: for licenses that have no {@code licensing} block (the common case), the schema-ordered
   * generator must leave output byte-for-byte identical to the stock generator (fast-path), and the
   * output must validate.
   */
  @Test
  public void testLicenseWithoutLicensing_outputUnchangedAndValid() throws Exception {
    Bom bom = new Bom();
    Component comp = new Component();
    comp.setType(Component.Type.LIBRARY);
    comp.setName("plain-lib");
    comp.setVersion("1.0.0");
    comp.setBomRef("comp-ref-1");
    comp.setPurl("pkg:maven/org.example/plain-lib@1.0.0");

    org.cyclonedx.model.License license = new org.cyclonedx.model.License();
    license.setId("Apache-2.0");
    license.setUrl("https://www.apache.org/licenses/LICENSE-2.0.txt");
    LicenseChoice licenseChoice = new LicenseChoice();
    licenseChoice.addLicense(license);
    comp.setLicenses(licenseChoice);
    bom.setComponents(Collections.singletonList(comp));

    String stockXml = BomGeneratorFactory.createXml(Version.VERSION_17, bom).toXmlString();
    String fixedXml = new CycloneDxSchemaOrderedXmlGenerator(bom, Version.VERSION_17).toXmlString();
    assertThat(fixedXml).isEqualTo(stockXml);
    List<org.cyclonedx.exception.ParseException> errors =
        new org.cyclonedx.parsers.XmlParser().validate(fixedXml.getBytes(StandardCharsets.UTF_8), Version.VERSION_17);
    assertThat(errors).isEmpty();
  }
}
