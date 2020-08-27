/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;

import javax.inject.Inject;

import com.sonatype.insight.test.InjectedTest;

import org.apache.shiro.realm.CachingRealm;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RealmCachingDisabledTest
    extends InjectedTest
{
  @Inject
  private Collection<CachingRealm> cachingRealms;

  @Test
  public void testRealmCachingDisabled() {
    // Authentication and/or authorization caching may not be cluster-friendly and should be disabled
    // see https://github.com/sonatype/insight-brain/pull/5475 for more information
    assertThat(cachingRealms).extracting(CachingRealm::getCacheManager).containsOnlyNulls();
  }
}
