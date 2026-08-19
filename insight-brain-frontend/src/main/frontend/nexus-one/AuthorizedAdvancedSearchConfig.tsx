/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import AdvancedSearchConfigContainer from 'MainRoot/configuration/advancedSearch/AdvancedSearchConfigContainer';

// AdvancedSearchConfig.jsx declares isAuthorized as a required prop, sourced from Classic's
// ui-router `resolve.isAuthorized` (configuration/route.js). mountClassicComponent
// does not inject ui-router resolve values, so mounting AdvancedSearchConfigContainer directly
// would leave isAuthorized undefined and flip AdvancedSearchConfig into the auth-error branch.
// requireConfigureSystem on the NOUX route already gates entry on CONFIGURE_SYSTEM,
// so hard-coding isAuthorized={true} here is equivalent to the Classic behavior.
export default function AuthorizedAdvancedSearchConfig() {
  return <AdvancedSearchConfigContainer isAuthorized={true} />;
}
