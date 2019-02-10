/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbutil;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.tools.common.PerfTestConfig.TestUrl;

import static java.util.stream.Collectors.toList;

public class CompoundReplacer
    extends Replacer
{
  private final List<Replacer> replacers;

  public CompoundReplacer(List<Replacer> replacers) {
    this.replacers = replacers;
  }

  @Override
  public List<TestUrl> generateUrls(TestUrl url) {
    List<TestUrl> source = new ArrayList<>();
    source.add(url);
    boolean replacementsMade = false;

    for (Replacer r : replacers) {
      List<TestUrl> intermediary = source.stream().map(r::generateUrls).flatMap(List::stream).collect(toList());
      if (!intermediary.isEmpty()) {
        source = intermediary;
        replacementsMade = true;
      }
    }

    return replacementsMade ? source : new ArrayList<>();
  }
}
