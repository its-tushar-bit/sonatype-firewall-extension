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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JsonUtilsTest
{
  @Test
  public void testIsNull() throws IOException {
    assertTrue(JsonUtils.isNull(null));
    assertTrue(JsonUtils.isNull(NullNode.getInstance()));

    JsonNode jsonNode = JsonUtils.parse("{\"a\":\"text value\",\"b\":null}");
    assertNotNull(jsonNode);
    assertFalse(JsonUtils.isNull(jsonNode));
    assertFalse(JsonUtils.isNull(jsonNode.get("a")));
    assertTrue(JsonUtils.isNull(jsonNode.get("b")));
    assertTrue(JsonUtils.isNull(jsonNode.get("c")));
  }

  @Test
  public void testAAData() throws IOException {
    final int[] pi = { 3, 1, 4, 5, 9 };

    assertArrayEquals(pi, JsonUtils.parse(Arrays.toString(pi), int[].class));
    assertArrayEquals(pi, JsonUtils.parse("{\"aaData\":" + Arrays.toString(pi) + "}", int[].class));

    final List<String> words = Arrays.asList("This", "is", "a", "test");
    final ArrayNode tree = JsonUtils.asTree(words);

    assertEquals(tree, JsonUtils.asTree(JsonUtils.aaData(words)).get("aaData"));
    assertEquals(tree, JsonUtils.aaDataNode(tree).get("aaData"));
  }

  @Test
  public void testWriteUnformatted() throws Exception {
    Map<String, String> pojo = new HashMap<>();
    pojo.put("groupId", "tomcat");// throw in a unicode character just for the hell of it
    pojo.put("artifactId", "tomcat-util");
    pojo.put("version", "5.5.23");

    String expectedJson = "{\"groupId\":\"tomcat\uF8FF\",\"artifactId\":\"tomcat-util\",\"version\":\"5.5.23\"}";
    assertThat(JsonUtils.writeUnformatted(pojo), is(expectedJson));
  }
}
