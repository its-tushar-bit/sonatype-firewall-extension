/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../webpackGlobals';
import './clmEndpoint';

import './lib/slickgrid/slick.grid.css';
import './slickgrid/slick.grid.custom.css';
import './lib/slickgrid/slick.pager.css';
import './report.css';

import '../utility/Polyfills';
import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'dateformat';
import './lib/slickgrid/jquery.event.drag-2.3.0';

import '../version-graph/appcheck';
import './insight';
import './table';

import './lib/slickgrid/slick.core';
import './lib/slickgrid/slick.grid';
import './lib/slickgrid/slick.dataview';
import './lib/slickgrid/slick.groupitemmetadataprovider';
import './lib/slickgrid/slick.pager';
import './lib/slickgrid/slick.rowselectionmodel';
import './lib/slickgrid/slick.checkboxselectcolumn';

import './slickgrid/column-grouping';
import './slickgrid/filter';
import './slickgrid/sort';

// repository-cip
import './cip/SlickGrid.ComponentInformationPlugin';

import './audit.module/audit.module';
