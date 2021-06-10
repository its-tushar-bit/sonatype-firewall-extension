/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'angular';
import 'angular-route';
import 'angular-ui-bootstrap/ui-bootstrap-tpls';

// only necessary for phantomjs geb tests
import 'core-js/modules/es.object.assign';

import './cip-index';

import './proprietary.matchers.modal/proprietary.matchers.module';

import './cip-loader';
import './cip-component-util';
import './ci-label-tab';
import './ci-policy-violations-tab';
import './ci-license-tab';
import './ci-vulnerability-tab';
import './ci-version-graph';

import Base64 from '../lib/Base64';
window.Base64 = Base64;
