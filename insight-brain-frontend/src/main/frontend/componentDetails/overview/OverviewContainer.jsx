/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import Overview from './Overview';
import { selectSelectedComponent } from '../../applicationReport/applicationReportSelectors';
import { selectComponentAncestors } from '../componentDetailsSelectors';
import { selectCurrentRouteName } from '../../reduxUiRouter/routerSelectors';

function mapStateToProps(state) {
  return {
    componentInformation: selectSelectedComponent(state),
    ancestors: selectComponentAncestors(state),
    routeName: selectCurrentRouteName(state),
  };
}

const mapDispatchToProps = {};

export const OverviewContainer = connect(mapStateToProps, mapDispatchToProps)(Overview);
