/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

public class Stage
{
    private String stageTypeId;

    public Stage()
    {
    }

    public Stage( final String stageTypeId )
    {
        this.stageTypeId = stageTypeId;
    }

    public String getStageTypeId()
    {
        return stageTypeId;
    }

    public void setStageTypeId( final String stageTypeId )
    {
        this.stageTypeId = stageTypeId;
    }
}
