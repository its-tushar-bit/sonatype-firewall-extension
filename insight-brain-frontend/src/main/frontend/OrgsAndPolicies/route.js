/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import { ORGS_AND_POLICIES_STATES, toManagementStateRegistration } from './orgsAndPoliciesStates';

// Every `management.*` state is defined once in orgsAndPoliciesStates.ts and shared with the Nexus
// One embedded mount (nexus-one/routes.tsx) so the two bundles' router instances can't drift apart.
ORGS_AND_POLICIES_STATES.forEach((stateDef) => {
  router.stateRegistry.register(toManagementStateRegistration(stateDef));
});
