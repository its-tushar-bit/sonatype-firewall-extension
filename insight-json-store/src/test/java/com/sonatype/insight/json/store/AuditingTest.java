/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.json.store;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertNull;

@SuppressWarnings("boxing")
public class AuditingTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder(new File("target"));

  private JsonStore store;

  @Before
  public void setUp() {
    store = new JsonFileStore(FileUtils.createTempFile("audit", "test", temporaryFolder.getRoot()));
  }

  @Test
  public void testNoAugmentedData() throws IOException {
    final String table = "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\" }, { \"id\" : \"C\" } ] }";

    final byte[] buf = JsonUtils.generate(store.augment(JsonUtils.parse(table.getBytes("UTF-8")), "sample.json"));

    assertThat(new String(buf, "UTF-8"), equalToIgnoringWhiteSpace(table));

    assertThat(store.modificationCount(), equalTo(0));
  }

  @Test
  public void testSingleAugmentedData() throws IOException {
    assertThat(store.modificationCount(), equalTo(0));

    final String table = "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\" }, { \"id\" : \"C\" } ] }";

    final String addition = "[ { \"id\" : \"B\", \"override\" : \"EPL\", \"comment\" : \"Testing...\" } ]";

    final String result = "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\", \"override\" : \"EPL\", \"comment\" : \"Testing...\" }, { \"id\" : \"C\" } ] }";

    store.commit("sample.json", JsonUtils.parse(addition.getBytes("UTF-8")));

    final byte[] buf = JsonUtils.generate(store.augment(JsonUtils.parse(table.getBytes("UTF-8")), "sample.json"));

    assertThat(new String(buf, "UTF-8"), equalToIgnoringWhiteSpace(result));

    assertThat(store.modificationCount(), equalTo(1));
  }

  @Test
  public void testMultipleAugmentedData() throws IOException {
    assertThat(store.modificationCount(), equalTo(0));

    final String table = "{ \"aaData\" : [ { \"id\" : \"A\" }, { \"id\" : \"B\" }, { \"id\" : \"C\" } ] }";

    final String addition1 = "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String result = "{ \"aaData\" : [ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" }, { \"id\" : \"C\" } ] }";

    store.commit("sample.json", JsonUtils.parse(addition1.getBytes("UTF-8")));

    assertThat(store.modificationCount(), equalTo(1));

    store.commit("sample.json", JsonUtils.parse(addition2.getBytes("UTF-8")));

    assertThat(store.modificationCount(), equalTo(2));

    final byte[] buf = JsonUtils.generate(store.augment(JsonUtils.parse(table.getBytes("UTF-8")), "sample.json"));

    assertThat(new String(buf, "UTF-8"), equalToIgnoringWhiteSpace(result));
  }

  @Test
  public void testCanAugmentSimpleObject() throws IOException {
    assertThat(store.modificationCount(), equalTo(0));

    final String object = "{ \"id\" : \"B\" }";

    final String addition1 = "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String result = "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" }";

    store.commit("sample.json", JsonUtils.parse(addition1.getBytes("UTF-8")));

    assertThat(store.modificationCount(), equalTo(1));

    store.commit("sample.json", JsonUtils.parse(addition2.getBytes("UTF-8")));

    assertThat(store.modificationCount(), equalTo(2));

    final byte[] buf = JsonUtils.generate(store.augment(JsonUtils.parse(object.getBytes("UTF-8")), "sample.json"));

    assertThat(new String(buf, "UTF-8"), equalToIgnoringWhiteSpace(result));
  }

  @Test
  public void testFilteredNamedAuditFeed() throws IOException {
    final String addition1 = "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String result = "{ \"aaData\" : [ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" } ] }";

    store.commit("sample.json",
        JsonUtils.stamp("anon", "127.0.0.1", "office", JsonUtils.parse(addition1.getBytes("UTF-8"))));

    store.commit("sample.json",
        JsonUtils.stamp("test", "192.168.1.8", "home", JsonUtils.parse(addition2.getBytes("UTF-8"))));

    final byte[] buf = JsonUtils.generate(store.history(JsonUtils.parse("{\"id\":\"B\"}".getBytes("UTF-8")),
        "sample.json"));

    assertThat(new String(buf, "UTF-8").replaceAll("\"time\" : [0-9]+", "\"time\" : 0"),
        equalToIgnoringWhiteSpace(result));
  }

  @Test
  public void testFilteredAuditFeed() throws IOException {
    final String addition1 = "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String addition3 = "[ { \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\" } ]";

    final String result = "{ \"aaData\" : [ "
        + "{ \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"cafe\", \"filename\" : \"another.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" }"
        + " ] }";

    store.commit("sample.json",
        JsonUtils.stamp("anon", "127.0.0.1", "office", JsonUtils.parse(addition1.getBytes("UTF-8"))));

    store.commit("sample.json",
        JsonUtils.stamp("test", "192.168.1.8", "home", JsonUtils.parse(addition2.getBytes("UTF-8"))));

    store.commit("another.json",
        JsonUtils.stamp("test", "127.0.0.1", "cafe", JsonUtils.parse(addition3.getBytes("UTF-8"))));

    final byte[] buf = JsonUtils.generate(store.history(JsonUtils.parse("{\"id\":\"B\"}".getBytes("UTF-8"))));

    assertThat(new String(buf, "UTF-8").replaceAll("\"time\" : [0-9]+", "\"time\" : 0"),
        equalToIgnoringWhiteSpace(result));
  }

  @Test
  public void testAuditFeed() throws IOException {
    final String addition1 = "[ { \"id\" : \"A\", \"override\" : \"EPL\" }, { \"id\" : \"B\", \"override\" : \"APL\" } ]";

    final String addition2 = "[ { \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\" } ]";

    final String addition3 = "[ { \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\" } ]";

    final String result = "{ \"aaData\" : [ "
        + "{ \"id\" : \"B\", \"confirmed\" : true, \"comment\" : \"Must fix\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"127.0.0.1\", \"where\" : \"cafe\", \"filename\" : \"another.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"ASL\", \"comment\" : \"Fix typo\", \"time\" : 0, \"user\" : \"test\", \"ip\" : \"192.168.1.8\", \"where\" : \"home\", \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"A\", \"override\" : \"EPL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" }, "
        + "{ \"id\" : \"B\", \"override\" : \"APL\", \"time\" : 0, \"user\" : \"anon\", \"ip\" : \"127.0.0.1\", \"where\" : \"office\", \"filename\" : \"sample.json\" }"
        + " ] }";

    store.commit("sample.json",
        JsonUtils.stamp("anon", "127.0.0.1", "office", JsonUtils.parse(addition1.getBytes("UTF-8"))));

    store.commit("sample.json",
        JsonUtils.stamp("test", "192.168.1.8", "home", JsonUtils.parse(addition2.getBytes("UTF-8"))));

    store.commit("another.json",
        JsonUtils.stamp("test", "127.0.0.1", "cafe", JsonUtils.parse(addition3.getBytes("UTF-8"))));

    final byte[] buf = JsonUtils.generate(store.history(null));

    assertThat(new String(buf, "UTF-8").replaceAll("\"time\" : [0-9]+", "\"time\" : 0"),
        equalToIgnoringWhiteSpace(result));
  }

  @Test
  public void testEmptyAuditFeed() throws IOException {
    assertNull(store.history(null, ""));
  }
}
