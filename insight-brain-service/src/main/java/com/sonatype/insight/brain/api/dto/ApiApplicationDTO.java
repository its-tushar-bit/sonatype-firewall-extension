/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.dto;

/**
 * @since 1.11.0
 */
public class ApiApplicationDTO
{
  private String id;

  private String publicId;

  private String name;

  private String organizationId;

  private String contactUserName;

  public ApiApplicationDTO() {
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

  public String getContactUserName() {
    return contactUserName;
  }

  public void setContactUserName(final String contactUserName) {
    this.contactUserName = contactUserName;
  }
}
