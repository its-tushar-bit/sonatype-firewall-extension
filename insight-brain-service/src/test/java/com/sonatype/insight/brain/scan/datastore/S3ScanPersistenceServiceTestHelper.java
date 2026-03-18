/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.service.InsightConfig;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3ScanPersistenceServiceTestHelper
    implements ScanPersistenceServiceTestHelper
{
  private final InsightConfig insightConfig;

  private final S3Client s3Client;

  private final Supplier<String> expectedEffectivePrefixSupplier;

  public S3ScanPersistenceServiceTestHelper(
      final InsightConfig insightConfig,
      final S3Client s3Client,
      final Supplier<String> expectedEffectivePrefixSupplier)
  {
    this.insightConfig = insightConfig;
    this.s3Client = s3Client;
    this.expectedEffectivePrefixSupplier = expectedEffectivePrefixSupplier;
  }

  @Override
  public void saveMockScan(String scanId) throws IOException {
    String scanName = "scan-" + scanId + ".xml.gz";
    String key = "%sscan/%s/%s".formatted(expectedEffectivePrefixSupplier.get(), APPLICATION_ID, scanName);

    byte[] compressedContent = createCompressedScanContent(getSampleScanContent(scanId));

    s3Client.putObject(
        PutObjectRequest.builder().bucket(getBucketName()).key(key).build(),
        RequestBody.fromBytes(compressedContent));
  }

  @Override
  public void saveEmptyMockScan(String scanId) throws IOException {
    String scanName = "scan-" + scanId + ".xml.gz";
    String key = "%sscan/%s/%s".formatted(expectedEffectivePrefixSupplier.get(), APPLICATION_ID, scanName);

    byte[] compressedContent = createCompressedScanContent("empty scan");

    s3Client.putObject(
        PutObjectRequest.builder().bucket(getBucketName()).key(key).build(),
        RequestBody.fromBytes(compressedContent));
  }

  @Override
  public String readDirectScanFile(String applicationId, String scanId) throws IOException {
    String scanName = "scan-" + scanId + ".xml.gz";
    String key = "%sscan/%s/%s".formatted(expectedEffectivePrefixSupplier.get(), applicationId, scanName);

    try {
      byte[] responseContents = s3Client.getObjectAsBytes(
          GetObjectRequest.builder()
              .bucket(getBucketName())
              .key(key)
              .build())
          .asByteArray();

      try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseContents));
          ByteArrayOutputStream decompressed = new ByteArrayOutputStream())
      {
        gis.transferTo(decompressed);
        return decompressed.toString(StandardCharsets.UTF_8);
      }
    }
    catch (NoSuchKeyException e) {
      return null;
    }
  }

  @Override
  public void waitForNewFileTime() throws InterruptedException {
    // S3 times come from the Last-Modified HTTP header which gives the appearance of 1-second resolution
    Thread.sleep(1000);
  }

  @Override
  public void assertScanExists(String applicationId, String scanId, boolean expected) {
    String scanName = "scan-" + scanId + ".xml.gz";
    String key = "%sscan/%s/%s".formatted(expectedEffectivePrefixSupplier.get(), applicationId, scanName);

    boolean exists;
    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(getBucketName())
          .key(key)
          .build());
      exists = true;
    }
    catch (NoSuchKeyException e) {
      exists = false;
    }

    if (expected != exists) {
      throw new AssertionError(
          "Expected scan %s/%s to %s, but it %s".formatted(
              applicationId, scanId,
              expected ? "exist" : "not exist",
              exists ? "exists" : "does not exist"));
    }
  }

  @Override
  public void cleanup() throws IOException {
    // List and delete all test objects
    String prefix = expectedEffectivePrefixSupplier.get() + "scan/" + APPLICATION_ID + "/";

    try {
      var listRequest = ListObjectsV2Request.builder()
          .bucket(getBucketName())
          .prefix(prefix)
          .build();

      var response = s3Client.listObjectsV2(listRequest);

      for (S3Object s3Object : response.contents()) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(getBucketName())
            .key(s3Object.key())
            .build());
      }
    }
    catch (Exception e) {
      // Ignore cleanup failures in tests
    }
  }

  private String getBucketName() {
    return insightConfig.getStorage().getS3Config().getBucketName();
  }

  private byte[] createCompressedScanContent(String content) throws IOException {
    var baos = new java.io.ByteArrayOutputStream();
    try (var gzipOut = new GZIPOutputStream(baos);
        var writer = new OutputStreamWriter(gzipOut, StandardCharsets.UTF_8))
    {
      writer.write(content);
    }
    return baos.toByteArray();
  }
}
