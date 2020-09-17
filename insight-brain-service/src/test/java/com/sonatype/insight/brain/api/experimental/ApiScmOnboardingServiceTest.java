/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiScmOnboardingServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiScmOnboardingService apiScmOnboardingService;

  @Test
  public void testStubMethod() {
    List<SCMRepository> repositories = apiScmOnboardingService.loadRepositories(null);

    assertThat(repositories.size()).isEqualTo(13);
  }
}
