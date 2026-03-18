/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScmUserMappingsHelper
{
  public static List<Map.Entry<String, String>> getRandomMappings() {
    List<String> fromStrings = Arrays.asList("SCM_USERNAME", "SCM_EMAIL", "SCM_FULLNAME",
        "GITLOG_EMAIL", "GITLOG_FULLNAME");
    List<String> toStrings = Arrays.asList("IQ_USERNAME", "IQ_EMAIL", "IQ_FULLNAME");

    return IntStream.range(0, new Random().nextInt(10))
        .mapToObj(num -> getMappingForScmUserJsonStorage(fromStrings.get(new Random().nextInt(fromStrings.size() - 1)),
            toStrings.get(new Random().nextInt(toStrings.size() - 1))))
        .distinct()
        .collect(Collectors.toList());
  }

  public static Map.Entry<String, String> getMappingForScmUserJsonStorage(
      final String fromMapping,
      final String toMapping)
  {
    return new AbstractMap.SimpleEntry<>(fromMapping, toMapping);
  }
}
