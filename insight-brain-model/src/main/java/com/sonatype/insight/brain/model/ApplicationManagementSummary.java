package com.sonatype.insight.brain.model;

public class ApplicationManagementSummary
{
    private String id;

    public String getId()
    {
        return id;
    }

    public void setId( final String id )
    {
        this.id = id;
    }

    private Long lastModified;

    public Long getLastModified()
    {
        return lastModified;
    }

    public void setLastModified( final Long lastModified )
    {
        this.lastModified = lastModified;
    }

    private String scanId;

    public String getScanId()
    {
        return scanId;
    }

    public void setScanId( final String scanId )
    {
        this.scanId = scanId;
    }
}
