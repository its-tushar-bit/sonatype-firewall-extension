/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

public class ApplicationDTO
{
  private String id;

  private String publicId;

  private String name;

  private String organizationId;

  private String organizationName;

  private ContactDTO contact;

  public ApplicationDTO() {
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(final String publicId) {
    this.publicId = publicId;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(final String organizationId) {
    this.organizationId = organizationId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(final String organizationName) {
    this.organizationName = organizationName;
  }

  /**
   * Get the contact DTO for the application
   *
   * @return the contact DTO
   * @since 1.8
   */
  public ContactDTO getContact() {
    return contact;
  }

  /**
   * Set the contact DTO for the application
   *
   * @param contact the contact DTO
   * @since 1.8
   */
  public void setContact(final ContactDTO contact) {
    this.contact = contact;
  }

  @Override
  public String toString() {
    return "ApplicationDTO{" + "id='" + id + '\'' + ", publicId='" + publicId + '\'' + ", name='" + name + '\''
        + ", organizationId='" + organizationId + '\'' + ", organizationName='" + organizationName + '\''
        + ", contact=" + contact + '}';
  }
}
