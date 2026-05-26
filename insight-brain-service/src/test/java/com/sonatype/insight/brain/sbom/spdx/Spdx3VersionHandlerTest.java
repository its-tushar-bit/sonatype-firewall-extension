/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.spdx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.spdx.library.SpdxModelFactory;

public class Spdx3VersionHandlerTest
{
  private Spdx3VersionHandler handler;

  @Before
  public void setUp() {
    SpdxModelFactory.init();
    handler = new Spdx3VersionHandler();
  }

  @Test
  public void parse_minimalDocument_extractsOnePackage() throws Exception {
    String content = loadFixture("sbom/spdx3/minimal-spdx-3.0.spdx.json");
    ParsedSpdxResult result = handler.parse(content, SbomFormat.JSON);

    assertNotNull(result);
    assertEquals("3.0", result.specVersion());
    assertFalse(result.resolvedComponents().isEmpty());
  }

  @Test
  public void parse_softwareProfile_extractsPackagesAndDependencies() throws Exception {
    String content = loadFixture("sbom/spdx3/software-profile-spdx-3.0.spdx.json");
    ParsedSpdxResult result = handler.parse(content, SbomFormat.JSON);

    assertNotNull(result);
    assertTrue("Expected at least 5 components", result.resolvedComponents().size() >= 5);
  }

  @Test
  public void parse_softwareProfile_allPackagesHaveComponentIdentifier() throws Exception {
    String content = loadFixture("sbom/spdx3/software-profile-spdx-3.0.spdx.json");
    ParsedSpdxResult result = handler.parse(content, SbomFormat.JSON);

    long identifiedCount = result.resolvedComponents()
        .stream()
        .filter(p -> p.getLeft() != null)
        .count();

    assertTrue("Expected all packages to have ComponentIdentifier, but only " + identifiedCount + " did",
        identifiedCount >= 5);
  }

  @Test
  public void ensureMavenTypeQualifier_addsMissingType() {
    assertEquals("pkg:maven/org.apache.commons/commons-lang3@3.14.0?type=jar",
        Spdx3VersionHandler.ensureMavenTypeQualifier("pkg:maven/org.apache.commons/commons-lang3@3.14.0"));
  }

  @Test
  public void ensureMavenTypeQualifier_preservesExistingType() {
    String purl = "pkg:maven/org.apache.commons/commons-lang3@3.14.0?type=pom";
    assertEquals(purl, Spdx3VersionHandler.ensureMavenTypeQualifier(purl));
  }

  @Test
  public void ensureMavenTypeQualifier_appendsToExistingQualifiers() {
    assertEquals("pkg:maven/org.apache.commons/commons-lang3@3.14.0?classifier=sources&type=jar",
        Spdx3VersionHandler.ensureMavenTypeQualifier(
            "pkg:maven/org.apache.commons/commons-lang3@3.14.0?classifier=sources"));
  }

  @Test
  public void ensureMavenTypeQualifier_ignoresNonMavenPurls() {
    String npmPurl = "pkg:npm/@angular/core@16.2.0";
    assertEquals(npmPurl, Spdx3VersionHandler.ensureMavenTypeQualifier(npmPurl));
  }

  @Test
  public void ensureMavenTypeQualifier_handlesNull() {
    assertNull(Spdx3VersionHandler.ensureMavenTypeQualifier(null));
  }

  @Test
  public void parse_securityProfile_extractsVulnerabilities() throws Exception {
    String content = loadFixture("sbom/spdx3/security-vex-spdx-3.0.spdx.json");
    ParsedSpdxResult result = handler.parse(content, SbomFormat.JSON);

    assertNotNull(result);
    assertFalse("Expected vulnerabilities", result.vulnerabilities().isEmpty());
  }

  @Test
  public void parseVex_extractsAllFourVexTypes() throws Exception {
    String content = loadFixture("sbom/spdx3/security-vex-spdx-3.0.spdx.json");
    List<ThirdPartyVulnerabilityExploitabilityExchange> vexList = handler.parseVex(content, SbomFormat.JSON);

    assertTrue("Expected at least 4 VEX entries", vexList.size() >= 4);
    assertTrue(vexList.stream().anyMatch(v -> "affected".equalsIgnoreCase(v.getState())));
    assertTrue(vexList.stream().anyMatch(v -> "not_affected".equalsIgnoreCase(v.getState())));
    assertTrue(vexList.stream().anyMatch(v -> "under_investigation".equalsIgnoreCase(v.getState())));
    assertTrue(vexList.stream().anyMatch(v -> "fixed".equalsIgnoreCase(v.getState())));
  }

  @Test
  public void extractUnsupportedProfileElements_aiProfile_returnsBlob() throws Exception {
    String content = loadFixture("sbom/spdx3/ai-profile-spdx-3.0.spdx.json");
    String blob = handler.extractUnsupportedProfileElements(content, SbomFormat.JSON);

    assertNotNull(blob);
    assertTrue(blob.contains("ai_"));
  }

  @Test
  public void extractUnsupportedProfileElements_noAiProfile_returnsNull() throws Exception {
    String content = loadFixture("sbom/spdx3/minimal-spdx-3.0.spdx.json");
    String blob = handler.extractUnsupportedProfileElements(content, SbomFormat.JSON);

    assertNull(blob);
  }

  @Test
  public void generate_producesValidJsonLd() throws Exception {
    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(),
        List.of(),
        Map.of(),
        List.of(),
        List.of(),
        "test-app",
        "1.0.0",
        "3.0",
        null,
        "https://example.com/iq/sbom/test-app/1.0.0",
        null,
        null);

    byte[] output = handler.generate(context);

    assertNotNull(output);
    String jsonLd = new String(output, StandardCharsets.UTF_8);
    assertTrue(jsonLd.contains("@context"));
    assertTrue(jsonLd.contains("test-app"));
  }

  @Test
  public void generate_withVulnerabilities_includesSecurityElements() throws Exception {
    ThirdPartyFileCoordinate comp = new ThirdPartyFileCoordinate();
    comp.setId("coord-1");
    comp.setName("jackson-databind");
    comp.setVersion("2.16.1");
    comp.setPackageUrl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.16.1");

    ThirdPartyCoordinateSecurity vuln = new ThirdPartyCoordinateSecurity();
    vuln.setId("sec-1");
    vuln.setFileCoordinateId("coord-1");
    vuln.setRefId("CVE-2024-1001");
    vuln.setDescription("Remote code execution vulnerability");

    ThirdPartyVulnerabilityExploitabilityExchange vex = new ThirdPartyVulnerabilityExploitabilityExchange();
    vex.setCoordinateSecurityId("sec-1");
    vex.setRefId("CVE-2024-1001");
    vex.setState("not_affected");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp),
        List.of(vuln),
        Map.of(),
        List.of(vex),
        List.of(),
        "test-app",
        "1.0.0",
        "3.0",
        null,
        "https://example.com/iq/sbom/test-app/1.0.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue("Expected Vulnerability element", jsonLd.contains("security_Vulnerability"));
    assertTrue("Expected CVE identifier", jsonLd.contains("CVE-2024-1001"));
    assertTrue("Expected VexNotAffected relationship",
        jsonLd.contains("security_VexNotAffectedVulnAssessmentRelationship"));
  }

  @Test
  public void generate_withVulnerabilities_defaultsToAffected() throws Exception {
    ThirdPartyFileCoordinate comp = new ThirdPartyFileCoordinate();
    comp.setId("coord-2");
    comp.setName("log4j-core");
    comp.setVersion("2.14.0");

    ThirdPartyCoordinateSecurity vuln = new ThirdPartyCoordinateSecurity();
    vuln.setId("sec-2");
    vuln.setFileCoordinateId("coord-2");
    vuln.setRefId("CVE-2021-44228");
    vuln.setDescription("Log4Shell RCE");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp),
        List.of(vuln),
        Map.of(),
        List.of(),
        List.of(),
        "test-app",
        "2.0.0",
        "3.0",
        null,
        "https://example.com/iq/sbom/test-app/2.0.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue("Expected Vulnerability element", jsonLd.contains("security_Vulnerability"));
    assertTrue("Expected CVE identifier", jsonLd.contains("CVE-2021-44228"));
    assertTrue("Expected VexAffected (default) relationship",
        jsonLd.contains("security_VexAffectedVulnAssessmentRelationship"));
  }

  @Test
  public void generate_withVulnerabilities_roundTripsVulnerabilities() throws Exception {
    ThirdPartyFileCoordinate comp = new ThirdPartyFileCoordinate();
    comp.setId("coord-3");
    comp.setName("spring-core");
    comp.setVersion("5.3.20");

    ThirdPartyCoordinateSecurity vuln1 = new ThirdPartyCoordinateSecurity();
    vuln1.setId("sec-3");
    vuln1.setFileCoordinateId("coord-3");
    vuln1.setRefId("CVE-2022-22965");
    vuln1.setDescription("Spring4Shell");

    ThirdPartyCoordinateSecurity vuln2 = new ThirdPartyCoordinateSecurity();
    vuln2.setId("sec-4");
    vuln2.setFileCoordinateId("coord-3");
    vuln2.setRefId("CVE-2022-22950");
    vuln2.setDescription("DoS via SpEL");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp),
        List.of(vuln1, vuln2),
        Map.of(),
        List.of(),
        List.of(),
        "test-app",
        "3.0.0",
        "3.0",
        null,
        "https://example.com/iq/sbom/test-app/3.0.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    ParsedSpdxResult parsed = handler.parse(jsonLd, SbomFormat.JSON);
    assertEquals(2, parsed.vulnerabilities().size());
    assertTrue(parsed.vulnerabilities().stream().anyMatch(v -> "CVE-2022-22965".equals(v.getRefId())));
    assertTrue(parsed.vulnerabilities().stream().anyMatch(v -> "CVE-2022-22950".equals(v.getRefId())));
  }

  @Test
  public void generate_multiplePackages_roundTripsAllComponents() throws Exception {
    ThirdPartyFileCoordinate comp1 = new ThirdPartyFileCoordinate();
    comp1.setId("coord-sw-1");
    comp1.setName("commons-lang3");
    comp1.setVersion("3.14.0");
    comp1.setPackageUrl("pkg:maven/org.apache.commons/commons-lang3@3.14.0");

    ThirdPartyFileCoordinate comp2 = new ThirdPartyFileCoordinate();
    comp2.setId("coord-sw-2");
    comp2.setName("guava");
    comp2.setVersion("33.0.0-jre");
    comp2.setPackageUrl("pkg:maven/com.google.guava/guava@33.0.0-jre");

    ThirdPartyFileCoordinate comp3 = new ThirdPartyFileCoordinate();
    comp3.setId("coord-sw-3");
    comp3.setName("jackson-core");
    comp3.setVersion("2.16.1");
    comp3.setPackageUrl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.16.1");

    ThirdPartyFileCoordinate comp4 = new ThirdPartyFileCoordinate();
    comp4.setId("coord-sw-4");
    comp4.setName("slf4j-api");
    comp4.setVersion("2.0.9");
    comp4.setPackageUrl("pkg:maven/org.slf4j/slf4j-api@2.0.9");

    ThirdPartyFileCoordinate comp5 = new ThirdPartyFileCoordinate();
    comp5.setId("coord-sw-5");
    comp5.setName("logback-classic");
    comp5.setVersion("1.4.14");
    comp5.setPackageUrl("pkg:maven/ch.qos.logback/logback-classic@1.4.14");

    List<ThirdPartyFileCoordinate> components = List.of(comp1, comp2, comp3, comp4, comp5);

    SpdxGenerationContext context = new SpdxGenerationContext(
        components,
        List.of(),
        Map.of(),
        List.of(),
        List.of(),
        "multi-pkg-app",
        "2.0.0",
        "3.0",
        null,
        "https://example.com/iq/sbom/multi-pkg-app/2.0.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue(jsonLd.contains("@context"));
    assertTrue(jsonLd.contains("multi-pkg-app"));

    ParsedSpdxResult parsed = handler.parse(jsonLd, SbomFormat.JSON);
    assertEquals(5, parsed.resolvedComponents().size());

    assertTrue(parsed.resolvedComponents()
        .stream()
        .anyMatch(p -> "commons-lang3".equals(p.getRight().getName())));
    assertTrue(parsed.resolvedComponents()
        .stream()
        .anyMatch(p -> "guava".equals(p.getRight().getName())));
    assertTrue(parsed.resolvedComponents()
        .stream()
        .anyMatch(p -> "jackson-core".equals(p.getRight().getName())));
    assertTrue(parsed.resolvedComponents()
        .stream()
        .anyMatch(p -> "slf4j-api".equals(p.getRight().getName())));
    assertTrue(parsed.resolvedComponents()
        .stream()
        .anyMatch(p -> "logback-classic".equals(p.getRight().getName())));

    assertTrue(parsed.resolvedComponents()
        .stream()
        .anyMatch(p -> {
          String purl = p.getRight().getPurl();
          return purl != null && purl.contains("com.google.guava/guava@33.0.0-jre");
        }));
  }

  @Test
  public void generate_allVexStates_roundTripsCorrectly() throws Exception {
    ThirdPartyFileCoordinate comp1 = new ThirdPartyFileCoordinate();
    comp1.setId("coord-vex-1");
    comp1.setName("jackson-databind");
    comp1.setVersion("2.16.1");
    comp1.setPackageUrl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.16.1");

    ThirdPartyFileCoordinate comp2 = new ThirdPartyFileCoordinate();
    comp2.setId("coord-vex-2");
    comp2.setName("log4j-core");
    comp2.setVersion("2.17.0");
    comp2.setPackageUrl("pkg:maven/org.apache.logging.log4j/log4j-core@2.17.0");

    ThirdPartyCoordinateSecurity vuln1 = new ThirdPartyCoordinateSecurity();
    vuln1.setId("sec-vex-1");
    vuln1.setFileCoordinateId("coord-vex-1");
    vuln1.setRefId("CVE-2024-0001");
    vuln1.setDescription("Affected vulnerability");

    ThirdPartyCoordinateSecurity vuln2 = new ThirdPartyCoordinateSecurity();
    vuln2.setId("sec-vex-2");
    vuln2.setFileCoordinateId("coord-vex-1");
    vuln2.setRefId("CVE-2024-0002");
    vuln2.setDescription("Not affected vulnerability");

    ThirdPartyCoordinateSecurity vuln3 = new ThirdPartyCoordinateSecurity();
    vuln3.setId("sec-vex-3");
    vuln3.setFileCoordinateId("coord-vex-2");
    vuln3.setRefId("CVE-2024-0003");
    vuln3.setDescription("Under investigation");

    ThirdPartyCoordinateSecurity vuln4 = new ThirdPartyCoordinateSecurity();
    vuln4.setId("sec-vex-4");
    vuln4.setFileCoordinateId("coord-vex-2");
    vuln4.setRefId("CVE-2024-0004");
    vuln4.setDescription("Fixed vulnerability");

    ThirdPartyVulnerabilityExploitabilityExchange vex1 = new ThirdPartyVulnerabilityExploitabilityExchange();
    vex1.setCoordinateSecurityId("sec-vex-1");
    vex1.setRefId("CVE-2024-0001");
    vex1.setState("affected");

    ThirdPartyVulnerabilityExploitabilityExchange vex2 = new ThirdPartyVulnerabilityExploitabilityExchange();
    vex2.setCoordinateSecurityId("sec-vex-2");
    vex2.setRefId("CVE-2024-0002");
    vex2.setState("not_affected");

    ThirdPartyVulnerabilityExploitabilityExchange vex3 = new ThirdPartyVulnerabilityExploitabilityExchange();
    vex3.setCoordinateSecurityId("sec-vex-3");
    vex3.setRefId("CVE-2024-0003");
    vex3.setState("under_investigation");

    ThirdPartyVulnerabilityExploitabilityExchange vex4 = new ThirdPartyVulnerabilityExploitabilityExchange();
    vex4.setCoordinateSecurityId("sec-vex-4");
    vex4.setRefId("CVE-2024-0004");
    vex4.setState("fixed");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp1, comp2),
        List.of(vuln1, vuln2, vuln3, vuln4),
        Map.of(),
        List.of(vex1, vex2, vex3, vex4),
        List.of(),
        "vex-test-app",
        "1.0.0",
        "3.0",
        null,
        "https://example.com/iq/sbom/vex-test-app/1.0.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue(jsonLd.contains("security_VexAffectedVulnAssessmentRelationship"));
    assertTrue(jsonLd.contains("security_VexNotAffectedVulnAssessmentRelationship"));
    assertTrue(jsonLd.contains("security_VexUnderInvestigationVulnAssessmentRelationship"));
    assertTrue(jsonLd.contains("security_VexFixedVulnAssessmentRelationship"));

    ParsedSpdxResult parsed = handler.parse(jsonLd, SbomFormat.JSON);
    assertEquals(4, parsed.vulnerabilities().size());
    assertTrue(parsed.vulnerabilities().stream().anyMatch(v -> "CVE-2024-0001".equals(v.getRefId())));
    assertTrue(parsed.vulnerabilities().stream().anyMatch(v -> "CVE-2024-0002".equals(v.getRefId())));
    assertTrue(parsed.vulnerabilities().stream().anyMatch(v -> "CVE-2024-0003".equals(v.getRefId())));
    assertTrue(parsed.vulnerabilities().stream().anyMatch(v -> "CVE-2024-0004".equals(v.getRefId())));

    List<ThirdPartyVulnerabilityExploitabilityExchange> parsedVex = handler.parseVex(jsonLd, SbomFormat.JSON);
    assertTrue(parsedVex.stream().anyMatch(v -> "affected".equalsIgnoreCase(v.getState())));
    assertTrue(parsedVex.stream().anyMatch(v -> "not_affected".equalsIgnoreCase(v.getState())));
    assertTrue(parsedVex.stream().anyMatch(v -> "under_investigation".equalsIgnoreCase(v.getState())));
    assertTrue(parsedVex.stream().anyMatch(v -> "fixed".equalsIgnoreCase(v.getState())));
  }

  @Test
  public void parse_securityVex_capturesVulnerabilityToPackageMappings() throws Exception {
    String content = loadFixture("sbom/spdx3/security-vex-spdx-3.0.spdx.json");
    ParsedSpdxResult result = handler.parse(content, SbomFormat.JSON);

    Map<String, Set<String>> mappings = result.vulnerabilityToPackageUris();
    assertNotNull(mappings);
    assertFalse("Expected vulnerability-to-package mappings", mappings.isEmpty());

    assertTrue(mappings.containsKey("CVE-2024-1001"));
    assertTrue(mappings.get("CVE-2024-1001").contains("https://example.org/vex-pkg-jackson"));
    assertTrue(mappings.get("CVE-2024-1001").contains("https://example.org/vex-pkg-log4j"));

    assertTrue(mappings.containsKey("CVE-2024-1002"));
    assertTrue(mappings.get("CVE-2024-1002").contains("https://example.org/vex-pkg-log4j"));
    assertTrue(mappings.get("CVE-2024-1002").contains("https://example.org/vex-pkg-jackson"));
  }

  @Test
  public void parse_securityVex_vexAnnotationsHaveCorrectStates() throws Exception {
    String content = loadFixture("sbom/spdx3/security-vex-spdx-3.0.spdx.json");
    ParsedSpdxResult result = handler.parse(content, SbomFormat.JSON);

    assertEquals(4, result.vexAnnotations().size());
    assertTrue(result.vexAnnotations().stream().anyMatch(v -> "affected".equals(v.getState())));
    assertTrue(result.vexAnnotations().stream().anyMatch(v -> "not_affected".equals(v.getState())));
    assertTrue(result.vexAnnotations().stream().anyMatch(v -> "under_investigation".equals(v.getState())));
    assertTrue(result.vexAnnotations().stream().anyMatch(v -> "fixed".equals(v.getState())));
  }

  @Test
  public void parse_securityVex_perVexPackageUrisMappedCorrectly() throws Exception {
    String content = loadFixture("sbom/spdx3/security-vex-spdx-3.0.spdx.json");
    ParsedSpdxResult result = handler.parse(content, SbomFormat.JSON);

    List<Set<String>> vexPackageUris = result.vexAffectedPackageUris();
    assertEquals(result.vexAnnotations().size(), vexPackageUris.size());

    for (int i = 0; i < result.vexAnnotations().size(); i++) {
      ThirdPartyVulnerabilityExploitabilityExchange vex = result.vexAnnotations().get(i);
      Set<String> uris = vexPackageUris.get(i);
      assertFalse("VEX entry should have at least one affected URI", uris.isEmpty());
      assertEquals("Each VEX relationship targets exactly one package", 1, uris.size());

      if ("affected".equals(vex.getState()) && "CVE-2024-1001".equals(vex.getRefId())) {
        assertTrue(uris.contains("https://example.org/vex-pkg-jackson"));
      }
      else if ("not_affected".equals(vex.getState()) && "CVE-2024-1001".equals(vex.getRefId())) {
        assertTrue(uris.contains("https://example.org/vex-pkg-log4j"));
      }
      else if ("under_investigation".equals(vex.getState()) && "CVE-2024-1002".equals(vex.getRefId())) {
        assertTrue(uris.contains("https://example.org/vex-pkg-log4j"));
      }
      else if ("fixed".equals(vex.getState()) && "CVE-2024-1002".equals(vex.getRefId())) {
        assertTrue(uris.contains("https://example.org/vex-pkg-jackson"));
      }
    }
  }

  @Test
  public void generate_withExtendedProfileElements_roundTripsAiProfile() throws Exception {
    String aiFixture = loadFixture("sbom/spdx3/ai-profile-spdx-3.0.spdx.json");
    String blob = handler.extractUnsupportedProfileElements(aiFixture, SbomFormat.JSON);
    assertNotNull("AI profile should be extracted from fixture", blob);

    ThirdPartyFileCoordinate comp = new ThirdPartyFileCoordinate();
    comp.setId("coord-ai-1");
    comp.setName("ml-application");
    comp.setVersion("1.0.0");
    comp.setPackageUrl("pkg:pypi/ml-application@1.0.0");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp),
        List.of(),
        Map.of(),
        List.of(),
        List.of(),
        "ai-test-app",
        "1.0.0",
        "3.0",
        blob,
        "https://example.com/iq/sbom/ai-test-app/1.0.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue("Expected AI profile element in output", jsonLd.contains("ai_AIPackage"));
    assertTrue("Expected AI domain data", jsonLd.contains("sentiment-analysis"));

    String reExtracted = handler.extractUnsupportedProfileElements(jsonLd, SbomFormat.JSON);
    assertNotNull("AI profile should survive round-trip", reExtracted);
    assertTrue("Re-extracted blob should contain AI package", reExtracted.contains("ai_AIPackage"));
  }

  @Test
  public void generate_withExtendedProfileElements_roundTripsDatasetProfile() throws Exception {
    String fixture = loadFixture("sbom/spdx3/dataset-profile-spdx-3.0.spdx.json");
    String blob = handler.extractUnsupportedProfileElements(fixture, SbomFormat.JSON);
    assertNotNull("Dataset profile should be extracted from fixture", blob);
    assertTrue(blob.contains("dataset_Dataset"));

    ThirdPartyFileCoordinate comp = new ThirdPartyFileCoordinate();
    comp.setId("coord-ds-1");
    comp.setName("data-pipeline");
    comp.setVersion("2.0.0");
    comp.setPackageUrl("pkg:pypi/data-pipeline@2.0.0");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp),
        List.of(),
        Map.of(),
        List.of(),
        List.of(),
        "dataset-test-app",
        "2.0.0",
        "3.0",
        blob,
        "https://example.com/iq/sbom/dataset-test-app/2.0.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue("Expected Dataset profile element in output", jsonLd.contains("dataset_Dataset"));
    assertTrue("Expected dataset type data", jsonLd.contains("structured"));
    assertTrue("Expected bias data", jsonLd.contains("Genre selection bias"));

    String reExtracted = handler.extractUnsupportedProfileElements(jsonLd, SbomFormat.JSON);
    assertNotNull("Dataset profile should survive round-trip", reExtracted);
    assertTrue("Re-extracted blob should contain Dataset element", reExtracted.contains("dataset_Dataset"));
  }

  @Test
  public void generate_withExtendedProfileElements_roundTripsBuildProfile() throws Exception {
    String fixture = loadFixture("sbom/spdx3/build-profile-spdx-3.0.spdx.json");
    String blob = handler.extractUnsupportedProfileElements(fixture, SbomFormat.JSON);
    assertNotNull("Build profile should be extracted from fixture", blob);
    assertTrue(blob.contains("build_Build"));

    ThirdPartyFileCoordinate comp = new ThirdPartyFileCoordinate();
    comp.setId("coord-bld-1");
    comp.setName("webapp-service");
    comp.setVersion("4.1.0");
    comp.setPackageUrl("pkg:maven/com.example/webapp-service@4.1.0");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp),
        List.of(),
        Map.of(),
        List.of(),
        List.of(),
        "build-test-app",
        "4.1.0",
        "3.0",
        blob,
        "https://example.com/iq/sbom/build-test-app/4.1.0",
        null,
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue("Expected Build profile element in output", jsonLd.contains("build_Build"));
    assertTrue("Expected build ID", jsonLd.contains("jenkins-pipeline-build-42"));
    assertTrue("Expected build environment data", jsonLd.contains("linux-agent-01"));

    String reExtracted = handler.extractUnsupportedProfileElements(jsonLd, SbomFormat.JSON);
    assertNotNull("Build profile should survive round-trip", reExtracted);
    assertTrue("Re-extracted blob should contain Build element", reExtracted.contains("build_Build"));
  }

  @Test
  public void generate_withCompanionCdxFilename_includesExternalRef() throws Exception {
    ThirdPartyFileCoordinate comp = new ThirdPartyFileCoordinate();
    comp.setId("coord-ref-1");
    comp.setName("jackson-databind");
    comp.setVersion("2.16.1");
    comp.setPackageUrl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.16.1");

    SpdxGenerationContext context = new SpdxGenerationContext(
        List.of(comp),
        List.of(),
        Map.of(),
        List.of(),
        List.of(),
        "ext-ref-app",
        "1.0.0",
        "3.0",
        null,
        "https://example.com/iq/sbom/ext-ref-app/1.0.0",
        "ext-ref-app-build-scan1.bom.json",
        null);

    byte[] output = handler.generate(context);
    String jsonLd = new String(output, StandardCharsets.UTF_8);

    assertTrue("Expected ExternalRef locator pointing to CycloneDX file",
        jsonLd.contains("file://ext-ref-app-build-scan1.bom.json"));
    assertTrue("Expected CycloneDX content type",
        jsonLd.contains("application/vnd.cyclonedx+json"));
  }

  @Test
  public void extractUnsupportedProfileElements_datasetProfile_returnsBlob() throws Exception {
    String content = loadFixture("sbom/spdx3/dataset-profile-spdx-3.0.spdx.json");
    String blob = handler.extractUnsupportedProfileElements(content, SbomFormat.JSON);

    assertNotNull(blob);
    assertTrue(blob.contains("dataset_"));
  }

  @Test
  public void extractUnsupportedProfileElements_buildProfile_returnsBlob() throws Exception {
    String content = loadFixture("sbom/spdx3/build-profile-spdx-3.0.spdx.json");
    String blob = handler.extractUnsupportedProfileElements(content, SbomFormat.JSON);

    assertNotNull(blob);
    assertTrue(blob.contains("build_"));
  }

  private String loadFixture(String path) throws IOException {
    return IOUtils.resourceToString("/" + path, StandardCharsets.UTF_8);
  }
}
