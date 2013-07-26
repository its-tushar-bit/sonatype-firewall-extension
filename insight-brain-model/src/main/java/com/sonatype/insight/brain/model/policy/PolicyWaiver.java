/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.6
 */
@Entity
@Table( name = "policy_waiver" )
public class PolicyWaiver
    implements HasStringId
{
    @Id
    @Column( name = "policy_waiver_id" )
    private String id;

    @Column( name = "hash" )
    private String hash;

    @Column( name = "policy_id" )
    private String policyId;

    @Column( name = "constraint_id" )
    private String constraintId;

    @Column( name = "owner_id" )
    private String ownerId;

    @Column( name = "comment" )
    private String comment;

    @Column( name = "create_time" )
    private Date createTime;

    public PolicyWaiver()
    {
    }

    public PolicyWaiver( String hash, String policyId, String ownerId, String comment )
    {
        setHash( hash );
        this.policyId = policyId;
        this.ownerId = ownerId;
        this.comment = comment;
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

    public String getHash()
    {
        return hash;
    }

    public void setHash( String hash )
    {
        this.hash = HashHelper.truncateHash( hash );
    }

    public String getPolicyId()
    {
        return policyId;
    }

    public void setPolicyId( String policyId )
    {
        this.policyId = policyId;
    }

    public String getConstraintId()
    {
        return constraintId;
    }

    public void setConstraintId( String constraintId )
    {
        this.constraintId = constraintId;
    }

    public String getOwnerId()
    {
        return ownerId;
    }

    public void setOwnerId( String ownerId )
    {
        this.ownerId = ownerId;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime( Date createTime )
    {
        this.createTime = createTime;
    }
}
