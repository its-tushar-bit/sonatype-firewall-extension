/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Locale;

import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Cacheable
@Entity
@Table(name = "application")
public class Application
    extends Nameable
    implements HasStringId, Owner
{
  @Id
  @Column(name = "application_id")
  private String id;

  @Column(name = "public_id")
  private String publicId;

  @Column(name = "public_id_lowercase")
  private String publicIdLowercase;

  @Column(name = "organization_id")
  private String organizationId;

  @Column(name = "contact_internal_name")
  private String contactInternalName;

  @Transient
  @JsonInclude(Include.NON_EMPTY)
  private Organization organization = null;

  /**
   * @since 1.168
   */
  @Column(name = "legacy_violation_enabled")
  private Boolean legacyViolationEnabled;

  /**
   * @since 1.132
   */
  @Column(name = "repository_connection_enabled")
  private Boolean repositoryConnectionEnabled;

  /**
   * @since 1.137
   */
  @Column(name = "artifactory_connection_enabled")
  private Boolean artifactoryConnectionEnabled;

  public Application() {
  }

  public Application(String publicId, String name, String organizationId) {
    setPublicId(publicId);
    setName(name);
    this.organizationId = organizationId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    if (publicId != null) {
      publicId = publicId.trim();
      publicIdLowercase = publicId.toLowerCase(Locale.ENGLISH);
    }
    else {
      publicIdLowercase = null;
    }
    this.publicId = publicId;
  }

  public String getPublicIdLowercase() {
    return publicIdLowercase;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * publicIdLowercase field. If this method is not defined, jackson will set/access the publicIdLowercase field
   * directly via reflection, possibly setting it to an incorrect value.
   *
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setPublicIdLowercase(String publicIdLowercase) {
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  /**
   * Set the internal name of the contact User (CLM User or LDAP User)
   *
   * @param contactInternalName the contact user's internal name (username)
   * @since 1.8
   */
  public void setContactInternalName(String contactInternalName) {
    this.contactInternalName = contactInternalName;
  }

  /**
   * Get the internal name of the contact User (CLM User or LDAP User)
   *
   * @return the internal name of the contact user
   * @since 1.8
   */
  public String getContactInternalName() {
    return contactInternalName;
  }

  @Override
  @JsonIgnore
  public String getParentOwnerId() {
    return getOrganizationId();
  }

  @Override
  @JsonIgnore
  public boolean canHaveChildren() {
    return false;
  }

  @Override
  @JsonIgnore
  public OwnerType getType() {
    return OwnerType.APPLICATION;
  }

  public Boolean isLegacyViolationEnabled() {
    return legacyViolationEnabled;
  }

  public void setLegacyViolationEnabled(Boolean legacyViolationEnabled) {
    this.legacyViolationEnabled = legacyViolationEnabled;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(final Organization organization) {
    this.organization = organization;
  }

  public Boolean isRepositoryConnectionEnabled() {
    return repositoryConnectionEnabled;
  }

  public void setRepositoryConnectionEnabled(Boolean repositoryConnectionEnabled) {
    this.repositoryConnectionEnabled = repositoryConnectionEnabled;
  }

  public Boolean isArtifactoryConnectionEnabled() {
    return artifactoryConnectionEnabled;
  }

  public void setArtifactoryConnectionEnabled(Boolean artifactoryConnectionEnabled) {
    this.artifactoryConnectionEnabled = artifactoryConnectionEnabled;
  }
}
