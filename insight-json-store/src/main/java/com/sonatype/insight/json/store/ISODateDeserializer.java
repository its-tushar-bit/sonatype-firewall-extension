/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ISODateDeserializer
    extends StdDeserializer<Date>
{
  private static final long serialVersionUID = 1L;

  protected ISODateDeserializer() {
    this(null);
  }

  protected ISODateDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Date deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
    String date = parser.getText();
    return Date.from(Instant.from(ISODateSerializer.formatter.parse(date)));
  }
}
