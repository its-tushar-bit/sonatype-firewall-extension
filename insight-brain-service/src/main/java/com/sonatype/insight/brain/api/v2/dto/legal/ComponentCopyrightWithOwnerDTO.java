/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Objects;

/**
 * @since 1.107
 */
public class ComponentCopyrightWithOwnerDTO
{
  private ComponentCopyrightDTO componentCopyrightDTO;

  private String ownerId;

  public ComponentCopyrightWithOwnerDTO() {
    // for jackson
  }

  public ComponentCopyrightWithOwnerDTO(
      final ComponentCopyrightDTO componentCopyrightDTO,
      final String ownerId)
  {
    this.componentCopyrightDTO = componentCopyrightDTO;
    this.ownerId = ownerId;
  }

  public ComponentCopyrightDTO getComponentCopyrightDTO() {
    return componentCopyrightDTO;
  }

  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentCopyrightWithOwnerDTO that = (ComponentCopyrightWithOwnerDTO) o;
    return Objects.equals(getComponentCopyrightDTO(), that.getComponentCopyrightDTO()) &&
        Objects.equals(getOwnerId(), that.getOwnerId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getComponentCopyrightDTO(), getOwnerId());
  }
}
