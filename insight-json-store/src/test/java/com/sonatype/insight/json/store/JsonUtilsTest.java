/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    final int[] pi = { 3, 1, 4, 5, 9 };

    assertThat(JsonUtils.parse(Arrays.toString(pi), int[].class)).isEqualTo(pi);
    assertThat(JsonUtils.parse("{\"aaData\":" + Arrays.toString(pi) + "}", int[].class)).isEqualTo(pi);

    final List<String> words = Arrays.asList("This", "is", "a", "test");
    final ArrayNode tree = JsonUtils.asTree(words);

    assertThat(JsonUtils.asTree(JsonUtils.aaData(words)).get("aaData")).isEqualTo(tree);
    assertThat(JsonUtils.aaDataNode(tree).get("aaData")).isEqualTo(tree);
  }

  @Test
  public void testWriteUnformatted() throws Exception {
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
}
