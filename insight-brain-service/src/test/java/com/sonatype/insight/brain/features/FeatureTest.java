/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FeatureTest
{
  @Test
  public void testJsonSerializationUsesToString() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    for (Feature feature : Feature.values()) {
      String json = mapper.writeValueAsString(feature);
      assertThat(json, is('"' + feature.toString() + '"'));
    }
  }
}
