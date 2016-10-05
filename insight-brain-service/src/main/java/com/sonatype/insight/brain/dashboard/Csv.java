/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

final class Csv
{
  public static ResponseBuilder generate(final ResponseBuilder response, String fileNamePrefix, final String headerLine,
                                         final Collection<? extends CSVWritable> results) throws IOException
  {
    final Date now = new Date();

    response.lastModified(now);
    response.expires(now);
    response.type("text/csv");

    final String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(now);
    final String filename = fileNamePrefix + "-" + timestamp + ".csv";
    response.header("Content-Disposition", "attachment; filename=\"" + filename + '"');

    return response.entity(new StreamingOutput()
    {
      @Override
      public void write(final OutputStream os) throws IOException, WebApplicationException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(os))) {
          writer.write(headerLine);
          for (CSVWritable csvWritable : results) {
            writer.write("\r\n");
            writer.write(csvWritable.toCsvLine());
          }
        }
      }
    });
  }
}
