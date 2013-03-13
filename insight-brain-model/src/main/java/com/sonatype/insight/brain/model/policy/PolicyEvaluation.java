/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.clm.dto.model.policy.Stage;

public class PolicyEvaluation
{
    private Stage stage;

    private String scanId;

    private long time;

    private String user;

    public long getTime()
    {
        return time;
    }

    public void setTime( long time )
    {
        this.time = time;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser( String user )
    {
        this.user = user;
    }

    public Stage getStage()
    {
        return stage;
    }

    public void setStage( Stage stage )
    {
        this.stage = stage;
    }

    public String getScanId()
    {
        return scanId;
    }

    public void setScanId( String scanId )
    {
        this.scanId = scanId;
    }
}
