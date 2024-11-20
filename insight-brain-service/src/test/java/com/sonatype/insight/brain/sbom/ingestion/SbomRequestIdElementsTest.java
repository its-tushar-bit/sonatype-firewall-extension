/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;

import org.junit.Test;

import static com.sonatype.insight.brain.sbom.ingestion.SbomRequestIdElements.decodeFromRequestId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SbomRequestIdElementsTest
{
  @Test
  public void testEncodeRequestId_binary() {
    String filenameUUID = "a7b0e4b584ae4bc3b5965ae1ea1665c9";
    String originalFilename = "test_bom.json";

    String encodedRequestId = new SbomRequestIdElements(filenameUUID, originalFilename).encodeRequestId();
    assertThat(encodedRequestId).isEqualTo("QklOQVJZLWE3YjBlNGI1ODRhZTRiYzNiNTk2NWFlMWVhMTY2NWM5LXRlc3RfYm9tLmpzb24=");

    String decodedRequestId = new String(Base64.getDecoder().decode(encodedRequestId), StandardCharsets.UTF_8);
    String[] parts = decodedRequestId.split("-");

    assertThat(parts[0]).isEqualTo(SbomScanType.BINARY.name());
    assertThat(parts[1]).isEqualTo(filenameUUID);
    assertThat(parts[2]).isEqualTo(originalFilename);
  }

  @Test
  public void testEncodeRequestId_SBOM_CDX() {
    String filenameUUID = "6f800ea6109e499f83f8a2f2482da722";
    String originalFilename = "test_bom.json";
    SbomFormat format = SbomFormat.forMimeType("application/json");
    ItemContentType contentType = ItemContentType.SBOM;

    String encodedRequestId = new SbomRequestIdElements(filenameUUID, originalFilename, format, contentType, false)
        .encodeRequestId();
    assertThat(encodedRequestId)
        .isEqualTo("U0JPTS1mYWxzZS1qc29uLVNCT00tNmY4MDBlYTYxMDllNDk5ZjgzZjhhMmYyNDgyZGE3MjItdGVzdF9ib20uanNvbg==");

    String decodedRequestId = new String(Base64.getDecoder().decode(encodedRequestId), StandardCharsets.UTF_8);
    String[] parts = decodedRequestId.split("-");

    assertThat(parts[0]).isEqualTo(SbomScanType.SBOM.name());
    assertThat(parts[1]).isEqualTo("false");
    assertThat(parts[2]).isEqualTo(format.toString());
    assertThat(parts[3]).isEqualTo(contentType.name());
    assertThat(parts[4]).isEqualTo(filenameUUID);
    assertThat(parts[5]).isEqualTo(originalFilename);
  }

  @Test
  public void testEncodeRequestId_SBOM_SPDX() {
    String filenameUUID = "d5e57e665f3e47fe861c97ad68778ca5";
    String originalFilenameHyphenated = "test-spdx-bom.json";
    SbomFormat format = SbomFormat.forMimeType("application/json");
    ItemContentType contentType = ItemContentType.SPDX;

    String encodedRequestId = new SbomRequestIdElements(filenameUUID, originalFilenameHyphenated, format, contentType,
        false).encodeRequestId();
    assertThat(encodedRequestId)
        .isEqualTo("U0JPTS1mYWxzZS1qc29uLVNQRFgtZDVlNTdlNjY1ZjNlNDdmZTg2MWM5N2FkNjg3NzhjYTUtdGVzdC1zcGR4LWJvbS5qc29u");

    String decodedRequestId = new String(Base64.getDecoder().decode(encodedRequestId), StandardCharsets.UTF_8);
    String[] parts = decodedRequestId.split("-");

    assertThat(parts[0]).isEqualTo(SbomScanType.SBOM.name());
    assertThat(parts[1]).isEqualTo("false");
    assertThat(parts[2]).isEqualTo(format.toString());
    assertThat(parts[3]).isEqualTo(contentType.name());
    assertThat(parts[4]).isEqualTo(filenameUUID);
    assertThat(String.format("%s-%s-%s", parts[5], parts[6], parts[7])).isEqualTo(originalFilenameHyphenated);
  }

  @Test
  public void testDecodeRequestId_emptyRequestId() {
    assertThat(decodeFromRequestId("")).isNull();
  }

  @Test
  public void testDecodeRequestId_invalidRequestId() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      decodeFromRequestId("invalid");
    }).withMessage("The provided requestId invalid is not valid.");
  }

  @Test
  public void testDecodeRequestId_validRequestId_SBOM_CDX() {
    String filenameUUID = "f92e435f02064d34b471728e33dd12f9";
    String originalFilenameHypenated = "test-cdx-bom.json";
    String requestId = Base64.getEncoder().encodeToString(
        String.format("SBOM-false-json-SBOM-%s-%s", filenameUUID, originalFilenameHypenated)
            .getBytes(StandardCharsets.UTF_8));

    SbomRequestIdElements requestIdElements = decodeFromRequestId(requestId);

    assertThat(requestIdElements).isNotNull();
    assertThat(requestIdElements.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(requestIdElements.getSbomFormat()).isEqualTo(SbomFormat.JSON);
    assertThat(requestIdElements.getFileNameUUID()).isEqualTo(filenameUUID);
    assertThat(requestIdElements.getOriginalFileName()).isEqualTo(originalFilenameHypenated);
    assertThat(requestIdElements.getStoredFileName()).isEqualTo(
        String.format("%s-%s", filenameUUID, originalFilenameHypenated));
    assertThat(requestIdElements.getContentType()).isEqualTo(ItemContentType.SBOM);
    assertThat(requestIdElements.isSbomValid()).isEqualTo(false);
  }

  @Test
  public void testDecodeRequestId_validRequestId_SBOM_SPDX() {
    String filenameUUID = "a831b57d66c14325885ad49d623cf966";
    String originalFilenameUnderscored = "test_spdx_bom.xml";
    String requestId = Base64.getEncoder().encodeToString(
        String.format("SBOM-true-xml-SPDX-%s-%s", filenameUUID, originalFilenameUnderscored)
            .getBytes(StandardCharsets.UTF_8));

    SbomRequestIdElements requestIdElements = decodeFromRequestId(requestId);

    assertThat(requestIdElements).isNotNull();
    assertThat(requestIdElements.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(requestIdElements.getSbomFormat()).isEqualTo(SbomFormat.XML);
    assertThat(requestIdElements.getFileNameUUID()).isEqualTo(filenameUUID);
    assertThat(requestIdElements.getOriginalFileName()).isEqualTo(originalFilenameUnderscored);
    assertThat(requestIdElements.getStoredFileName()).isEqualTo(
        String.format("%s-%s", filenameUUID, originalFilenameUnderscored));
    assertThat(requestIdElements.getContentType()).isEqualTo(ItemContentType.SPDX);
    assertThat(requestIdElements.isSbomValid()).isEqualTo(true);
  }

  @Test
  public void testDecodeRequestId_validRequestId_BINARY() {
    String filenameUUID = "705591283c78482eab39b71aa4e8229b";
    String originalFilename = "test.jar";
    String requestId = Base64.getEncoder().encodeToString(String.format("BINARY-%s-%s", filenameUUID, originalFilename)
        .getBytes(StandardCharsets.UTF_8));

    SbomRequestIdElements requestIdElements = decodeFromRequestId(requestId);

    assertThat(requestIdElements).isNotNull();
    assertThat(requestIdElements.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(requestIdElements.getSbomFormat()).isNull();
    assertThat(requestIdElements.getFileNameUUID()).isEqualTo(filenameUUID);
    assertThat(requestIdElements.getOriginalFileName()).isEqualTo(originalFilename);
    assertThat(requestIdElements.getStoredFileName()).isEqualTo(String.format("%s-%s", filenameUUID, originalFilename));
    assertThat(requestIdElements.getContentType()).isNull();
    assertThat(requestIdElements.isSbomValid()).isEqualTo(false);
  }

  @Test
  public void testDecodeRequestId_invalidSbomScanType() {
    String requestId = Base64.getEncoder().encodeToString(
        "INVALID_REQUEST_TYPE-false-1d3708d94ee24cc5bd5dbf513d0d44cc-test.jar".getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      decodeFromRequestId(requestId);
    }).withMessage("The provided requestId " + requestId + " is not valid.");
  }

  @Test
  public void testDecodeRequestId_invalidPathTraversalInFileName() {
    String[] filePaths = new String[]{"path/test.jar", "path\\test.jar"};
    for (String filePath : filePaths) {
      String requestId = Base64.getEncoder().encodeToString(
          String.format("BINARY-701007f9c3f74929938e9e44fad4559c-%s", filePath).getBytes(StandardCharsets.UTF_8));
      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
        decodeFromRequestId(requestId);
      }).withMessage("The provided requestId " + requestId + " is not valid.");
    }
  }

  @Test
  public void testDecodeRequestId_invalidRequestUuidLength_SBOM() {
    String requestId = Base64.getEncoder()
        .encodeToString("SBOM-true-xml-SPDX-53ae831e8c1a-test_spdx_bom.xml".getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      decodeFromRequestId(requestId);
    }).withMessage("The provided requestId " + requestId + " is not valid.");
  }

  @Test
  public void testDecodeRequestId_invalidRequestUuidDigit_SBOM() {
    String requestId = Base64.getEncoder()
        .encodeToString(
            "SBOM-true-xml-SPDX-6XX195795f524XXX85241X327927fX45-test_spdx_bom.xml".getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      decodeFromRequestId(requestId);
    }).withMessage("The provided requestId " + requestId + " is not valid.");
  }

  @Test
  public void testDecodeRequestId_invalidRequestUuid_BINARY() {
    String requestId =
        Base64.getEncoder().encodeToString("BINARY-eec068c9cf7145c18600e2b0-test.jar".getBytes(StandardCharsets.UTF_8));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      decodeFromRequestId(requestId);
    }).withMessage("The provided requestId " + requestId + " is not valid.");
  }

  @Test
  public void testEncodeAndDecodeRequestId_binary() {
    String filenameUUID = "20f532ee84c34f489349b61172b040b0";
    String originalFilename = "test_bom.json";

    String encodedRequestId = new SbomRequestIdElements(filenameUUID, originalFilename).encodeRequestId();
    SbomRequestIdElements decodedRequest = decodeFromRequestId(encodedRequestId);

    assertThat(decodedRequest.getScanType()).isEqualTo(SbomScanType.BINARY);
    assertThat(decodedRequest.isSbomValid()).isEqualTo(false);
    assertThat(decodedRequest.getFileNameUUID()).isEqualTo(filenameUUID);
    assertThat(decodedRequest.getOriginalFileName()).isEqualTo(originalFilename);
  }

  @Test
  public void testEncodeAndDecodeRequestId_SBOM_CDX() {
    String filenameUUID = "f127ae02541547f1aa6f792a8211ae88";
    String originalFilename = "test_bom.json";
    SbomFormat format = SbomFormat.forMimeType("application/json");
    ItemContentType contentType = ItemContentType.SBOM;

    String encodedRequestId = new SbomRequestIdElements(filenameUUID, originalFilename, format, contentType, false)
        .encodeRequestId();
    SbomRequestIdElements decodedRequest = decodeFromRequestId(encodedRequestId);

    assertThat(decodedRequest.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(decodedRequest.isSbomValid()).isEqualTo(false);
    assertThat(decodedRequest.getSbomFormat()).isEqualTo(format);
    assertThat(decodedRequest.getContentType()).isEqualTo(contentType);
    assertThat(decodedRequest.getFileNameUUID()).isEqualTo(filenameUUID);
    assertThat(decodedRequest.getOriginalFileName()).isEqualTo(originalFilename);
  }

  @Test
  public void testEncodeAndDecodeRequestId_SBOM_SPDX() {
    String filenameUUID = "9d1d29a5bd644be0bc0df69474095217";
    String originalFilenameHyphenated = "test-spdx-bom.json";
    SbomFormat format = SbomFormat.forMimeType("application/json");
    ItemContentType contentType = ItemContentType.SPDX;

    String encodedRequestId = new SbomRequestIdElements(filenameUUID, originalFilenameHyphenated, format, contentType,
        false).encodeRequestId();
    SbomRequestIdElements decodedRequest = decodeFromRequestId(encodedRequestId);

    assertThat(decodedRequest.getScanType()).isEqualTo(SbomScanType.SBOM);
    assertThat(decodedRequest.isSbomValid()).isEqualTo(false);
    assertThat(decodedRequest.getSbomFormat()).isEqualTo(format);
    assertThat(decodedRequest.getContentType()).isEqualTo(contentType);
    assertThat(decodedRequest.getFileNameUUID()).isEqualTo(filenameUUID);
    assertThat(decodedRequest.getOriginalFileName()).isEqualTo(originalFilenameHyphenated);
  }
}
