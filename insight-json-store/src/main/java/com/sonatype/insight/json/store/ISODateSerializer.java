/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ISODateSerializer
    extends JsonSerializer<Date>
{
  // DateTimeFormatter.ISO_OFFSET_DATE_TIME works fine, but nanoseconds become optional when in zeros.
  // Using this pattern instead to keep it compatible with the original code using Joda time to always print nanoseconds
  static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneId.systemDefault());

  @Override
  public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
    if (value != null) {
      jgen.writeString(formatter.format(value.toInstant()));
    }
    else {
      jgen.writeNull();
    }
  }
}
