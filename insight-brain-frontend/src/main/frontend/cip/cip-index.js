/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import './cip-bootstrap.scss';
import './cip.scss';

import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'bootstrap2-umd/js/bootstrap-tooltip';
import '../utility/directives/utility.directives.module';
import '../utility/services/utility.services.module';
import '../utilAngular/Globals';
import '../utilAngular/AngularCommon';
import '../utilAngular/CommonServices';
import '../utilAngular/CLMContextLocation';
import '../util/CLMLocation';
import '../utilAngular/HttpInterceptors';
import 'angular-sanitize';
import 'angular-vs-repeat';
import 'es6-collections';
import { polyfill } from 'es6-promise';

// cip-version-graph
import './cip.version.graph/cip.version.graph.module';
import '../version-graph/appcheck';

// cip-label-editor
import './cip.label.editor/cip.label.editor.module';

// cip-license-editor
import './cip.license.editor/cip.license.editor.module';

// cip-policy-violations
import './cip.policy.violations/cip.policy.violations.module';

// cip-claim-component
import 'bootstrap-datepicker';
import './cip-claim-component';

polyfill();
