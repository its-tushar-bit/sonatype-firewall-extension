package com.sonatype.insight.brain.model;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    private long lastModified;

    private String lastModifiedSimple;

    public long getLastModified()
    {
        return lastModified;
    }

    public String getLastModifiedSimple()
    {
        return lastModifiedSimple;
    }

    public void setLastModified( final long lastModified )
    {
        this.lastModified = lastModified;
        this.lastModifiedSimple = new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date( lastModified ) );
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
