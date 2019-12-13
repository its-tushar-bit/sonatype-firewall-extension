/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global CLM_SERVER_VERSION, CLM_BUILD_TIMESTAMP */
// constants provided by webpack DefinePlugin
window.clmServerVersion = CLM_SERVER_VERSION;
window.clmBuildTimestamp = CLM_BUILD_TIMESTAMP;
window.angularDebug = process.env.NODE_ENV !== 'production';
