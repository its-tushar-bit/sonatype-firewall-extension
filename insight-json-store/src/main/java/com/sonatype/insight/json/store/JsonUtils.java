/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory.Feature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonUtils
{
  private static final MappingJsonFactory JSON = new MappingJsonFactory();

  static {
    JSON.getCodec().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    JSON.disable(Feature.INTERN_FIELD_NAMES);
  }

  public static JsonStore fileStore(final File folder) {
    return new JsonFileStore(folder);
  }

  public static ObjectNode stamp(final String user, final String ip, final String where, final ContainerNode<?> data) {
    final ObjectNode stampedData = objectNode(data);
    stampedData.put("time", System.currentTimeMillis());
    stampedData.put("user", user);
    stampedData.put("ip", ip);
    stampedData.put("where", where);
    stampedData.set("data", data);
    return stampedData;
  }

  @SuppressWarnings("unchecked")
  public static <T extends ContainerNode<?>> T read(final File file) throws IOException {
    try (final JsonParser parser = JSON.createParser(file)) {
      return (T) parser.readValueAsTree();
    }
  }

  public static <T> T read(final File file, final Class<? extends T> type) throws IOException {
    try (final JsonParser parser = JSON.createParser(file)) {
      return parser.readValueAs(type);
    }
  }

  public static void write(final File file, final JsonNode data) throws IOException {
    Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
    try (final JsonGenerator generator = JSON.createGenerator(file, JsonEncoding.UTF8)) {
      generator.useDefaultPrettyPrinter().writeTree(data);
    }
  }

  public static void write(final File file, final Object pojo) throws IOException {
    Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
    try (final JsonGenerator generator = JSON.createGenerator(file, JsonEncoding.UTF8)) {
      generator.useDefaultPrettyPrinter().writeObject(pojo);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends ContainerNode<?>> T parse(final byte[] buf) throws IOException {
    try (final JsonParser parser = JSON.createParser(buf)) {
      return (T) parser.readValueAsTree();
    }
  }

  public static <T> T parse(final byte[] buf, final Class<? extends T> type) throws IOException {
    try (final JsonParser parser = JSON.createParser(buf)) {
      try {
        return parser.readValueAs(type);
      }
      catch (final JsonMappingException e) {
        if (type.isArray()) {
          try {
            // handle situation where array is actually wrapped inside root 'aaData' property
            return JSON.getCodec().reader().withRootName("aaData").readValue(parser, type);
          }
          catch (final JsonMappingException ignore) {
            // no luck, fall-through and throw the original parsing exception
          }
        }
        throw e;
      }
    }
  }

  public static <T extends ContainerNode<?>> T parse(final String json) throws IOException {
    return parse(json.getBytes(StandardCharsets.UTF_8));
  }

  public static <T> T parse(final String json, final Class<? extends T> type) throws IOException {
    return parse(json.getBytes(StandardCharsets.UTF_8), type);
  }

  public static <T> T parse(final InputStream stream, final Class<? extends T> type) throws IOException {
    try (final JsonParser parser = JSON.createParser(stream)) {
      return parser.readValueAs(type);
    }
  }

  public static byte[] generate(final JsonNode data) throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try (final JsonGenerator generator = JSON.createGenerator(os, JsonEncoding.UTF8)) {
      generator.useDefaultPrettyPrinter().writeTree(data);
    }
    return os.toByteArray();
  }

  public static byte[] generate(final Object pojo) throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try (final JsonGenerator generator = JSON.createGenerator(os, JsonEncoding.UTF8)) {
      generator.useDefaultPrettyPrinter().writeObject(pojo);
    }
    return os.toByteArray();
  }

  public static String format(Object pojo) {
    try {
      return new String(generate(pojo), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String writeUnformatted(Object pojo) {
    try {
      return JSON.getCodec().writeValueAsString(pojo);
    }
    catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static <T extends ContainerNode<?>> T asTree(final Object pojo) {
    return JSON.getCodec().valueToTree(pojo);
  }

  public static <T> T asPojo(final JsonNode tree, final Class<? extends T> type) throws IOException {
    return JSON.getCodec().treeToValue(tree, type);
  }

  public static ArrayNode arrayNode(final ContainerNode<?> data) {
    return data != null ? data.arrayNode() : JsonNodeFactory.instance.arrayNode();
  }

  public static ObjectNode objectNode(final ContainerNode<?> data) {
    return data != null ? data.objectNode() : JsonNodeFactory.instance.objectNode();
  }

  public static ObjectNode aaDataNode(final Iterable<JsonNode> data) {
    final ArrayNode aaData = JsonNodeFactory.instance.arrayNode();
    for (final JsonNode d : data) {
      aaData.add(d);
    }
    return (ObjectNode) aaData.objectNode().set("aaData", aaData);
  }

  public static <T> Object aaData(final Iterable<T> data) {
    return Collections.singletonMap("aaData", data);
  }

  public static Float getNullableFloat(final JsonNode jsonNode) {
    if (isNull(jsonNode)) {
      return null;
    }
    return (float) jsonNode.asDouble();
  }

  public static String getNullableString(final JsonNode jsonNode) {
    if (isNull(jsonNode)) {
      return null;
    }
    return jsonNode.asText();
  }

  public static boolean isNull(final JsonNode jsonNode) {
    return jsonNode == null || jsonNode instanceof NullNode;
  }

  public static List<String> getStringListFromArray(final JsonNode jsonNode) {
    if (JsonUtils.isNull(jsonNode)) {
      return null;
    }
    final ArrayNode jsonArray = (ArrayNode) jsonNode;
    if (jsonArray.size() == 0) {
      return null;
    }
    final List<String> result = new ArrayList<>();
    for (final JsonNode child : jsonArray) {
      result.add(child.asText());
    }
    return result;
  }
}
