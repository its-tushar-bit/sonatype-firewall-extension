/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.regression;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiSbomResource;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.assertj.JsonAssert.ConfigurableJsonAssert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.assertj.CompareAssert;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelector;
import org.xmlunit.diff.ElementSelectors;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.CYCLONEDX_JSON_IGNORE_FIELDS;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.SPDX3_JSON_IGNORE_FIELDS;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.SPDX_JSON_IGNORE_FIELDS;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreAttributesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreNodesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.spdxIgnoreAttributesFilter;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * <p>
 * Note: These are expensive/slow tests. So expand it carefully.
 * </p>
 * <p>
 * Some Guidance on using/extending these tests:
 * <p>
 * <ul>
 * <li>We use these tests to test the end-to-end flow of importing and exporting SBOMs in different formats, versions,
 * specifications.</li>
 *
 * <li>The idea is to catch regression issues when converting and exporting to different formats due the
 * ongoing changes in core SBOM features.</li>
 *
 * <li>Try to use comprehensive SBOMs for each scenario to cover as many edge cases as possible</li>
 *
 * <li>Use the `variant` scenarios only for exceptional cases that cannot be meaningfully covered in general cases
 * and that is agreed upon by the team. (Ideally all variants should be tested at unit tests level)</li>
 *
 * <li>If any unit/int test scenarios are automatically getting covered by these regression tests,
 * consider removing those duplicate unit/int tests.</li>
 *
 * </ul>
 */
@IqPostgresTest
class SbomRegressionTest
{
  private static final String ORIGINALS_DIR = "/SbomRegressionTest/originals/";

  private static final String MOCK_HDS_DIR = "/SbomRegressionTest/mock-reports/";

  private static final String VEX_DIR = "/SbomRegressionTest/vex/";

  private static final String EXPECTED_DIR = "/SbomRegressionTest/expected/";

  private static final String IMPORT_SBOM_TEMPLATE = "%s_%s.%s";

  private static final String IMPORT_SBOM_VARIANT_TEMPLATE = "%s_%s_%s.%s";

  private static final String MOCK_HDS_DEFAULT = "default";

  private static final String MOCK_HDS_TEMPLATE = "%s_%s_%s";

  private static final String MOCK_HDS_VARIANT_TEMPLATE = "%s_%s_%s_%s";

  private static final String VEX_FILE_TEMPLATE = "%s_%s_%s_to_%s_%s_%s.json";

  private static final String VEX_FILE_VARIANT_TEMPLATE = "%s_%s_%s_%s_to_%s_%s_%s_%s.json";

  private static final String EXPECTED_SBOM_TEMPLATE = "%s_%s_%s_to_%s_%s.%s";

  private static final String EXPECTED_SBOM_VARIANT_TEMPLATE = "%s_%s_%s_%s_to_%s_%s_%s.%s";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private IqTestContext ctx;

  private Application app;

  @BeforeEach
  void before() throws Exception {
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.APPLICATION_EVALUATION);
    app = ctx.tempEntity().newApplicationWithParent("SbomRegressionTestApp", "SbomRegressionTestApp");
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.SBOM_RESOURCE_PATH);
  }

  static Collection<Object[]> data() {
    Object[][] data = {
      {"cyclonedx", "1.1", "xml", "cyclonedx", "1.5", "xml", ""},
      {"cyclonedx", "1.1", "xml", "cyclonedx", "1.5", "json", ""},
      {"cyclonedx", "1.1", "xml", "cyclonedx", "1.6", "xml", ""},
      {"cyclonedx", "1.1", "xml", "cyclonedx", "1.6", "json", ""},
      {"cyclonedx", "1.1", "xml", "spdx", "2.2", "xml", ""},
      {"cyclonedx", "1.1", "xml", "spdx", "2.2", "json", ""},
      {"cyclonedx", "1.1", "xml", "spdx", "2.3", "xml", ""},
      {"cyclonedx", "1.1", "xml", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.4", "xml", "cyclonedx", "1.5", "xml", ""},
      {"cyclonedx", "1.4", "xml", "cyclonedx", "1.5", "json", ""},
      {"cyclonedx", "1.4", "xml", "cyclonedx", "1.6", "xml", ""},
      {"cyclonedx", "1.4", "xml", "cyclonedx", "1.6", "json", ""},
      {"cyclonedx", "1.4", "xml", "spdx", "2.2", "xml", ""},
      {"cyclonedx", "1.4", "xml", "spdx", "2.2", "json", ""},
      {"cyclonedx", "1.4", "xml", "spdx", "2.3", "xml", ""},
      {"cyclonedx", "1.4", "xml", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.4", "json", "cyclonedx", "1.5", "xml", ""},
      {"cyclonedx", "1.4", "json", "cyclonedx", "1.5", "json", ""},
      {"cyclonedx", "1.4", "json", "cyclonedx", "1.6", "xml", ""},
      {"cyclonedx", "1.4", "json", "cyclonedx", "1.6", "json", ""},
      {"cyclonedx", "1.4", "json", "spdx", "2.2", "xml", ""},
      {"cyclonedx", "1.4", "json", "spdx", "2.2", "json", ""},
      {"cyclonedx", "1.4", "json", "spdx", "2.3", "xml", ""},
      {"cyclonedx", "1.4", "json", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.5", "xml", "cyclonedx", "1.6", "xml", ""},
      {"cyclonedx", "1.5", "xml", "cyclonedx", "1.6", "json", ""},
      {"cyclonedx", "1.5", "json", "cyclonedx", "1.6", "xml", ""},
      {"cyclonedx", "1.5", "json", "cyclonedx", "1.6", "json", ""},
      {"cyclonedx", "1.5", "xml", "spdx", "2.3", "xml", ""},
      {"cyclonedx", "1.5", "xml", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.5", "json", "spdx", "2.3", "xml", ""},
      {"cyclonedx", "1.5", "json", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.6", "xml", "cyclonedx", "1.6", "xml", ""},
      {"cyclonedx", "1.6", "xml", "cyclonedx", "1.6", "json", ""},
      {"cyclonedx", "1.6", "json", "cyclonedx", "1.6", "xml", ""},
      {"cyclonedx", "1.6", "json", "cyclonedx", "1.6", "json", ""},
      {"cyclonedx", "1.6", "xml", "spdx", "2.2", "xml", ""},
      {"cyclonedx", "1.6", "xml", "spdx", "2.2", "json", ""},
      {"cyclonedx", "1.6", "json", "spdx", "2.2", "xml", ""},
      {"cyclonedx", "1.6", "json", "spdx", "2.2", "json", ""},
      {"cyclonedx", "1.6", "xml", "spdx", "2.3", "xml", ""},
      {"cyclonedx", "1.6", "xml", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.6", "json", "spdx", "2.3", "xml", ""},
      {"cyclonedx", "1.6", "json", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.6", "json", "spdx", "3.0", "json", ""},
      {"cyclonedx", "1.6", "xml", "spdx", "3.0", "json", ""},
      // CycloneDX 1.7 round-trip and SPDX bridges only (forward-only export per
      // validateCycloneDxAllowedForwardSpecVersionsOnly)
      {"cyclonedx", "1.7", "json", "cyclonedx", "1.7", "json", ""},
      {"cyclonedx", "1.7", "json", "cyclonedx", "1.7", "xml", ""},
      {"cyclonedx", "1.7", "json", "spdx", "2.3", "json", ""},
      {"cyclonedx", "1.7", "json", "spdx", "3.0", "json", ""},
      // 1.7 XML source: exercises the XML import/parse path for the new 1.7 fields
      // (isExternal, versionRange, evidence.identity, license.licensing).
      {"cyclonedx", "1.7", "xml", "cyclonedx", "1.7", "json", ""},
      {"cyclonedx", "1.7", "xml", "cyclonedx", "1.7", "xml", ""},
      {"cyclonedx", "1.7", "xml", "spdx", "2.3", "json", ""},
    };
    return Arrays.asList(data);
  }

  static Stream<Arguments> importExportSpecs() {
    return data().stream()
        .map(row -> Arguments.of(row[0], row[1], row[2], row[3], row[4], row[5], row[6]));
  }

  @ParameterizedTest(name = "from: {0}_{1}_{2} -> to: {3}_{4}_{5} {6}")
  @MethodSource("importExportSpecs")
  void testImportAndExport(
      final String importSpec,
      final String importSpecVersion,
      final String importSpecFormat,
      final String exportSpec,
      final String exportSpecVersion,
      final String exportSpecFormat,
      final String variant) throws Exception
  {
    // import flow
    String originalSbomFileName = getOriginalSbomFileName(importSpec, importSpecVersion, importSpecFormat, variant);
    ctx.mockReport("SCAN-ID", getMockHdsReport(importSpec, importSpecVersion, importSpecFormat, variant));
    byte[] sbomFile = loadFileFromAssets(
        getOriginalSbomFileFullPath(importSpec, importSpecVersion, importSpecFormat, variant));

    HttpResponse importResponse = restRequest().path(ApiSbomResource.SBOM_IMPORT_PATH)
        .part("file", originalSbomFileName, sbomFile)
        .part("applicationId", app.getId())
        .post();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), importResponse);
    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = importResponse.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).startsWith(
        String.format("%s%s/%s/status", PublicApiPaths.SBOM_RESOURCE_PATH, ApiSbomResource.SBOMS_APPLICATIONS_PATH,
            app.getId()));

    ApiSbomStatusDTO resultDTO = getSbomStatusDTO(apiThirdPartyScanTicketDTO.statusUrl);
    assertThat(resultDTO.errorMessage).isNull();
    assertThat(resultDTO.isError).isFalse();

    String sbomVersion = resultDTO.version;
    // apply vex
    String vexDataFile = getVexFileName(importSpec, importSpecVersion, importSpecFormat, exportSpec,
        exportSpecVersion, exportSpecFormat, variant);
    if (fileExists(vexDataFile)) {
      applyVex(vexDataFile, sbomVersion);
    }

    // export flow
    HttpResponse exportResponse = restRequest().path(ApiSbomResource.SBOM_VERSION_PATH)
        .parameter(app.getId(), sbomVersion)
        .query("specification=" + "%s%s".formatted(exportSpec, exportSpecVersion))
        .header(HttpHeaders.ACCEPT, getAcceptMediaType(exportSpecFormat))
        .get();
    ctx.assertResponseStatus(Status.OK.getStatusCode(), exportResponse);
    assertThat(exportResponse.getContentType()).isEqualTo("application/%s".formatted(exportSpecFormat));

    String sbomContent = new String(exportResponse.getBodyBytes());
    assertExportedContentAsExpected(sbomContent, exportSpec, exportSpecVersion, exportSpecFormat,
        importSpec, importSpecVersion, importSpecFormat, variant);
  }

  private void applyVex(final String vexFileName, String sbomVersion) throws Exception {
    JsonNode jsonNode = MAPPER.readTree(loadFileFromAssets(vexFileName)).get("aaData");
    for (JsonNode node : jsonNode) {
      String refId = node.get("refId").asText();
      ApiSbomVulnerabilityAnalysisRequestDTO dto =
          MAPPER.readValue(node.get("vex").toString(), ApiSbomVulnerabilityAnalysisRequestDTO.class);
      HttpResponse response = restRequest().path(ApiSbomResource.SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
          .parameter(app.getId(), sbomVersion, refId)
          .body(dto)
          .put();
      ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    }
  }

  private void assertExportedContentAsExpected(
      String sbomContent,
      String exportSpec,
      String exportSpecVersion,
      String exportSpecFormat,
      String importSpec,
      String importSpecVersion,
      String importSpecFormat,
      String variant) throws Exception
  {
    String expectedContent = expectedContentIn(getExpectedSbomContentFile(importSpec, importSpecVersion,
        importSpecFormat, exportSpec, exportSpecVersion, exportSpecFormat, variant));
    if (exportSpecFormat.equals("xml")) {
      CompareAssert xmlAssert = XmlAssert.assertThat(sbomContent).and(expectedContent);

      if (exportSpec.equals("spdx")) {
        xmlAssert.withNodeFilter(spdxIgnoreAttributesFilter());
      }
      else if (exportSpec.equals("cyclonedx")) {
        xmlAssert.withNodeFilter(cycloneDxIgnoreNodesFilter())
            .withAttributeFilter(cycloneDxIgnoreAttributesFilter());
      }

      ElementSelector selector =
          ElementSelectors.conditionalBuilder()
              .whenElementIsNamed("packages")
              .thenUse(ElementSelectors.byXPath("./SPDXID", ElementSelectors.byNameAndText))
              .whenElementIsNamed("checksums")
              .thenUse(ElementSelectors.byXPath("./algorithm", ElementSelectors.byNameAndText))
              .whenElementIsNamed("licenseDeclared")
              .thenUse(ElementSelectors.byNameAndText) // pairs same-text elements, order-agnostic
              .whenElementIsNamed("externalRefs")
              .thenUse(ElementSelectors.and(
                  ElementSelectors.byXPath("./referenceCategory", ElementSelectors.byNameAndText),
                  ElementSelectors.byXPath("./referenceType", ElementSelectors.byNameAndText),
                  ElementSelectors.byXPath("./referenceLocator", ElementSelectors.byNameAndText)))
              .whenElementIsNamed("relationships")
              .thenUse(ElementSelectors.and(
                  ElementSelectors.byXPath("./spdxElementId", ElementSelectors.byNameAndText),
                  ElementSelectors.byXPath("./relationshipType", ElementSelectors.byNameAndText),
                  ElementSelectors.byXPath("./relatedSpdxElement", ElementSelectors.byNameAndText)))
              // default fallback: same element name; if it's a leaf, text must match
              .elseUse(ElementSelectors.byName)
              .build();

      xmlAssert
          .withNodeMatcher(new DefaultNodeMatcher(selector))
          .ignoreWhitespace()
          .ignoreComments()
          .areSimilar();
    }
    else {
      ConfigurableJsonAssert asserter = assertThatJson(sbomContent);
      if (exportSpec.equals("spdx") && "3.0".equals(exportSpecVersion)) {
        asserter = asserter.whenIgnoringPaths(SPDX3_JSON_IGNORE_FIELDS)
            .withOptions(IGNORING_ARRAY_ORDER);
      }
      else if (exportSpec.equals("spdx")) {
        asserter = asserter.whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
            .withOptions(IGNORING_ARRAY_ORDER);
      }
      else {
        asserter = asserter.whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS);
      }
      asserter.isEqualTo(expectedContent);
    }
  }

  private static String getAcceptMediaType(String exportSpecFormat) {
    if (exportSpecFormat.equals("xml")) {
      return MediaType.APPLICATION_XML;
    }
    return MediaType.APPLICATION_JSON;
  }

  private String expectedContentIn(String filePath) throws Exception {
    return new String(loadFileFromAssets(filePath));
  }

  private ApiSbomStatusDTO getSbomStatusDTO(String statusUrl) {
    HttpResponse response = await().atMost(10, TimeUnit.SECONDS)
        .until(() -> ctx.restRequest().path(statusUrl).get(),
            resp -> resp.getStatusCode() == 200);
    return response.getBody(ApiSbomStatusDTO.class);
  }

  private byte[] loadFileFromAssets(String fileName) throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
      assertThat(inputStream).as("Missing resource: " + fileName).isNotNull();
      return IOUtils.toByteArray(inputStream);
    }
  }

  private String getOriginalSbomFileFullPath(
      String importSpec,
      String importSpecVersion,
      String importSpecFormat,
      String variant)
  {
    return ORIGINALS_DIR + getOriginalSbomFileName(importSpec, importSpecVersion, importSpecFormat, variant);
  }

  private String getOriginalSbomFileName(
      String importSpec,
      String importSpecVersion,
      String importSpecFormat,
      String variant)
  {
    return (StringUtils.isNotEmpty(variant)
        ? IMPORT_SBOM_VARIANT_TEMPLATE.formatted(importSpec, importSpecVersion, variant, importSpecFormat)
        : IMPORT_SBOM_TEMPLATE.formatted(importSpec, importSpecVersion, importSpecFormat));
  }

  private String getVexFileName(
      String importSpec,
      String importSpecVersion,
      String importSpecFormat,
      String exportSpec,
      String exportSpecVersion,
      String exportSpecFormat,
      String variant)
  {
    return VEX_DIR + (StringUtils.isNotEmpty(variant)
        ? VEX_FILE_VARIANT_TEMPLATE.formatted(importSpec, importSpecVersion, importSpecFormat, variant, exportSpec,
            exportSpecVersion, variant, exportSpecFormat)
        : VEX_FILE_TEMPLATE.formatted(importSpec, importSpecVersion, importSpecFormat, exportSpec, exportSpecVersion,
            exportSpecFormat));
  }

  private String getMockHdsReport(
      String importSpec,
      String importSpecVersion,
      String importSpecFormat,
      String variant)
  {
    String reportDir = MOCK_HDS_DIR + (StringUtils.isNotEmpty(variant)
        ? MOCK_HDS_VARIANT_TEMPLATE.formatted(importSpec, importSpecVersion, importSpecFormat, variant)
        : MOCK_HDS_TEMPLATE.formatted(importSpec, importSpecVersion, importSpecFormat));
    if (!directoryExists(reportDir)) {
      reportDir = MOCK_HDS_DIR + MOCK_HDS_DEFAULT;
    }
    return reportDir;
  }

  private String getExpectedSbomContentFile(
      String importSpec,
      String importSpecVersion,
      String importSpecFormat,
      String exportSpec,
      String exportSpecVersion,
      String exportSpecFormat,
      String variant)
  {
    return EXPECTED_DIR + (StringUtils.isNotEmpty(variant)
        ? EXPECTED_SBOM_VARIANT_TEMPLATE.formatted(importSpec, importSpecVersion, importSpecFormat, variant, exportSpec,
            exportSpecVersion, variant, exportSpecFormat)
        : EXPECTED_SBOM_TEMPLATE.formatted(importSpec, importSpecVersion, importSpecFormat, exportSpec,
            exportSpecVersion,
            exportSpecFormat));
  }

  private boolean fileExists(final String vexFileName) {
    return resourceExists(vexFileName, false);
  }

  private boolean directoryExists(final String vexFileName) {
    return resourceExists(vexFileName, true);
  }

  private boolean resourceExists(final String fileName, boolean isDirectory) {
    URL resource = getClass().getResource(fileName);
    if (resource == null) {
      return false;
    }
    try {
      File file = new File(resource.toURI());
      return isDirectory ? FileUtils.isDirectory(file) : FileUtils.isRegularFile(file);
    }
    catch (URISyntaxException e) {
      return false;
    }
  }
}
