/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import './webpackGlobals.js';

import 'components-font-awesome/css/font-awesome.min.css';
import './lib/glyphicon/glyphicons.css';
import './lib/bootstrap.scss';
import 'bootstrap-toggle/css/bootstrap2-toggle.css';
import './lib/glyphicon/halflings.css';
import 'angular-xeditable/dist/css/xeditable.css';
import 'plottable/plottable.css';
import './glyphicons-sonatype.css';
import './sonatype-icons.css';
import './scss/scss.scss';

import './lib/jquery-loader';
import 'angular';
import 'angular-aria';
import 'angular-cookies';
import 'angular-route';
import 'angular-sanitize';
import 'angular-ui-validate';
import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'angular-vs-repeat';
import 'angular-xeditable';
import './lib/bootstrap-loader';
import 'es6-collections';
import { polyfill } from 'es6-promise';

// d3 has a commonjs impl and a es6 impl.  The commonjs impl doesn't work with rollup for
// unknown reasons and the es6 one isn't detected correctly, despite the jsnext config which
// should enable it.  As a workaround we point to the specific file that has the es6 imports
// See https://stackoverflow.com/questions/39909200/d3-4-0-import-statement-gives-moduleexports-wrapper
import * as d3 from 'd3/index.js';
import pv from './lib/protovis/protovis.min';

import Fuse from 'fuse.js';
import ClassyBrew from 'classybrew/src/classybrew';

import './utility/Polyfills';
import './utilAngular/Globals';
import './ManagementApp';

import Base64 from './lib/Base64';

window.Base64 = Base64;
window.Fuse = Fuse;
window.d3 = d3;
window.classyBrew = ClassyBrew;
window.pv = pv;
polyfill();
