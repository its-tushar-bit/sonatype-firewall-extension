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
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonUtilsTest
{
  @Test
  public void testIsNull() throws IOException {
    Assert.assertTrue(JsonUtils.isNull(null));
    Assert.assertTrue(JsonUtils.isNull(NullNode.getInstance()));

    JsonNode jsonNode = JsonUtils.parse("{\"a\":\"text value\",\"b\":null}");
    Assert.assertNotNull(jsonNode);
    Assert.assertFalse(JsonUtils.isNull(jsonNode));
    Assert.assertFalse(JsonUtils.isNull(jsonNode.get("a")));
    Assert.assertTrue(JsonUtils.isNull(jsonNode.get("b")));
    Assert.assertTrue(JsonUtils.isNull(jsonNode.get("c")));
  }

  @Test
  public void testAAData() throws IOException {
    final int[] pi = { 3, 1, 4, 5, 9 };

    Assert.assertArrayEquals(pi, JsonUtils.parse(Arrays.toString(pi), int[].class));
    Assert.assertArrayEquals(pi, JsonUtils.parse("{\"aaData\":" + Arrays.toString(pi) + "}", int[].class));

    final List<String> words = Arrays.asList("This", "is", "a", "test");
    final ArrayNode tree = JsonUtils.asTree(words);

    Assert.assertEquals(tree, JsonUtils.asTree(JsonUtils.aaData(words)).get("aaData"));
    Assert.assertEquals(tree, JsonUtils.aaDataNode(tree).get("aaData"));
  }

  @Test
  public void testWriteUnformatted() throws Exception {
    Map<String, String> pojo = new HashMap<String, String>();
    pojo.put("groupId", "tomcat");// throw in a unicode character just for the hell of it
    pojo.put("artifactId", "tomcat-util");
    pojo.put("version", "5.5.23");

    String expectedJson = "{\"groupId\":\"tomcat\uF8FF\",\"artifactId\":\"tomcat-util\",\"version\":\"5.5.23\"}";
    assertThat(JsonUtils.writeUnformatted(pojo), is(expectedJson));
  }
}
