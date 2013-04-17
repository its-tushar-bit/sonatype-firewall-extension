package com.sonatype.insight.brain.releasegraph;

import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;

public class ReportItemKey
{
    private String licenseFingerprint;

    private String applicationPublicId;

    private String scanId;

    private InsightWork work;

    private InsightProxy proxy;

    public ReportItemKey( String licenseFingerprint, String applicationPublicId, String scanId, InsightWork work,
                          InsightProxy proxy )
    {
        this.licenseFingerprint = licenseFingerprint;
        this.applicationPublicId = applicationPublicId;
        this.scanId = scanId;
        this.work = work;
        this.proxy = proxy;
    }

    public InsightProxy getProxy()
    {
        return proxy;
    }

    public InsightWork getWork()
    {
        return work;
    }

    public String getApplicationPublicId()
    {
        return applicationPublicId;
    }

    public String getLicenseFingerprint()
    {
        return licenseFingerprint;
    }

    public String getScanId()
    {
        return scanId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( applicationPublicId == null ) ? 0 : applicationPublicId.hashCode() );
        result = prime * result + ( ( scanId == null ) ? 0 : scanId.hashCode() );
        result = prime * result + ( ( licenseFingerprint == null ) ? 0 : licenseFingerprint.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        ReportItemKey other = (ReportItemKey) obj;
        if ( applicationPublicId == null )
        {
            if ( other.applicationPublicId != null )
                return false;
        }
        else if ( !applicationPublicId.equals( other.applicationPublicId ) )
            return false;
        if ( scanId == null )
        {
            if ( other.scanId != null )
                return false;
        }
        else if ( !scanId.equals( other.scanId ) )
            return false;
        if ( licenseFingerprint == null )
        {
            if ( other.licenseFingerprint != null )
                return false;
        }
        else if ( !licenseFingerprint.equals( other.licenseFingerprint ) )
            return false;
        return true;
    }

}