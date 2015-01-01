/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import static com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil.GAV_SEPARATOR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Shared assertions for structure of DisplayFieldValues
 *
 * @since 1.13.0
 */
public class DisplayFieldValueAssertionUtil
{
  public static void assertDisplayFieldValue(final ComponentDisplayNamePart displayFieldValue, final String field,
                                             final String value)
  {
    assertThat(displayFieldValue.field, is(field));
    assertThat(displayFieldValue.value, is(value));
  }

  public static void assertDisplayFieldValuesForGAV(List<ComponentDisplayNamePart> displayName, String groupId,
                                                    String artifactId, String version)
  {
    assertThat(displayName, hasSize(5));
    assertDisplayFieldValue(displayName.get(0), "Group", groupId);
    assertDisplayFieldValue(displayName.get(1), null, GAV_SEPARATOR);
    assertDisplayFieldValue(displayName.get(2), "Artifact", artifactId);
    assertDisplayFieldValue(displayName.get(3), null, GAV_SEPARATOR);
    assertDisplayFieldValue(displayName.get(4), "Version", version);
  }

  public static void assertDisplayFieldValues(final List<ComponentDisplayNamePart> displayName,
                                              final PolicyViolation policyViolation)
  {
    if (policyViolation.getComponentIdentifier() == null) {
      fail();
    }
    ComponentIdentifier componentIdentifier = policyViolation.getComponentIdentifier();
    assertDisplayFieldValuesForGAV(displayName, componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
        componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID),
        componentIdentifier.get(ComponentIdentifier.VERSION));
  }
}
