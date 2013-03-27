package com.sonatype.insight.brain.model;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public class ApplicationManagementSummary
{
    private String id;

    private String publicId;

    private String name;

    private PolicyEvaluation policyEvaluation;

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

    public PolicyEvaluation getPolicyEvaluation()
    {
        return policyEvaluation;
    }

    public void setPolicyEvaluation( PolicyEvaluation policyEvaluation )
    {
        this.policyEvaluation = policyEvaluation;
    }

    public static ApplicationManagementSummary fromApplication( Application application )
    {
        ApplicationManagementSummary summary = new ApplicationManagementSummary();
        summary.setId( application.getId() );
        summary.setName( application.getName() );
        summary.setPublicId( application.getPublicId() );
        return summary;
    }
}
