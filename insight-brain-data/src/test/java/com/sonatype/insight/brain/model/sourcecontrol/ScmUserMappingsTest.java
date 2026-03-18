/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.brain.utils.ScmUserMappingsBuilder;
import org.junit.Test;

import java.util.List;
import java.util.Map.Entry;

import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getRandomMappings;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmUserMappingsTest
{
  @Test
  public void testGetMappings() {
    List<Entry<String, String>> mappings = getRandomMappings();
    ScmUserMappings scmUserMappings = new ScmUserMappingsBuilder()
        .withMappings(mappings)
        .build();

    assertThat(scmUserMappings.getMappings()).containsExactlyInAnyOrderElementsOf(mappings);
  }

  @Test
  public void testGetMappingsAsJson() {
    List<Entry<String, String>> mappings = getRandomMappings();
    ScmUserMappings scmUserMappings = new ScmUserMappingsBuilder()
        .withMappings(mappings)
        .build();

    assertThat(scmUserMappings.getMappingsJson()).isEqualTo(JsonUtils.format(mappings));
  }
}
