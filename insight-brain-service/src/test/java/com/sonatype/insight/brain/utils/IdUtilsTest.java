/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.security.MembershipMapping;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IdUtilsTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testGetInternalOwnerId_Global() {
    String id = IdUtils.getInternalOwnerId(IdUtils.TYPE_GLOBAL, null /* ownerId */);
    assertThat(id, is(MembershipMapping.GLOBAL_CONTEXT_ID));
  }
}
