/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

public enum ScmResultStatus
{
  SUCCESS,
  SCM_AUTHN_FAILURE,
  SCM_AUTHZ_FAILURE,
  SCM_UNKNOWN_HOST_FAILURE,
  SCM_INVALID_REPOSITORY_URL
}
