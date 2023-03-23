/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;

import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepository;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepositoryManager;

class FirewallOnboardingDTO
{
  public FirewallOnboardingRepositoryManager repositoryManager;

  public List<FirewallOnboardingRepository> repositories;
}
