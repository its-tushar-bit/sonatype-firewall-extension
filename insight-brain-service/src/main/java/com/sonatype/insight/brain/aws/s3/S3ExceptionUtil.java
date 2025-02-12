/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.aws.s3;

import java.io.IOException;
import java.util.function.Supplier;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3ExceptionUtil
{
  /**
   * Executes the supplier and wraps any thrown S3Exception in an IOException.
   */
  public static <T> T wrapS3Exception(Supplier<T> supplier) throws IOException {
    try {
      return supplier.get();
    }
    catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Executes the runnable and wraps any thrown S3Exception in an IOException.
   */
  public static void wrapS3Exception(Runnable runnable) throws IOException {
    try {
      runnable.run();
    }
    catch (S3Exception e) {
      throw new IOException(e);
    }
  }
}
