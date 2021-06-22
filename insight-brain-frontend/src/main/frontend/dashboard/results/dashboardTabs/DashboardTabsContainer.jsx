/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import DashboardTabs from './DashboardTabs.jsx';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { prop } from 'ramda';

export default connect(prop('dashboard'), { stateGo })(DashboardTabs);
