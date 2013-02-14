/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.rest;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyAlert;

public interface RestClient
{

    interface Base
    {

        void validateConfiguration()
            throws IOException;

        App forApplication( String appId );

    }

    interface App
    {

        void validateApplicationId()
            throws IOException;

        String uploadScan( File scanFile )
            throws IOException;

        Scan forScan( String scanId );

    }

    interface Scan
    {

        List<PolicyAlert> evaluatePolicies()
            throws IOException;

    }

}
