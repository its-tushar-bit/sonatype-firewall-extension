/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactory.Feature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public final class JsonUtils
{
  private static final MappingJsonFactory JSON;

  static {
    JsonFactory src = JsonFactory.builder()
        .configure(Feature.INTERN_FIELD_NAMES, false)
        .build();

    ObjectMapper mapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    JSON = new MappingJsonFactory(src, mapper);
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

  @SuppressWarnings("unchecked")
  public static <T extends ContainerNode<?>> T read(final InputStream stream) throws IOException {
    try (final JsonParser parser = JSON.createParser(stream)) {
      return (T) parser.readValueAsTree();
    }
  }

  public static <T> T read(final File file, final Class<? extends T> type) throws IOException {
    try (final JsonParser parser = JSON.createParser(file)) {
      return parser.readValueAs(type);
    }
  }

  public static <T> T read(final InputStream is, final Class<? extends T> type) throws IOException {
    try (final JsonParser parser = JSON.createParser(is)) {
      return parser.readValueAs(type);
    }
  }

  public static <T> T read(final byte[] b, final Class<? extends T> type) throws IOException {
    try (final JsonParser parser = JSON.createParser(b)) {
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

  public static void write(final OutputStream os, final Object pojo) throws IOException {
    try (final JsonGenerator generator = JSON.createGenerator(os, JsonEncoding.UTF8)) {
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

  public static <T> T parse(final String json, final TypeReference<T> typeReference) throws IOException {
    return parse(json.getBytes(StandardCharsets.UTF_8), typeReference);
  }

  public static <T> T parse(final byte[] bytes, final TypeReference<T> typeReference) throws IOException {
    try (final JsonParser parser = JSON.createParser(bytes)) {
      return parser.readValueAs(typeReference);
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

  /**
   * Streams pretty-printed JSON for the given POJO directly to a file.
   * Unlike {@link #format(Object)} / {@link #generate(Object)}, this does not materialize the JSON
   * as an in-memory {@code String} or {@code byte[]} and therefore is not bounded by the
   * {@code Integer.MAX_VALUE - 8} array-size limit. Use this for potentially large payloads
   * (e.g. support-bundle DB dumps).
   */
  public static void generate(final Object pojo, final File file) throws IOException {
    Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
    try (final OutputStream os = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
      write(os, pojo);
    }
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

  public static <T> T asType(final String json, final TypeReference<T> type) {
    try {
      return parse(json, type);
    }
    catch (IOException e) {
      throw new UncheckedIOException(String.format("Failed to parse JSON: %s", e.getMessage()), e);
    }
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
      if (!child.isNull()) {
        result.add(child.asText());
      }
    }
    return result;
  }

  public static Set<String> getStringSetFromArray(JsonNode arrayNode) {
    if (arrayNode != null && arrayNode.isArray() && !arrayNode.isEmpty()) {
      Set<String> results = new LinkedHashSet<>();
      for (JsonNode jsonNode : arrayNode) {
        if (jsonNode != null && !jsonNode.isNull()) {
          String result = jsonNode.asText();
          if (result != null) {
            results.add(jsonNode.asText());
          }
        }
      }
      if (!results.isEmpty()) {
        return results;
      }
    }
    return null;
  }

  public static String getTypeToString(JsonNode jsonNode, Class<?> type) {
    try {
      return JsonUtils.asPojo(jsonNode, type).toString();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  public static <T> Set<T> getObjectSetFromArray(JsonNode arrayNode, Class<T> type) {
    if (arrayNode != null && arrayNode.isArray() && !arrayNode.isEmpty()) {
      Set<T> results = new LinkedHashSet<>();
      for (JsonNode jsonNode : arrayNode) {
        if (jsonNode != null && !jsonNode.isNull()) {
          T result;
          try {
            result = JsonUtils.asPojo(jsonNode, type);
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
          if (result != null) {
            results.add(result);
          }
        }
      }
      if (!results.isEmpty()) {
        return results;
      }
    }
    return null;
  }

  /**
   * Receives a JSON object and iterates over its fields. Replaces the value of any occurrence
   * of {@code fieldNameToEmpty} with an empty array.
   * If the field had another type of content inside, it will transform it to an empty array.
   *
   * <p>
   * For example, given the following JSON object and {@code fieldNameToEmpty} as "field2":
   *
   * <pre>
   * {
   *     "field1": ["field1_value"],
   *     "field2": ["field2_value"],
   *     "field3": {
   *         "field31": "field31_value",
   *         "field32": "field32_value",
   *         "field2": ["field2_value"]
   *     }
   * }
   * </pre>
   *
   * The method will return:
   *
   * <pre>
   * {
   *     "field1": ["field1_value"],
   *     "field2": [],
   *     "field3": {
   *         "field31": "field31_value",
   *         "field32": "field32_value",
   *         "field2": []
   *     }
   * }
   * </pre>
   *
   * @param jsonByteBuffer The JSON object to transform.
   * @param fieldNameToEmpty The name of the field whose value should be replaced with an empty array.
   * @return The modified JSON object.
   */
  public static byte[] setFieldToEmptyArray(byte[] jsonByteBuffer, String fieldNameToEmpty) {
    try (ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(jsonByteBuffer),
            StandardCharsets.UTF_8));
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8)))
    {
      iterateJsonSetArrayToEmpty(reader, writer, fieldNameToEmpty);
      writer.close();
      reader.close();
      return outStream.toByteArray();
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to process json to remove field streams", e);
    }
  }

  public static <T, U> U convertValue(final T input, final Class<U> clazz) {
    return JSON.getCodec().convertValue(input, clazz);
  }

  private static void iterateJsonSetArrayToEmpty(
      JsonReader reader,
      JsonWriter writer,
      String fieldNameToExclude) throws IOException
  {
    JsonToken token = reader.peek();

    switch (token) {
      case BEGIN_ARRAY:
        reader.beginArray();
        writer.beginArray();
        while (reader.hasNext()) {
          iterateJsonSetArrayToEmpty(reader, writer, fieldNameToExclude);
        }
        reader.endArray();
        writer.endArray();
        break;

      case BEGIN_OBJECT:
        reader.beginObject();
        writer.beginObject();
        while (reader.hasNext()) {
          String name = reader.nextName();
          if (name.equals(fieldNameToExclude)) {
            reader.skipValue();
            writer.name(fieldNameToExclude).beginArray().endArray();
          }
          else {
            writer.name(name);
            iterateJsonSetArrayToEmpty(reader, writer, fieldNameToExclude);
          }
        }
        reader.endObject();
        writer.endObject();
        break;

      case STRING:
        writer.value(reader.nextString());
        break;

      case NUMBER:
        writer.value(reader.nextDouble());
        break;

      case BOOLEAN:
        writer.value(reader.nextBoolean());
        break;

      case NULL:
        reader.nextNull();
        writer.nullValue();
        break;

      default:
        throw new IllegalStateException("Unexpected JSON token: " + token);
    }
  }
}
