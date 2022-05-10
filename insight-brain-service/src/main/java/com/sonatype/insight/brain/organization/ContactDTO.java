/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

/**
 * The contact DTO
 *
 * @since 1.8
 */
public class ContactDTO
{
  private String internalName;

  private String displayName;

  private String email;

  private String realm;

  private String error;

  public ContactDTO() {

  }

  /**
   * Constructor for contact DTO
   *
   * @param internalName the internal name of the contact user (username)
   * @param displayName the display name for the contact user
   * @param email the email address for the contact user
   * @param realm the realm for the contact user
   */
  public ContactDTO(final String internalName, final String displayName, final String email, final String realm) {

    this.internalName = internalName;
    this.displayName = displayName;
    this.email = email;
    this.realm = realm;
  }

  /**
   * Constructor for contact DTO
   *
   * @param internalName the internal name of the contact user (username)
   */
  public ContactDTO(final String internalName) {
    this(internalName, null, null, null);
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(final String internalName) {
    this.internalName = internalName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getRealm() {
    return realm;
  }

  public void setRealm(final String realm) {
    this.realm = realm;
  }

  public String getError() {
    return error;
  }

  public void setError(final String error) {
    this.error = error;
  }

  @Override
  public String toString() {
    return "ContactDTO{" + "internalName='" + internalName + '\'' + ", displayName='" + displayName + '\''
        + ", email='" + email + '\'' + ", realm='" + realm + '\'' + ", error='" + error + '\'' + '}';
  }
}
