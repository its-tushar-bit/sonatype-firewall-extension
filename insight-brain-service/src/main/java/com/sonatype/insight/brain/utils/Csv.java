/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * Dashboard-specific CSV utility class
 *
 * @since 1.24.0
 */
public final class Csv
{
  /**
   * Fill out the supplied response with required headers and data for
   * downloading the supplied results as a CSV file.
   *
   * @param response       The response to fill in
   * @param fileNamePrefix The file name prefix to use before timestamp
   * @param headerLine     The CSV header line
   * @param results        The CSV content
   */
  public static ResponseBuilder generate(ResponseBuilder response,
                                         String fileNamePrefix,
                                         final String headerLine,
                                         final Collection<? extends CsvWritable> results)
  {
    final Date now = new Date();

    response.lastModified(now);
    response.expires(now);
    response.type("text/csv");

    final String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(now);
    final String filename = fileNamePrefix + "-" + timestamp + ".csv";
    response.header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(filename));

    return response.entity(new StreamingOutput()
    {
      @Override
      public void write(final OutputStream os) throws IOException, WebApplicationException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));
        writer.write(headerLine);
        for (CsvWritable csvWritable : results) {
          writer.write("\r\n");
          writer.write(csvWritable.toCsvLine());
        }
        writer.flush();
      }
    });
  }
}
