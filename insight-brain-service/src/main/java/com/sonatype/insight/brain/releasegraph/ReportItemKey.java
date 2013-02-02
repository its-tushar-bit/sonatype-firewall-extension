package com.sonatype.insight.brain.releasegraph;

import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.InsightWork;

public class ReportItemKey
{
    private String applicationPublicId;

    private String scanId;

    private InsightWork work;

    private InsightProxy proxy;

    public ReportItemKey( String applicationPublicId, String scanId, InsightWork work, InsightProxy proxy )
    {
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
        return true;
    }

}