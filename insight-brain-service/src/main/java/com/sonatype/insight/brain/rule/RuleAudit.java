/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.rule;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.sonatype.insight.brain.data.Auditing;
import com.sonatype.insight.brain.model.rule.Rule;

public class RuleAudit
{
    public static String RULE_AUDIT_FILENAME = "rule.json";

    public static void saveChange( final File auditDir, final Rule rule, final String user, final String ip,
                                   final String where )
        throws IOException
    {
        Auditing.saveData( auditDir, RULE_AUDIT_FILENAME, toJson( rule ), user, ip, where );
    }

    private static ContainerNode<?> toJson( final Rule rule )
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure( SerializationFeature.INDENT_OUTPUT, true );
        return mapper.convertValue( rule, ContainerNode.class );
    }
}
