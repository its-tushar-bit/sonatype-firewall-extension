/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

/**
 * A DTO to report on the state of an application bundle scan.
 *
 * @since 1.8
 */
public class ScanTicket
{
  public String ticketId;

  public String applicationPublicId;

  public int currentStep;

  public String currentStepName;

  public int totalSteps;

  public String error;

  public String scanId;
}
