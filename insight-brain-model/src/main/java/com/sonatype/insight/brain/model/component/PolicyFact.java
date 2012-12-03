/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

public class PolicyFact
{
    private Component component;

    private String ruleId;

    private String ruleName;

    private String kind;

    // Required for json deserialization
    public PolicyFact()
    {
    }

    public PolicyFact( final Component component, final String ruleId, final String kind )
    {
        this.component = component;
        this.ruleId = ruleId;
        this.kind = kind;
    }

    public Component getComponent()
    {
        return component;
    }

    public String getRuleId()
    {
        return ruleId;
    }

    public String getKind()
    {
        return kind;
    }

    public String getRuleName()
    {
        return ruleName;
    }

    public void setRuleName( final String ruleName )
    {
        this.ruleName = ruleName;
    }
}
