/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import ComponentDetails from './ComponentDetails';
import { loadReportAndSelectComponentByHash } from '../applicationReport/applicationReportActions';

function mapStateToProps(state) {
  const {
    router: {
      currentParams: { hash, publicId, scanId, unknownjs },
    },
    applicationReport: { selectedComponent, selectedReport },
  } = state;

  return { hash, publicId, scanId, unknownjs, selectedReport, selectedComponent };
}

const mapDispatchToProps = { loadReportAndSelectComponentByHash };

const ComponentDetailsContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentDetails);
export default ComponentDetailsContainer;
