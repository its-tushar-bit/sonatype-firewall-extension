/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;

public interface RestClient
{

  interface Base
  {

    void validateConfiguration() throws IOException;

    Map<String, String> getApplications() throws IOException;

    ProprietaryConfig getProprietaryConfiguration() throws IOException;

    App forApplication(String appId);

  }

  interface App
  {

    void validateApplicationId() throws IOException;

    ScanReceipt uploadScan(File scanFile) throws IOException;

    Scan forScan(String scanId);

  }

  interface Scan
  {

    PolicyEvaluationResult evaluatePolicies(Stage stage) throws IOException;

  }

}
