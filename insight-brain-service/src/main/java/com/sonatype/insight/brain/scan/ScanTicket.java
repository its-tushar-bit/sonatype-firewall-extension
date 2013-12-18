/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

/**
 * A DTO to report on the state of an application bundle scan.
 * 
 * @since 1.7.1
 */
public class ScanTicket
{
  public String ticketId;

  public String state;

  public String error;
}
