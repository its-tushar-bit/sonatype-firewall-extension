/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aws.s3;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static com.sonatype.insight.brain.aws.s3.S3ExceptionUtil.wrapS3Exception;

public class S3OutputStream
    extends OutputStream
{
  private final S3AsyncClient s3AsyncClient;

  private final String key;

  private final String bucketName;

  private final String serverSideEncryption;

  private OutputStream outputStream;

  private CompletableFuture<PutObjectResponse> putObjectResponseCompletableFuture;

  public S3OutputStream(
      final S3AsyncClient s3AsyncClient,
      final String key,
      final String bucketName,
      final String serverSideEncryption)
  {
    this.s3AsyncClient = s3AsyncClient;
    this.key = key;
    this.bucketName = bucketName;
    this.serverSideEncryption = serverSideEncryption;
  }

  private void initializeOutputStreamIfNeeded() {
    if (outputStream == null) {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .serverSideEncryption(serverSideEncryption)
          .build();
      BlockingOutputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingOutputStream(null);
      putObjectResponseCompletableFuture = s3AsyncClient.putObject(putObjectRequest, body);
      outputStream = body.outputStream();
    }
  }

  @Override
  public void write(final int b) throws IOException {
    wrapS3Exception(this::initializeOutputStreamIfNeeded);
    outputStream.write(b);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    wrapS3Exception(this::initializeOutputStreamIfNeeded);
    outputStream.write(b);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    wrapS3Exception(this::initializeOutputStreamIfNeeded);
    outputStream.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    if (outputStream == null) {
      return;
    }
    outputStream.flush();
  }

  @Override
  public void close() throws IOException {
    if (outputStream == null) {
      return;
    }
    outputStream.close();
    try {
      putObjectResponseCompletableFuture.join();
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }
}
