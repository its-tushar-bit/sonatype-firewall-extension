/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class ISODateSerializer
    extends JsonSerializer<Date>
{
  private final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

  @Override
  public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
      JsonProcessingException
  {
    if (value != null) {
      jgen.writeString(formatter.print(value.getTime()));
    }
    else {
      jgen.writeNull();
    }
  }
}
