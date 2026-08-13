/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureTest
{
  @Test
  public void testJsonSerializationUsesToString() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Collection<Feature> allFeatures = new HashSet<>();
    Collections.addAll(allFeatures, LicensedFeature.values());
    Collections.addAll(allFeatures, NonLicensedFeature.values());
    for (Feature feature : allFeatures) {
      String json = mapper.writeValueAsString(feature);
      assertThat(json).isEqualTo('"' + feature.toString() + '"');
    }
  }
}
