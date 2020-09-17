/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.98
 */
public class ApiScmOnboardingService
{
  private static final Logger log = LoggerFactory.getLogger(ApiScmOnboardingService.class);

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public List<SCMRepository> loadRepositories(final String orgId) {
    log.debug("loadRepositories returning stubbed data for org {}", orgId);

    return Arrays.asList(
        new SCMRepository("https://github.com/depshield-ci/ci-project-1.git", true),
        new SCMRepository("https://github.com/depshield-ci/ci-project-16.git", true),
        new SCMRepository("https://github.com/depshield-ci/create-react-app.git", false),
        new SCMRepository("https://github.com/sonatype-nexus-community/nexus-repository-p2.git", false),
        new SCMRepository("https://github.com/sonatype-nexus-community/nexus-repository-puppet.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/nexus-repository-terraform.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/nexus-repository-vgo.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/nexus-scripting-examples.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/nexus-webhook-example-collection.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/nxrm-cli.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/ossindex-gradle-plugin.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/oysteR.git", true),
        new SCMRepository("https://github.com/sonatype-nexus-community/prime-nexus-proxy-repos.git", false)
    );
  }
}
