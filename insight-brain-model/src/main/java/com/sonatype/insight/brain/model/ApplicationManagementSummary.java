package com.sonatype.insight.brain.model;

import java.util.Collections;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public class ApplicationManagementSummary
{
    private String id;

    private String publicId;

    private String name;

    private Map<String, PolicyEvaluation> policyEvaluations;

    private Map<String, PolicyEvaluationResult> policyEvaluationsResults;

    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }

    public String getPublicId()
    {
        return publicId;
    }

    public void setPublicId( String publicId )
    {
        this.publicId = publicId;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public Map<String, PolicyEvaluation> getPolicyEvaluations()
    {
        return ( policyEvaluations != null ) ? policyEvaluations : Collections.<String, PolicyEvaluation> emptyMap();
    }

    public void setPolicyEvaluations( Map<String, PolicyEvaluation> policyEvaluations )
    {
        this.policyEvaluations = policyEvaluations;
    }

    public Map<String, PolicyEvaluationResult> getPolicyEvaluationsResults()
    {
        return ( policyEvaluationsResults != null ) ? policyEvaluationsResults
                        : Collections.<String, PolicyEvaluationResult> emptyMap();
    }

    public void setPolicyEvaluationsResults( Map<String, PolicyEvaluationResult> policyEvaluationsResults )
    {
        this.policyEvaluationsResults = policyEvaluationsResults;
    }

    public static ApplicationManagementSummary fromApplication( Application application )
    {
        ApplicationManagementSummary summary = new ApplicationManagementSummary();
        summary.setId( application.getId() );
        summary.setName( application.getName() );
        summary.setPublicId( application.getPublicId() );
        return summary;
    }

    @Override
    public String toString()
    {
        return "ApplicationManagementSummary [publicId=" + publicId + ", name=" + name + "]";
    }
}
