package com.sonatype.insight.brain.model.policy;

import java.io.Serializable;
import java.util.List;

public class IdeMatchedComponent
    implements Serializable
{
    private static final long serialVersionUID = 5461164085057984895L;

    private List<PolicyAlert> alerts;

    private String artifactId;

    private String classifier;

    private String groupId;

    private String hash;

    private String matchState;

    private boolean simpleMatch;

    private String version;

    private Integer waitDelta;

    public List<PolicyAlert> getAlerts()
    {
        return alerts;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getHash()
    {
        return hash;
    }

    public String getMatchState()
    {
        return matchState;
    }

    public String getVersion()
    {
        return version;
    }

    public Integer getWaitDelta()
    {
        return waitDelta;
    }

    public boolean isSimpleMatch()
    {
        return simpleMatch;
    }

    public void setAlerts( List<PolicyAlert> alerts )
    {
        this.alerts = alerts;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setHash( String hash )
    {
        this.hash = hash;
    }

    public void setMatchState( String matchState )
    {
        this.matchState = matchState;
    }

    public void setSimpleMatch( boolean simpleMatch )
    {
        this.simpleMatch = simpleMatch;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public void setWaitDelta( Integer waitDelta )
    {
        this.waitDelta = waitDelta;
    }
}
