/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

public class PolicyEvaluation
{
    public class PolicyEvaluationData
    {
        public class PolicyEvaluationStage
        {
            private String stageTypeId;

            public String getStageTypeId()
            {
                return stageTypeId;
            }

            public void setStageTypeId( String stageTypeId )
            {
                this.stageTypeId = stageTypeId;
            }
        }

        private PolicyEvaluationStage stage;

        private String scanId;

        public PolicyEvaluationData.PolicyEvaluationStage getStage()
        {
            return stage;
        }

        public void setStage( PolicyEvaluationData.PolicyEvaluationStage stage )
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

    private long time;

    private String user;

    private PolicyEvaluationData data;

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

    public PolicyEvaluation.PolicyEvaluationData getData()
    {
        return data;
    }

    public void setData( PolicyEvaluation.PolicyEvaluationData data )
    {
        this.data = data;
    }
}
