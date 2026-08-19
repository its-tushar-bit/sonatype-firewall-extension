/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response DTO for cascade re-evaluation status containing progress information.
 *
 * @since 1.196
 */
@JsonInclude(Include.NON_NULL)
public class CascadeStatusResponseDTO
{
  /**
   * Overall status of the cascade operation.
   */
  public ReevaluateCascadeRequestStatus status;

  /**
   * The component hash that was submitted for cascade re-evaluation.
   */
  public String referenceComponentHash;

  /**
   * List of components that have been evaluated successfully.
   */
  public List<CascadeComponentProgressDTO> evaluated = new ArrayList<>();

  /**
   * List of components that have been evaluated but FAILED.
   */
  public List<CascadeComponentProgressDTO> failed = new ArrayList<>();

  /**
   * List of components that are still pending evaluation (status is PENDING).
   */
  public List<CascadeComponentProgressDTO> pending = new ArrayList<>();
}
