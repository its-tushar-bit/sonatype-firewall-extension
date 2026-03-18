/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Comparator;
import java.util.Objects;

import com.sonatype.insight.brain.model.Owner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.jetbrains.annotations.NotNull;

public class ApiOwnerDTO
    implements Comparable<ApiOwnerDTO>
{
  private static final Comparator<ApiOwnerDTO> COMPARATOR =
      Comparator.comparing((ApiOwnerDTO dto) -> dto.ownerName)
          .thenComparing(dto -> dto.ownerPublicId)
          .thenComparing(dto -> dto.ownerId);

  @JsonInclude(Include.NON_NULL)
  public String ownerPublicId;

  public String ownerId;

  @JsonInclude(Include.NON_NULL)
  public String ownerName;

  public String ownerType;

  public static ApiOwnerDTO fromOwner(Owner ownerModel) {
    ApiOwnerDTO ownerDTO = new ApiOwnerDTO();
    ownerDTO.ownerType = ownerModel.getType().name();
    ownerDTO.ownerId = ownerModel.getId();
    ownerDTO.ownerPublicId = ownerModel.getPublicId();
    ownerDTO.ownerName = ownerModel.getName();
    return ownerDTO;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiOwnerDTO that = (ApiOwnerDTO) o;
    return Objects.equals(ownerId, that.ownerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerId);
  }

  @Override
  public int compareTo(@NotNull ApiOwnerDTO other) {
    return COMPARATOR.compare(this, other);
  }
}
