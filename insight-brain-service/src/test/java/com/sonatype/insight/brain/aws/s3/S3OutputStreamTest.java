/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aws.s3;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3OutputStreamTest
{
  @Test
  public void shouldWaitForPutObjectSubscriptionBeforeWritingFirstBytes() throws Exception {
    S3AsyncClient s3AsyncClient = Mockito.mock(S3AsyncClient.class);
    CompletableFuture<PutObjectResponse> responseFuture = new CompletableFuture<>();
    AtomicReference<String> payload = new AtomicReference<>("");

    when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
        .thenAnswer(invocation -> {
          AsyncRequestBody requestBody = invocation.getArgument(1);
          Thread subscriberThread = new Thread(() -> requestBody.subscribe(new Subscriber<ByteBuffer>()
          {
            @Override
            public void onSubscribe(final Subscription subscription) {
              subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final ByteBuffer byteBuffer) {
              byte[] bytes = new byte[byteBuffer.remaining()];
              byteBuffer.get(bytes);
              payload.updateAndGet(existing -> existing + new String(bytes, UTF_8));
            }

            @Override
            public void onError(final Throwable throwable) {
              responseFuture.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
              responseFuture.complete(PutObjectResponse.builder().build());
            }
          }));
          subscriberThread.setDaemon(true);
          subscriberThread.start();
          return responseFuture;
        });

    S3OutputStream outputStream = new S3OutputStream(s3AsyncClient, "prefix/object.txt", "bucket-name", null);
    outputStream.write("payload".getBytes(UTF_8));
    outputStream.close();

    verify(s3AsyncClient).putObject(
        argThat((PutObjectRequest request) -> request.bucket().equals("bucket-name")
            && request.key().equals("prefix/object.txt")),
        any(AsyncRequestBody.class));
    assertThat(payload.get()).isEqualTo("payload");
  }
}
