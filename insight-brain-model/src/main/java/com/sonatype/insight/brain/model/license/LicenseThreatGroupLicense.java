/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Association between license threat groups and licenses.
 */
@Entity
@Table( name = "license_threat_group_license" )
public class LicenseThreatGroupLicense
    implements HasStringId
{
    @Id
    @Column( name = "license_threat_group_license_id" )
    private String id;

    @Column( name = "application_id" )
    private String applicationId;

    @Column( name = "license_threat_group_id" )
    private String licenseThreatGroupId;

    @Column( name = "multi_license_id" )
    private String multiLicenseId;

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

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId( String applicationId )
    {
        this.applicationId = applicationId;
    }

    public String getLicenseThreatGroupId()
    {
        return licenseThreatGroupId;
    }

    public void setLicenseThreatGroupId( String licenseThreatGroupId )
    {
        this.licenseThreatGroupId = licenseThreatGroupId;
    }

    public String getMultiLicenseId()
    {
        return multiLicenseId;
    }

    public void setMultiLicenseId( String multiLicenseId )
    {
        this.multiLicenseId = multiLicenseId;
    }
}
