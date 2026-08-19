/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Central file for importing all route definitions.
 * Each route file self-registers on import.
 */

// Root route (special case, doesn't match route.js pattern)
import './RootRoute';

// Organization and policy routes
import './OrgsAndPolicies/route';
import './OrgsAndPolicies/sourceControlRateLimits/route';

// Search and API routes
import './advancedSearch/route';
import './api/route';

// Application and report routes
import './applicationLatestEvaluations/route';
import './applicationReport/route';
import './hostedRepositoryComponentReport/route';
import './report/route';
import './report/react2shell/route';

// Repository configuration routes
import './artifactoryRepositoryConfiguration/route';
import './innerSourceRepositoryConfiguration/route';

// Configuration routes
import './configuration/route';
import './configuration/baseUrl/route';
import './configuration/baseUrl/baseUrlNotSetNotice/route';
import './configuration/crowd/route';
import './configuration/ldap/route';
import './configuration/license/route';
import './configuration/oidc/route';
import './configuration/saml/route';
import './configuration/webhook/route';

// Development routes
import './development/route';
import './development/prioritiesPage/route';

// Dashboard and reporting routes
import './dashboard/route';
import './enterpriseReporting/route';
import './operationalReporting/route';

// Firewall routes
import './firewall/route';
import './firewallOnboarding/route';
import './hostedRepos/route';

// Labs route
import './labs/route';

// Legal and SBOM routes
import './legal/route';
import './legal/sbomManager/route';
import './sbomManager/route';

// Quarantine and security routes
import './quarantinedComponentReport/route';
import './security/route';

// SAST and vulnerability routes
import './sastScan/route';
import './violation/route';
import './vulnerabilityCustomize/route';
import './vulnerabilitySearch/route';

// Waiver routes
import './waivers/route';

// Router configuration - must come AFTER all routes are imported
import router from './router/routerInstance';
import store from './reduxConfig/store';
import { setError } from './session/appErrorSlice';

// Configure error handler for unknown routes
// IMPORTANT: This must be configured after all routes are loaded, so it stays at the end of this file
router.urlService.rules.otherwise(() => {
  store.dispatch(setError('Unknown Address'));
});
