/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CSVMessageBodyWriterTest
{
  private CSVMessageBodyWriter writer;

  @SuppressWarnings("unused")
  private static class DTO
  {
    public int foo;

    public String bar;

    public DTO(int foo, String bar) {
      this.foo = foo;
      this.bar = bar;
    }
  }

  /**
   * Aides in the construction of a Type object for ArrayList<DTO>
   */
  @SuppressWarnings("serial")
  private static class DTOArrayListTypeHelper
      extends ArrayList<DTO>
  {
  }

  @Before
  public void setup() {
    writer = new CSVMessageBodyWriter(new CsvMapper());
  }

  @Test
  public void testIsWriteable() {
    assertThat(writer.isWriteable(null, null, null, new MediaType("text", "csv"))).isTrue();
    assertThat(writer.isWriteable(null, null, null, MediaType.TEXT_PLAIN_TYPE)).isFalse();
    assertThat(writer.isWriteable(null, null, null, MediaType.APPLICATION_JSON_TYPE)).isFalse();
  }

  @Test
  public void testWriteTo_SimpleObject() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    DTO dto = new DTO(4, "asdf qwerty\"'\"");

    writer.writeTo(dto, DTO.class, DTO.class, null, null, null, outputStream);

    String writtenOutput = outputStream.toString();

    // double quotes in quoted strings are represented as two double quotes in succession
    assertThat(writtenOutput).isEqualTo("bar,foo\n\"asdf qwerty\"\"'\"\"\",4\n");
  }

  @Test
  public void testWriteTo_ContainerOfSimpleObjects() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<DTO> toSerialize = new ArrayList<>(3);
    toSerialize.add(new DTO(1, "asdf"));
    toSerialize.add(new DTO(-500, ""));
    toSerialize.add(new DTO(0, null));

    Type genericType = DTOArrayListTypeHelper.class.getGenericSuperclass();

    writer.writeTo(toSerialize, ArrayList.class, genericType, null, null, null, outputStream);

    String writtenOutput = outputStream.toString("UTF-8");

    assertThat(writtenOutput).isEqualTo("bar,foo\nasdf,1\n,-500\n,0\n");
  }
}
