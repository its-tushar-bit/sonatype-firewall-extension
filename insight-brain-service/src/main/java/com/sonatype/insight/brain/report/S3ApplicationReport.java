/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import com.sonatype.insight.brain.report.pdf.PdfGenerator;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.utils.IdValidationUtils;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3Response;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_LICENSE_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

public class S3ApplicationReport
    extends AbstractS3ReportEntity
    implements ApplicationReport
{
  private static final Logger log = LoggerFactory.getLogger(S3ApplicationReport.class);

  private static final String TEMPLATE_PROPERTIES = "template.properties";

  public S3ApplicationReport(
      final S3Client s3Client,
      final S3DataStoreConfig s3Config,
      final String appId,
      final String scanId,
      final String name)
  {
    super(s3Client, s3Config, appId, scanId, name);
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(scanId);
  }

  @Override
  public ReportEntry getEntry(final String name) throws IOException {
    if (name.contains("../") || name.contains("..\\")) {
      // legit callers use normalized paths, no directory traversal into restricted areas
      return null;
    }

    return Stream.of(getCacheKey(name), getAdditionalObjectKey(name), getKey(name))
        .map(k -> getReportEntry(name, k))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private ReportEntry getReportEntry(final String name, final S3ObjectKey key) {
    try {
      ResponseInputStream<GetObjectResponse> s3Object = getGetObjectResponseResponseInputStream(key);
      long lastModified = s3Object.response().lastModified().toEpochMilli();
      return new ReportEntry(name, lastModified, s3Object.readAllBytes());
    }
    catch (NoSuchKeyException e) {
      log.trace("Key not found {}", key, e);
      return null;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void putEntry(final String name, final byte[] buf) {
    putObject(getCacheKey(name), buf);
  }

  private void putObject(final S3ObjectKey key, final byte[] buf) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(key.toString()).build();
    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(buf));
  }

  @Override
  public void saveReportEntry(final String entryFileName, final ContainerNode<?> jsonData) throws IOException {
    putEntry(entryFileName, JsonUtils.generate(jsonData));
  }

  @Override
  public ContainerNode<?> loadReportEntry(final String entryFileName) throws IOException {
    long start = System.currentTimeMillis();

    ReportEntry reportEntry = getEntry(entryFileName);
    ContainerNode<?> result = JsonUtils.parse(reportEntry.buf);

    log.debug("loadReportEntry: {} in {} ms.", entryFileName, System.currentTimeMillis() - start);

    return result;
  }

  @Override
  public void deletePdfReport() {
    S3ObjectKey key = getKey(PdfGenerator.REPORT_FILE_NAME);
    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(bucketName).key(key.toString()).build();
    DeleteObjectResponse deleteObjectResponse = s3Client.deleteObject(deleteObjectRequest);
    logDeleteResult(deleteObjectResponse, List.of(ObjectIdentifier.builder().key(key.toString()).build()));
  }

  @Override
  public void appendToReport(final ThirdPartyApplicationReportDTO dto) throws IOException {
    appendFileToReport(THIRD_PARTY_BOM_JSON_FILENAME, dto.billOfMaterials);
    appendFileToReport(THIRD_PARTY_SECURITY_JSON_FILENAME, dto.securityRows);
    appendFileToReport(THIRD_PARTY_LICENSE_JSON_FILENAME, dto.licenseRows);
  }

  private void appendFileToReport(final String name, final List<?> data) throws IOException {
    putObject(getAdditionalObjectKey(name), JsonUtils.generate(JsonUtils.aaData(data)));
  }

  @Override
  public ReportType getType() {
    if (!exists(getKey(SECURITY_JSON_FILENAME)) && !exists(getKey(LICENSES_JSON_FILENAME))) {
      return ReportType.ERROR;
    }
    return ReportType.FULL;
  }

  @Override
  public void deleteCacheDir() {
    ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName)
        .prefix(getCacheKeyPrefix()).build();

    ListObjectsV2Iterable listObjectsV2Iterable = s3Client.listObjectsV2Paginator(listObjectsV2Request);
    for (ListObjectsV2Response response : listObjectsV2Iterable) {
      List<S3Object> objects = response.contents();
      if (objects.isEmpty()) {
        break;
      }
      List<ObjectIdentifier> objectIdentifiers =
          objects.stream().map(o -> ObjectIdentifier.builder().key(o.key()).build()).toList();
      DeleteObjectsRequest deleteObjectsRequest =
          DeleteObjectsRequest.builder().bucket(bucketName).delete(Delete.builder().objects(objectIdentifiers).build())
              .build();
      DeleteObjectsResponse deleteObjectsResponse = s3Client.deleteObjects(deleteObjectsRequest);
      logDeleteResult(deleteObjectsResponse, objectIdentifiers);
    }
  }

  private String getCacheKeyPrefix() {
    return keyPrefix + String.format(CACHE_KEY_FORMAT_PREFIX, appId, scanId);
  }

  private static void logDeleteResult(final S3Response deleteObjectResponse, final List<ObjectIdentifier> keys) {
    SdkHttpResponse sdkHttpResponse = deleteObjectResponse.sdkHttpResponse();
    if (!sdkHttpResponse.isSuccessful()) {
      log.warn("Attempted to delete '{}' but it did not succeed. Code: {}, Text: {}", keys,
          sdkHttpResponse.statusCode(), sdkHttpResponse.statusText());
    }
  }

  @Override
  public Properties getTemplateProperties() throws IOException {
    Properties props = new Properties();
    ReportEntry entry = getEntry(TEMPLATE_PROPERTIES);
    if (entry != null) {
      try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(entry.buf)) {
        props.load(byteArrayInputStream);
      }
    }
    return props;
  }

  @Override
  public String getLocation() {
    return getKey().toString();
  }
}
