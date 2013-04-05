/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Association between application profiles and policies.
 */
@Entity
@Table( name = "application_profile_policy" )
public class ApplicationProfilePolicy
    implements HasStringId
{
    @Id
    @Column( name = "application_profile_policy_id" )
    private String id;

    @Column( name = "application_profile_id" )
    private String applicationProfileId;

    @Column( name = "policy_id" )
    private String policyId;

    public ApplicationProfilePolicy()
    {
    }

    public ApplicationProfilePolicy( String applicationProfileId, String policyId )
    {
        this.applicationProfileId = applicationProfileId;
        this.policyId = policyId;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public void setId( String id )
    {
        this.id = id;
    }

    public String getApplicationProfileId()
    {
        return applicationProfileId;
    }

    public void setApplicationProfileId( String applicationProfileId )
    {
        this.applicationProfileId = applicationProfileId;
    }

    public String getPolicyId()
    {
        return policyId;
    }

    public void setPolicyId( String policyId )
    {
        this.policyId = policyId;
    }
}
