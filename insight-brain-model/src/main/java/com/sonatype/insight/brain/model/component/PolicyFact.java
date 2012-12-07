/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

public class PolicyFact
{
    private Component component;

    private String policyId;

    private String policyName;

    private String kind;

    // Required for json deserialization
    public PolicyFact()
    {
    }

    public PolicyFact( final Component component, final String policyId, final String kind )
    {
        this.component = component;
        this.policyId = policyId;
        this.kind = kind;
    }

    public Component getComponent()
    {
        return component;
    }

    public String getPolicyId()
    {
        return policyId;
    }

    public String getKind()
    {
        return kind;
    }

    public String getPolicyName()
    {
        return policyName;
    }

    public void setPolicyName( final String policyName )
    {
        this.policyName = policyName;
    }
}
