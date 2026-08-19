/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditingTest
    extends AbstractDbDAOTest
{
  @TempDir
  public File temporaryFolder;

  private JsonStore store;

  @BeforeEach
  public void setUp() {
    store = new JsonFileStore(new File(temporaryFolder, "audit-test"), "ownerId", clusterLockManager);
  }

  @Test
  public void testFilteredNamedAuditFeed() throws IOException {
    final String addition1 =
        "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String result = "{ \"aaData\" : [ "
        + "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\","
        + " \"ip\" : \"192.168.1.8\", \"where\" : \"home\"," + " \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\","
        + " \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" } ] }";

    store.commit("sample.json",
        JsonUtils.stamp("anon", "127.0.0.1", "office", JsonUtils.parse(addition1.getBytes(StandardCharsets.UTF_8))));

    store.commit("sample.json",
        JsonUtils.stamp("test", "192.168.1.8", "home", JsonUtils.parse(addition2.getBytes(StandardCharsets.UTF_8))));

    final byte[] buf = JsonUtils
        .generate(store.history(JsonUtils.parse("{\"id\":\"B\"}".getBytes(StandardCharsets.UTF_8)), "sample.json"));

    assertThat(new String(buf, StandardCharsets.UTF_8).replaceAll("\"time\" : [0-9]+", "\"time\" : 0"))
        .isEqualToIgnoringWhitespace(result);
  }

  @Test
  public void testFilteredAuditFeed() throws IOException {
    final String addition1 =
        "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String addition3 = "[ { \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\" } ]";

    final String result = "{ \"aaData\" : [ "
        + "{ \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\", \"time\" : 0, \"user\" : \"test\","
        + " \"ip\" : \"127.0.0.1\", \"where\" : \"cafe\", \"filename\" : \"another.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\","
        + " \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\","
        + " \"where\" : \"office\", \"filename\" : \"sample.json\" }" + " ] }";

    store.commit("sample.json",
        JsonUtils.stamp("anon", "127.0.0.1", "office", JsonUtils.parse(addition1.getBytes(StandardCharsets.UTF_8))));

    store.commit("sample.json",
        JsonUtils.stamp("test", "192.168.1.8", "home", JsonUtils.parse(addition2.getBytes(StandardCharsets.UTF_8))));

    store.commit("another.json",
        JsonUtils.stamp("test", "127.0.0.1", "cafe", JsonUtils.parse(addition3.getBytes(StandardCharsets.UTF_8))));

    final byte[] buf = JsonUtils
        .generate(store.history(JsonUtils.parse("{\"id\":\"B\"}".getBytes(StandardCharsets.UTF_8))));

    assertThat(new String(buf, StandardCharsets.UTF_8).replaceAll("\"time\" : [0-9]+", "\"time\" : 0"))
        .isEqualToIgnoringWhitespace(result);
  }

  @Test
  public void testAuditFeed() throws IOException {
    final String addition1 =
        "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String addition3 = "[ { \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\" } ]";

    final String result = "{ \"aaData\" : [ "
        + "{ \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\", \"time\" : 0, \"user\" : \"test\","
        + " \"ip\" : \"127.0.0.1\", \"where\" : \"cafe\", \"filename\" : \"another.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\","
        + " \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"A\", \"override\" : \"EPL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\","
        + " \"where\" : \"office\", \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\","
        + " \"where\" : \"office\", \"filename\" : \"sample.json\" }" + " ] }";

    store.commit("sample.json",
        JsonUtils.stamp("anon", "127.0.0.1", "office", JsonUtils.parse(addition1.getBytes(StandardCharsets.UTF_8))));

    store.commit("sample.json",
        JsonUtils.stamp("test", "192.168.1.8", "home", JsonUtils.parse(addition2.getBytes(StandardCharsets.UTF_8))));

    store.commit("another.json",
        JsonUtils.stamp("test", "127.0.0.1", "cafe", JsonUtils.parse(addition3.getBytes(StandardCharsets.UTF_8))));

    final byte[] buf = JsonUtils.generate(store.history(null));

    assertThat(new String(buf, StandardCharsets.UTF_8).replaceAll("\"time\" : [0-9]+", "\"time\" : 0"))
        .isEqualToIgnoringWhitespace(result);
  }

  @Test
  public void testEmptyAuditFeed() throws IOException {
    assertThat(store.history(null, "")).isNull();
  }
}
