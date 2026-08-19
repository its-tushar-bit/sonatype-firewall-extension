/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.organization.ApplicationNameConverter;
import com.sonatype.nexus.scm.api.model.SCMRepository;

/**
 * Creates valid public IDs and names for IQ from a given <i>SCMRepository</i> object.
 * Special characters are removed using <i>ApplicationNameConverter</i>
 *
 * @see SCMRepository
 * @see ApplicationNameConverter
 */
@Named
public class ScmApplicationNameConverter
{
  public static final String PUBLICID_NAME_SEPARATOR = "__";

  public static final String PUBLICID_POSTFIX_SEPARATOR = "_";

  public static final String NAME_POSTFIX_SEPARATOR = " - ";

  private final ApplicationNameConverter applicationNameConverter;

  @Inject
  public ScmApplicationNameConverter(final ApplicationNameConverter applicationNameConverter) {
    this.applicationNameConverter = applicationNameConverter;
  }

  public String buildPublicIdWithPostfix(final SCMRepository scmRepository, final int postfix) {
    return buildPublicId(scmRepository) + PUBLICID_POSTFIX_SEPARATOR + postfix;
  }

  public String buildPublicId(SCMRepository scmRepository) {
    return applicationNameConverter.toPublicId(buildRawPublicId(scmRepository));
  }

  /**
   * Returns true if the public ID generated from the given scmRepository object needs to be modified before it can
   * be imported into IQ because of invalid characters.
   */
  public boolean doesPublicIdRequireModification(SCMRepository scmRepository) {
    return !buildPublicId(scmRepository).equals(buildRawPublicId(scmRepository));
  }

  public String buildNameWithPostfix(final SCMRepository scmRepository, final int postfix) {
    return buildName(scmRepository) + NAME_POSTFIX_SEPARATOR + postfix;
  }

  public String buildName(SCMRepository scmRepository) {
    return applicationNameConverter.toReadableName(applicationNameConverter.toName(scmRepository.getProject())) +
        " - " + applicationNameConverter.toReadableName(applicationNameConverter.toName(scmRepository.getNamespace()));
  }

  private String buildRawPublicId(SCMRepository scmRepository) {
    return scmRepository.getProject() + PUBLICID_NAME_SEPARATOR + scmRepository.getNamespace();
  }
}
