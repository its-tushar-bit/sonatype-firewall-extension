/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2.dto.scmusermatching;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public record SCMUserMappingsDTO(String role, List<UserMapping> mappings)
{
  public static List<Entry<String, String>> userMappingsAsEntries(List<UserMapping> mappings) {
    return mappings.stream()
        .map(userMapping -> userMapping.toSimpleEntry())
        .collect(
            Collectors.toList());
  }
}
