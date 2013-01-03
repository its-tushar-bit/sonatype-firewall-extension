/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.error.HttpStatusCode;

@HttpStatusCode( 400 /* HttpServletResponse.SC_BAD_REQUEST */)
@SuppressWarnings( "serial" )
public class InvalidPolicyException
    extends RuntimeException
{
    public InvalidPolicyException( ValidationResult validationResult )
    {
        super( ValidationResult.toMessageString( validationResult.getErrors() ) );
    }
}
