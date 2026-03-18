/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class JsonUtilsTest
{
  @Test
  public void testIsNull() throws IOException {
    assertThat(JsonUtils.isNull(null)).isTrue();
    assertThat(JsonUtils.isNull(NullNode.getInstance())).isTrue();

    JsonNode jsonNode = JsonUtils.parse("{\"a\":\"text value\",\"b\":null}");
    assertThat(jsonNode).isNotNull();
    assertThat(JsonUtils.isNull(jsonNode)).isFalse();
    assertThat(JsonUtils.isNull(jsonNode.get("a"))).isFalse();
    assertThat(JsonUtils.isNull(jsonNode.get("b"))).isTrue();
    assertThat(JsonUtils.isNull(jsonNode.get("c"))).isTrue();
  }

  @Test
  public void testAAData() throws IOException {
    final int[] pi = {3, 1, 4, 5, 9};

    assertThat(JsonUtils.parse(Arrays.toString(pi), int[].class)).isEqualTo(pi);
    assertThat(JsonUtils.parse("{\"aaData\":" + Arrays.toString(pi) + "}", int[].class)).isEqualTo(pi);

    final List<String> words = Arrays.asList("This", "is", "a", "test");
    final ArrayNode tree = JsonUtils.asTree(words);

    assertThat(JsonUtils.asTree(JsonUtils.aaData(words)).get("aaData")).isEqualTo(tree);
    assertThat(JsonUtils.aaDataNode(tree).get("aaData")).isEqualTo(tree);
  }

  @Test
  public void testWriteUnformatted() {
    Map<String, String> pojo = new HashMap<>();
    pojo.put("groupId", "tomcat");// throw in a unicode character just for the hell of it
    pojo.put("artifactId", "tomcat-util");
    pojo.put("version", "5.5.23");

    String expectedJson = "{\"groupId\":\"tomcat\uF8FF\",\"artifactId\":\"tomcat-util\",\"version\":\"5.5.23\"}";
    assertThat(JsonUtils.writeUnformatted(pojo)).isEqualTo(expectedJson);
  }

  @Test
  public void testGetStringListFromArray_withNullValues() throws IOException {
    JsonNode jsonNode = JsonUtils.parse("[null, \"value\"]");
    assertThat(JsonUtils.getStringListFromArray(jsonNode)).containsOnly("value");
  }

  @Test
  public void testGetStringSetFromArray_Null() {
    assertThat(JsonUtils.getStringSetFromArray(null)).isNull();
  }

  @Test
  public void testGetStringSetFromArray_NotArray() {
    assertThat(JsonUtils.getStringSetFromArray(new ObjectMapper().createObjectNode())).isNull();
  }

  @Test
  public void testGetStringSetFromArray_EmptyArray() {
    assertThat(JsonUtils.getStringSetFromArray(new ObjectMapper().createArrayNode())).isNull();
  }

  @Test
  public void testGetStringSetFromArray_NullElement() {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    arrayNode.add((Integer) null);
    arrayNode.add((JsonNode) null);

    assertThat(JsonUtils.getStringSetFromArray(arrayNode)).isNull();
  }

  @Test
  public void testGetStringSetFromArray() {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode node = objectMapper.createArrayNode();
    node.add("value3");
    node.add("value1");
    node.add("value2");
    node.add("value1");

    assertThat(JsonUtils.getStringSetFromArray(node)).containsExactly("value3", "value1", "value2");
  }

  @Test
  public void testGetTypeToString() {
    Pair pair = new Pair("key", "value");
    JsonNode jsonNode = JsonUtils.asTree(pair);

    assertThat(JsonUtils.getTypeToString(jsonNode, Pair.class)).isEqualTo("key=value");
  }

  @Test
  public void testGetObjectSetFromArray_Null() {
    assertThat(JsonUtils.getObjectSetFromArray(null, Pair.class)).isNull();
  }

  @Test
  public void testGetObjectSetFromArray_NotArray() {
    assertThat(JsonUtils.getObjectSetFromArray(new ObjectMapper().createObjectNode(), Pair.class)).isNull();
  }

  @Test
  public void testGetObjectSetFromArray_EmptyArray() {
    assertThat(JsonUtils.getObjectSetFromArray(new ObjectMapper().createArrayNode(), Pair.class)).isNull();
  }

  @Test
  public void testGetObjectSetFromArray_NullElement() {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    arrayNode.add((Integer) null);
    arrayNode.add((JsonNode) null);

    assertThat(JsonUtils.getObjectSetFromArray(arrayNode, Pair.class)).isNull();
  }

  @Test
  public void testGetObjectSetFromArray_ElementWrongType() {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    arrayNode.add(5);

    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> JsonUtils.getObjectSetFromArray(arrayNode, Pair.class));
  }

  @Test
  public void testGetObjectSetFromArray() {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    Pair pair1 = new Pair("a", "b");
    Pair pair2 = new Pair("c", "d");
    Pair pair3 = new Pair("e", "f");
    arrayNode.add(JsonUtils.asTree(pair1));
    arrayNode.add(JsonUtils.asTree(pair2));
    arrayNode.add(JsonUtils.asTree(pair3));
    arrayNode.add(JsonUtils.asTree(pair2));

    Set<Pair> pairs = JsonUtils.getObjectSetFromArray(arrayNode, Pair.class);
    assertThat(pairs).containsExactly(pair1, pair2, pair3);
  }

  @Test
  public void testSetFieldToEmptyArray() {
    String jsonInput = "{\"name\":\"John\",\"last_name\":\"Doe\",\"toRemove\":[\"some_value1\", \"some_value2\"]," +
        "\"details\":{\"city\":\"New York\",\"toRemove\":[\"some_value1\", \"some_value2\"]}}";
    String expectedOutput = "{\"name\":\"John\",\"last_name\":\"Doe\",\"toRemove\":[]," +
        "\"details\":{\"city\":\"New York\",\"toRemove\":[]}}";
    byte[] modifiedJson = JsonUtils.setFieldToEmptyArray(jsonInput.getBytes(), "toRemove");
    assertThat(expectedOutput.getBytes()).containsExactly(modifiedJson);
  }

  @Test
  public void testSetFieldToEmptyArray_MalformedJson() {
    String jsonInputWrongFormatted = "{\"name\":\"John\",\"age\":30.0,\"toRemove\":\"[s\"ome_value1, some_value2]\"," +
        "\"details\":{\"city\":\"New York\",\"toRemove\":\"[another_value1, another_value2]\"}}";
    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> JsonUtils.setFieldToEmptyArray(jsonInputWrongFormatted.getBytes(), "toRemove"));
  }

  @Test
  public void testAsType() {
    String jsonInput = "{\"name\":\"John\"}";
    Map<String, String> type = JsonUtils.asType(jsonInput, new TypeReference<Map<String, String>>()
    {
    });
    assertThat(type.get("name")).isEqualTo("John");
  }

  @Test
  public void testAsType_MalformedJson() {
    String jsonInput = "{\"name\":\"John";
    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> JsonUtils.asType(jsonInput, new TypeReference<Map<String, String>>()
        {
        }));
  }

  @Test
  public void testJsonMapper_UnknownProperties() {
    String json = "{\"a\":\"a\", \"b\":\"b\", \"c\":\"c\"}"; // attribute c is unknown in class Pair
    Pair pair = JsonUtils.asType(json, new TypeReference<Pair>()
    {
    });
    assertThat(pair.a).isEqualTo("a");
    assertThat(pair.b).isEqualTo("b");
  }

  private static class Pair
  {
    public Object a;

    public Object b;

    @SuppressWarnings("unused")
    public Pair() {
      // for jackson
    }

    public Pair(Object a, Object b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public String toString() {
      return a.toString() + "=" + b.toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Pair pair = (Pair) o;
      return Objects.equals(a, pair.a) && Objects.equals(b, pair.b);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b);
    }
  }
}
