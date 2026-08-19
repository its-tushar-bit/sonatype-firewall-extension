/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.clm.dto.model.policy.ComponentFact;

import static com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil.fromIdentifier;

/**
 * Utility to build Brain Policy specific component display names from coordinates.
 *
 * @since 1.48
 */
public class ComponentFactUtil
{
  public static void injectDisplayName(ComponentFact componentFact) {
    componentFact.setDisplayName(fromIdentifier(componentFact.getComponentIdentifier()));
  }
}
