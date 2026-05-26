/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import org.junit.Test;

public class InsightJacksonMessageBodyProviderTest
{
  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  @Test
  public void shouldReadTextPlainJsonPartsWithConfiguredObjectMapper() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(TestDto.class, new JsonDeserializer<>()
    {
      @Override
      public TestDto deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        parser.skipChildren();
        return new TestDto("configured-mapper");
      }
    });
    mapper.registerModule(module);
    InsightJacksonMessageBodyProvider provider = new InsightJacksonMessageBodyProvider(mapper);

    assertThat(provider.isReadable(TestDto.class, TestDto.class, NO_ANNOTATIONS, MediaType.TEXT_PLAIN_TYPE)).isTrue();

    Object value = provider.readFrom(
        objectClass(TestDto.class),
        TestDto.class,
        NO_ANNOTATIONS,
        MediaType.TEXT_PLAIN_TYPE,
        new MultivaluedHashMap<>(),
        new ByteArrayInputStream("{}".getBytes(UTF_8)));

    assertThat(value).isInstanceOf(TestDto.class);
    assertThat(((TestDto) value).value).isEqualTo("configured-mapper");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Class<Object> objectClass(Class<?> type) {
    return (Class) type;
  }

  private static class TestDto
  {
    private final String value;

    private TestDto(String value) {
      this.value = value;
    }
  }
}
