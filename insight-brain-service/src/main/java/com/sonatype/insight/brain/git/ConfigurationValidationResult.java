/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.nexus.scm.api.model.ValidationResult;

/**
 * The object containing the results of testing all of the configuration options for an SCM
 */
public class ConfigurationValidationResult
{
  private ValidationResult configurationComplete;

  private ValidationResult repoPrivate;

  private ValidationResult repoPublic;

  private ValidationResult tokenPermissions;

  private ValidationResult sshConfiguration;

  public ConfigurationValidationResult() {
  }

  public ValidationResult getRepoPrivate() {
    return repoPrivate;
  }

  public void setRepoPrivate(final ValidationResult repoPrivate) {
    this.repoPrivate = repoPrivate;
  }

  public ValidationResult getRepoPublic() {
    return repoPublic;
  }

  public void setRepoPublic(final ValidationResult repoPublic) {
    this.repoPublic = repoPublic;
  }

  public ValidationResult getConfigurationComplete() {
    return configurationComplete;
  }

  public void setConfigurationComplete(final ValidationResult configurationComplete) {
    this.configurationComplete = configurationComplete;
  }

  public ValidationResult getTokenPermissions() {
    return tokenPermissions;
  }

  public void setTokenPermissions(final ValidationResult tokenPermissions) {
    this.tokenPermissions = tokenPermissions;
  }

  public ValidationResult getSshConfiguration() {
    return sshConfiguration;
  }

  public void setSshConfiguration(ValidationResult sshConfiguration) {
    this.sshConfiguration = sshConfiguration;
  }
}
