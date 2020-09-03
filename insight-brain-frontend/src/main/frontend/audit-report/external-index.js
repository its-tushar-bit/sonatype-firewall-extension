/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../lib/jquery-loader';
import './lib/jquery/jquery.browser';
import 'angular';
import 'angular-aria';
import 'angular-cookies';
import 'angular-route';
import 'angular-sanitize';
import '../lib/bootstrap-loader';
import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'angular-vs-repeat';
import 'angular-xeditable';
import 'fuse.js';
import pv from '../lib/protovis/protovis.min';

import Base64 from '../lib/Base64';
window.Base64 = Base64;
window.pv = pv;
