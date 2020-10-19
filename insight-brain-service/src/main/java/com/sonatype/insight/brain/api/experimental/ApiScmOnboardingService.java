/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.counting;

/**
 * This service supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.98
 */
public class ApiScmOnboardingService
{
  private static final Logger log = LoggerFactory.getLogger(ApiScmOnboardingService.class);

  private final SourceControlDAO sourceControlDAO;

  @Inject
  public ApiScmOnboardingService(final SourceControlDAO sourceControlDAO) {
    this.sourceControlDAO = sourceControlDAO;
  }

  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public List<SCMRepository> loadRepositories(final String orgId) {
    log.debug("loadRepositories returning stubbed data for org {}", orgId);

    return Arrays.asList(
        new SCMRepository(SourceControlProvider.GITHUB, "https://github.com/depshield-ci/ci-project-1.git", true,
            "depshield-ci", "ci-project-1", "long description: Nexus IQ Server is the on-premises server that " +
            "customers run to evaluate their applications against a set of policies and review the results. " +
            "It is part of the Nexus Lifecycle product umbrella (historically, this product umbrella was previously " +
            "known as Component Lifecycle Management (CLM)).\n" +
            "\n" +
            "insight-brain contains the server, front-end, and component scanner for Nexus IQ Server. It scans " +
            "projects (i.e. it generates hashes that represent components in an application - see also: " +
            "insight-scanner), and evaluates known component vulnerabilities and component license information " +
            "against user-configured policies. It then uses these data to generate an application scan report."),
        new SCMRepository(SourceControlProvider.GITHUB, "https://github.com/depshield-ci/ci-project-16.git", true,
            "depshield-ci", "ci-project-16", "were"),
        new SCMRepository(SourceControlProvider.GITHUB, "https://github.com/depshield-ci/create-react-app.git", false,
            "depshield-ci", "create-react-app", "the"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/nexus-repository-p2.git", false, "sonatype-nexus-community",
            "nexus-repository-pw", "days"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/nexus-repository-puppet.git", true, "sonatype-nexus-community",
            "nexus-repository-puppet", "my"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/nexus-repository-terraform.git", true,
            "sonatype-nexus-community", "nexus-repository-terraform", "friend"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/nexus-repository-vgo.git", true, "sonatype-nexus-community",
            "nexus-repository-vgo", "we"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/nexus-scripting-examples.git", true,
            "sonatype-nexus-community", "nexus-scripting-examples", "thought"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/nexus-webhook-example-collection.git", true,
            "sonatype-nexus-community", "nexus-webhook-example-collection", "they'd"),
        new SCMRepository(SourceControlProvider.GITHUB, "https://github.com/sonatype-nexus-community/nxrm-cli.git",
            true, "sonatype-nexus-community", "nxrm-cli", "never"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/ossindex-gradle-plugin.git", true, "sonatype-nexus-community",
            "ossindex-gradle-plugin", "end"),
        new SCMRepository(SourceControlProvider.GITHUB, "https://github.com/sonatype-nexus-community/oysteR.git", true,
            "sonatype-nexus-community", "oysteR", "we'd"),
        new SCMRepository(SourceControlProvider.GITHUB,
            "https://github.com/sonatype-nexus-community/prime-nexus-proxy-repos.git", false,
            "sonatype-nexus-community", "prime-nexus-proxy-repos", "sing")
    );
  }

  /**
   * calculates the default host URL for use in onboarding
   * @param orgId optional, if provided will attempt to use existing SCM repos in this org
   */
  @Authorize(permission = Permission.MANAGE_AUTOMATIC_SCM_CONFIGURATION)
  public String getDefaultHostUrl(final String providerString, final String orgId) {
    if (StringUtils.isBlank(providerString)) {
      throw new BadRequestException("Provider has not been specified");
    }
    SourceControlProvider provider;
    try {
      provider = SourceControlProvider.fromString(providerString);
    }
    catch (IllegalArgumentException e ) {
      throw new BadRequestException("Invalid provider: " + providerString, e);
    }

    // if org is provided, try to gather URL from an app within the org
    String repoUrl = getMostCommonRepoBaseUrlForOrg(orgId);
    if (!StringUtils.isEmpty(repoUrl)) {
      return repoUrl;
    }

    switch (provider) {
      case GITHUB:
        return "https://github.com/";
      case GITLAB:
        return "https://gitlab.com/";
      case BITBUCKET:
        return "https://bitbucket.org/";
      default:
        return null;
    }
  }

  private String getMostCommonRepoBaseUrlForOrg(final String orgId) {
    List<SourceControl> sourceControls = Collections.emptyList();
    if (StringUtils.isNotEmpty(orgId)) {
      sourceControls =
          sourceControlDAO.getApplicationSourceControlsByOrganizationWithRepositories(orgId);
    }
    if (sourceControls.isEmpty()) {
      sourceControls = sourceControlDAO.getApplicationSourceControlsWithRepositories();
    }
    if (!sourceControls.isEmpty()) {
      Optional<Entry<String, Long>> maxEntry = sourceControls.stream()
          .map(SourceControl::getRepositoryUrl)
          .collect(Collectors.groupingBy(this::getBaseUrl, counting()))
          .entrySet().stream()
          .max(Entry.comparingByValue());
      return maxEntry.get().getKey();
    }
    return null;
  }

  private String getBaseUrl(String repoUrl) {
    try {
      URI url = new URI(repoUrl);
      return new URI(url.getScheme(), url.getUserInfo(), url.getHost(), url.getPort(), null, null, null).toString();
    }
    catch (URISyntaxException e) {
      log.info("Was not able to parse repo url {}, falling back to default for the provider", repoUrl, e);
      return "";
    }
  }
}
