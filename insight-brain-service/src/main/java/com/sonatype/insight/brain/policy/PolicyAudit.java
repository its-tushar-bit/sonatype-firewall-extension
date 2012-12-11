/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

public class PolicyAudit
{
    public static String POLICY_AUDIT_FILENAME = "policy.json";

    public static void saveChange( final File auditDir, final Policy policy, final String user, final String ip,
                                   final String where )
        throws IOException
    {
        final JsonStore store = JsonUtils.fileStore( auditDir );
        store.commit( POLICY_AUDIT_FILENAME, JsonUtils.stamp( user, ip, where, toJson( policy ) ) );
    }

    private static ContainerNode<?> toJson( final Policy policy )
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure( SerializationFeature.INDENT_OUTPUT, true );
        return mapper.convertValue( policy, ContainerNode.class );
    }
}
