/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.tenant.auth.provisionig;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class TenantCLI
{
  private static final ActionResolver actionResolver = new ActionResolver();

  public static void main(String[] args) {
    TenantParameters tenantParameters = new TenantParameters();
    JCommander tenantCmd = JCommander.newBuilder().addObject(tenantParameters).build();
    try {
      tenantCmd.parse(args);
    }
    catch (ParameterException e) {
      tenantCmd.usage();
      System.exit(0);
    }
    actionResolver.perform(tenantParameters);
  }
}
