/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReplacerTest
{
  private static final int ZERO = 0;

  private List<TestUrl> makeUrls(String[] vals) {
    return Arrays.stream(vals).map(s -> {
      TestUrl t = new TestUrl();
      t.setUrl(s);
      return t;
    }).collect(toList());
  }

  @Test
  public void testGenerateUrls_DIRECT_URLS_WithoutReplacementKey() {
    List<TestUrl> safeUrls = makeUrls(new String[] { "some", "safe", "values" });
    int expected = safeUrls.size();
    assertThat(Replacer.DIRECT_URLS.generateUrls(safeUrls).size(), is(expected));
  }

  @Test
  public void testGenerateUrls_DIRECT_URLS_WithReplacementKey() {
    List<TestUrl> safeUrls = makeUrls(new String[] { "some", "safe", "values" });
    List<TestUrl> unsafeUrls = makeUrls(
        new String[] { "unsafe" + Replacer.REPLACE_KEY, "entries" + Replacer.REPLACE_KEY });

    List<TestUrl> testUrls = new ArrayList<>();
    testUrls.addAll(safeUrls);
    testUrls.addAll(unsafeUrls);

    int expected = safeUrls.size();
    assertThat(Replacer.DIRECT_URLS.generateUrls(testUrls).size(), is(expected));
  }

  @Test
  public void testGenerateUrls_EMPTY() {
    List<TestUrl> allUrls = makeUrls(
        new String[] { "some", "safe", "values", "unsafe" + Replacer.REPLACE_KEY, "entries" + Replacer.REPLACE_KEY });

    int expected = ZERO;

    assertThat(Replacer.EMPTY.generateUrls(allUrls).size(), is(expected));
  }

  private Replacer buildReplacer(String key1, String[] vals1, String key2, String[] vals2) {
    Map<String, List<String>> replaceConfig = new HashMap<>();

    if (key1 != null && vals1 != null) {
      replaceConfig.put(key1, Arrays.asList(vals1));
    }

    if (key2 != null && vals2 != null) {
      replaceConfig.put(key2, Arrays.asList(vals2));
    }

    return new Replacer(replaceConfig);
  }

  @Test
  public void testGenerateUrls_SingleReplacement() {
    String replaceKey = "{test}";
    String[] replacements = new String[] { "ONE", "TWO" };

    Replacer replacer = buildReplacer(replaceKey, replacements, null, null);

    TestUrl test = new TestUrl();
    test.setUrl("some/value/" + replaceKey);

    String resultUrl1 = "some/value/" + "ONE";
    TestUrl expected1 = new TestUrl();
    expected1.setUrl(resultUrl1);

    String resultUrl2 = "some/value/" + "TWO";
    TestUrl expected2 = new TestUrl();
    expected2.setUrl(resultUrl2);

    int expected = replacements.length;

    List<TestUrl> results = replacer.generateUrls(test);
    List<String> resultUrls = results.stream().map(u -> u.getUrl()).collect(toList());

    assertThat(results.size(), is(expected));
    assertThat(resultUrls, hasItems(resultUrl1, resultUrl2));
  }

  @Test
  public void testGenerateUrls_SingleReplacementInvalid() {
    String invalidReplaceKey = "{invalid}";
    String replaceKey = "{test}";
    String[] replacements = new String[] { "ONE", "TWO" };

    Replacer replacer = buildReplacer(replaceKey, replacements, null, null);

    TestUrl test = new TestUrl();
    test.setUrl("some/value/" + invalidReplaceKey);

    int expected = ZERO;

    assertThat(replacer.generateUrls(test).size(), is(expected));
  }

  @Test
  public void testGenerateUrls_MultipleReplacement() {
    String replaceKey1 = "{test1}";
    String replaceKey2 = "{test2}";
    String[] replacements1 = new String[] { "A1", "B1" };
    String[] replacements2 = new String[] { "A2", "B2" };

    Replacer replacer = buildReplacer(replaceKey1, replacements1, replaceKey2, replacements2);

    TestUrl test = new TestUrl();
    test.setUrl("some/value/" + replaceKey1 + "/more/values/" + replaceKey2);

    String resultUrl1 = "some/value/" + "A1" + "/more/values/" + "A2";
    TestUrl expected1 = new TestUrl();
    expected1.setUrl(resultUrl1);

    String resultUrl2 = "some/value/" + "B1" + "/more/values/" + "B2";
    TestUrl expected2 = new TestUrl();
    expected2.setUrl(resultUrl2);

    int expected = replacements1.length;

    List<TestUrl> results = replacer.generateUrls(test);
    List<String> resultUrls = results.stream().map(u -> u.getUrl()).collect(toList());

    assertThat(results.size(), is(expected));
    assertThat(resultUrls, hasItems(resultUrl1, resultUrl2));
  }

  @Test
  public void testGenerateUrls_MultipleReplacementInvalid() {
    String invalidReplaceKey = "{invalid}";
    String replaceKey1 = "{test1}";
    String replaceKey2 = "{test2}";
    String[] replacements1 = new String[] { "A1", "B1" };
    String[] replacements2 = new String[] { "A2", "B2" };

    Replacer replacer = buildReplacer(replaceKey1, replacements1, replaceKey2, replacements2);

    TestUrl test = new TestUrl();
    test.setUrl("some/value/" + invalidReplaceKey);

    int expected = ZERO;

    assertThat(replacer.generateUrls(test).size(), is(expected));
  }

  @Test
  public void testGenerateUrls_MultipleReplacementIncomplete() {
    String invalidReplaceKey = "{invalid}";
    String replaceKey1 = "{test1}";
    String replaceKey2 = "{test2}";
    String[] replacements1 = new String[] { "A1", "B1" };
    String[] replacements2 = new String[] { "A2", "B2" };

    Replacer replacer = buildReplacer(replaceKey1, replacements1, replaceKey2, replacements2);

    TestUrl test = new TestUrl();
    test.setUrl("some/value/" + replaceKey1 + "/more/values/" + invalidReplaceKey);

    int expected = ZERO;

    assertThat(replacer.generateUrls(test).size(), is(expected));
  }
}
