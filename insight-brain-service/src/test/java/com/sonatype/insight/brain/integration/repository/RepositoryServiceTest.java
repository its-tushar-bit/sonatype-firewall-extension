/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @since 1.17
 */
public class RepositoryServiceTest
    extends AbstractRepositoryServiceTest
{
  @Inject
  private RepositoryService repositoryService;

  @Override
  protected AbstractRepositoryService getRepositoryService() {
    return repositoryService;
  }

  @Test
  public void testGetIgnorePatterns() throws Exception {
    // Prepare request and mock the HDS request
    FirewallIgnorePatterns hdsResult = new FirewallIgnorePatterns();
    hdsResult.regexpsByRepositoryFormat = new HashMap<>();
    hdsResult.regexpsByRepositoryFormat.put("foo", Collections.singletonList("bar"));
    when(hdsClient.get(eq(FirewallIgnorePatterns.class), eq(AbstractRepositoryService.HDS_IGNORE_PATTERNS_PATH)))
        .thenReturn(hdsResult);

    // Call the service
    FirewallIgnorePatterns firewallIgnorePatterns = repositoryService.getIgnorePatterns();

    assertThat(firewallIgnorePatterns).isEqualTo(hdsResult);
  }
}
