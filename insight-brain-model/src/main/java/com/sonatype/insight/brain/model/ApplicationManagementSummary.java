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

    private String publicId;

    public String getPublicId()
    {
        return publicId;
    }

    public void setPublicId( String publicId )
    {
        this.publicId = publicId;
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

    private String reportName;

    public String getReportName()
    {
        return reportName;
    }

    public void setReportName( final String reportName )
    {
        this.reportName = reportName;
    }
}
