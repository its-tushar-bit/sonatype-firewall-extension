/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.utils.ComponentFactUtil.injectDisplayName;
import static com.sonatype.insight.brain.utils.DisplayFieldValueAssertionUtil.assertDisplayFieldValuesForGAV;

/**
 * @since 1.48
 */
public class ComponentFactUtilTest
{
  @Test
  public void testInjectDisplayName() {
    ComponentFact componentFact = new ComponentFact(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), "h");
    injectDisplayName(componentFact);
    ComponentDisplayName componentDisplayName = componentFact.getDisplayName();

    assertDisplayFieldValuesForGAV(componentDisplayName.parts, "g", "a", "v");
  }
}
