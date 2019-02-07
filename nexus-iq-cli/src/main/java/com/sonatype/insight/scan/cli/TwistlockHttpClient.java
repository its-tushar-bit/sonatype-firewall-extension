/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

import org.apache.http.client.HttpResponseException;

/**
 * @since 1.24
 */
public final class TwistlockHttpClient
    extends AbstractClient
{
  public TwistlockHttpClient(Configuration config) {
    super(config);
  }

  public void downloadScanResults(File resultsFile) {
    try {
      final Result result = path().get();
      final int status = result.status();
      if (status >= 300) {
        throw new HttpResponseException(status, result.message());
      }

      byte[] data = result.data();
      try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(resultsFile))) {
        fos.write(data);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
