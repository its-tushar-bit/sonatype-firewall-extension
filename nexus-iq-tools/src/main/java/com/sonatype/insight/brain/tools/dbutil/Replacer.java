/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import static java.util.stream.Collectors.toList;

public class Replacer
{
  protected static final String REPLACE_KEY = "{";

  protected static final String REPLACE_KEY_JSON = "\"{";

  public static final Replacer DIRECT_URLS = new Replacer()
  {
    @Override
    public List<TestUrl> generateUrls(TestUrl url) {
      List<TestUrl> direct = new ArrayList<>();
      if (!url.getUrl().contains(REPLACE_KEY)
          && (url.getPayload() == null || !url.getPayload().contains(REPLACE_KEY_JSON))) {
        direct.add(url);
      }
      return direct;
    }
  };

  public static final Replacer EMPTY = new Replacer()
  {
    @Override
    public List<TestUrl> generateUrls(TestUrl url) {
      return new ArrayList<>();
    }
  };

  private final Map<String, List<String>> replacements;

  private final int replacementCount;

  Replacer() {
    replacements = null;
    replacementCount = 0;
  }

  public Replacer(Map<String, List<String>> replacements) {
    this.replacements = replacements;

    replacementCount = replacements.values().iterator().next().size();
    for (List<String> list : replacements.values()) {
      if (list.size() != replacementCount) {
        throw new RuntimeException("Replacer: lists for all replacement keys must be of matching length.");
      }
    }
  }

  TestUrl buildUrl(TestUrl source, String newUrl) {
    return buildUrl(source, newUrl, source.getPayload());
  }

  TestUrl buildUrl(TestUrl source, String newUrl, String newPayload) {
    TestUrl cloned = (TestUrl) source.clone();
    cloned.setUrl(newUrl);
    cloned.setPayload(newPayload);
    return cloned;
  }

  public List<TestUrl> generateUrls(List<TestUrl> urls) {
    return urls.stream().map(this::generateUrls).flatMap(List::stream).collect(toList());
  }

  public List<TestUrl> generateUrls(TestUrl url) {
    List<TestUrl> generated = new ArrayList<>();

    if (replacements.keySet().stream().anyMatch(url.getUrl()::contains)) {
      for (int i = 0; i < replacementCount; i++) {

        String targetUrl = url.getUrl();

        for (String key : replacements.keySet()) {
          if (targetUrl.contains(key)) {
            targetUrl = targetUrl.replace(key, replacements.get(key).get(i));
          }
        }

        if (targetUrl.contains(REPLACE_KEY)) {
          continue;
        }

        generated.add(buildUrl(url, targetUrl));
      }
    }

    return generated;
  }
}
