package com.sonatype.insight.brain.model;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public class ApplicationManagementSummary
{
    private String id;

    private String publicId;

    private String name;

    private String applicationProfileId;

    private List<PolicyEvaluation> policyEvaluations;

    private int scansCount;

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

    public List<PolicyEvaluation> getPolicyEvaluations()
    {
        return ( policyEvaluations != null ) ? policyEvaluations : Collections.<PolicyEvaluation>emptyList();
    }

    public void setPolicyEvaluations( List<PolicyEvaluation> policyEvaluations )
    {
        this.policyEvaluations = policyEvaluations;
    }

    public int getScansCount()
    {
        return scansCount;
    }

    public void setScansCount( int scansCount )
    {
        this.scansCount = scansCount;
    }

    public static ApplicationManagementSummary fromApplication( Application application )
    {
        ApplicationManagementSummary summary = new ApplicationManagementSummary();
        summary.setId( application.getId() );
        summary.setName( application.getName() );
        summary.setPublicId( application.getPublicId() );
        summary.setApplicationProfileId( application.getApplicationProfileId() );
        return summary;
    }

    @Override
    public String toString()
    {
        return "ApplicationManagementSummary [publicId=" + publicId + ", name=" + name + "]";
    }

    public String getApplicationProfileId()
    {
        return applicationProfileId;
    }

    public void setApplicationProfileId( String applicationProfileId )
    {
        this.applicationProfileId = applicationProfileId;
    }
}
