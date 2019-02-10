/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.SortedSet;

import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import static java.util.stream.Collectors.toList;

public class ListReplacer
    extends Replacer
{
  protected static final String MAX = "MAX";

  private static final String TEMPLATE_KEY = "{TEMPLATE_KEY}";

  private final String keyPrefix;

  private final List<String> replacements;

  private final int maxReplace;

  private final int defaultMax;

  public ListReplacer(String keyPrefix, List<String> replacements, int defaultMax) {
    this.keyPrefix = keyPrefix;
    this.replacements = replacements;
    this.maxReplace = replacements.size();
    this.defaultMax = defaultMax;
  }

  private static String quote(String val) {
    return "\"" + val + "\"";
  }

  private int getReplacementCount(String count) {
    int replacementCount;
    if (count.contains(MAX)) {
      String maxAdjust = count.replace(MAX, "").trim();
      int relativeToMax = maxAdjust.length() > 0 ? Integer.parseInt(maxAdjust) : 0;
      replacementCount = maxReplace + relativeToMax;
    }
    else {
      replacementCount = Integer.parseInt(count.trim());
    }
    replacementCount = Math.min(replacementCount, maxReplace);
    replacementCount = Math.max(replacementCount, 0);
    return replacementCount;
  }

  private String buildReplacement(int count, boolean json) {
    return json ? String.join(",", replacements.subList(0, count).stream().map(ListReplacer::quote).collect(toList()))
        : String.join(",", replacements.subList(0, count));
  }

  private List<String> buildReplacementBlocks(String data, boolean json) {
    int prefix = data.indexOf(keyPrefix);
    int suffix = data.indexOf("}", prefix);
    String replInfo = data.substring(prefix + keyPrefix.length(), suffix).trim();

    if (replInfo.length() >= 2) {
      replInfo = replInfo.substring(1, replInfo.length() - 1).trim();
    }

    List<String> replacementBlocks = new ArrayList<>();
    if (replInfo.equals("")) {
      replInfo = "" + defaultMax;
    }

    SortedSet<Integer> targets = Arrays.stream(replInfo.split(",")).map(this::getReplacementCount)
        .collect(Collectors.toCollection(TreeSet::new));
    targets.forEach(rep -> replacementBlocks.add(buildReplacement(rep, json)));

    return replacementBlocks;
  }

  private String getTemplateString(String url, boolean json) {
    int prefix = json ? url.indexOf("\"" + keyPrefix) : url.indexOf(keyPrefix);
    int suffix = json ? url.indexOf("}\"", prefix) : url.indexOf("}", prefix);
    int suffixLength = json ? 2 : 1;
    return url.substring(0, prefix) + TEMPLATE_KEY + url.substring(suffix + suffixLength);
  }

  @Override
  public List<TestUrl> generateUrls(TestUrl url) {
    List<TestUrl> generated = new ArrayList<>();

    List<TestUrl> generatedUrl = new ArrayList<>();

    if (url.getUrl().contains(keyPrefix)) {
      String targetUrl = url.getUrl();
      String templateUrl = getTemplateString(targetUrl, false);

      for (String block : buildReplacementBlocks(targetUrl, false)) {
        generatedUrl.add(buildUrl(url, templateUrl.replace(TEMPLATE_KEY, block)));
      }
    }

    if (url.getPayload() != null && url.getPayload().contains(keyPrefix)) {
      if (generatedUrl.isEmpty()) {
        generatedUrl.add(url);
      }

      for (TestUrl urlPartial : generatedUrl) {
        String targetPayload = url.getPayload();
        String templatePayload = getTemplateString(targetPayload, true);

        for (String block : buildReplacementBlocks(targetPayload, true)) {
          generated.add(buildUrl(urlPartial, urlPartial.getUrl(), templatePayload.replace(TEMPLATE_KEY, block)));
        }
      }
    }
    else {
      generated.addAll(generatedUrl);
    }

    return generated;
  }
}
