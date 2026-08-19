/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@Named
@Provider
@Produces("text/csv")
public class CSVMessageBodyWriter
    implements MessageBodyWriter<Object>
{
  private final CsvMapper csvMapper;

  @Inject
  public CSVMessageBodyWriter(CsvMapper csvMapper) {
    this.csvMapper = csvMapper;
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return mediaType.toString().equals("text/csv");
  }

  /**
   * This method is deprecated/unused in Jersey 2, and returning -1 is recommended.
   */
  @Override
  public long getSize(Object obj, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(
      Object obj,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream) throws IOException
  {
    JavaType javaType = csvMapper.getTypeFactory().constructType(genericType);

    if (javaType.isContainerType()) {
      javaType = javaType.getContentType();
    }

    CsvSchema schema = csvMapper.schemaFor(javaType).withHeader();
    csvMapper.writer(schema).writeValue(entityStream, obj);
  }
}
