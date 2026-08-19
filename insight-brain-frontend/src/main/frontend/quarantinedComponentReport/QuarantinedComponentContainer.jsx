/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import QuarantinedComponentReport from './QuarantinedComponentReport';
import { pick } from 'ramda';
import { loadQuarantineReportData } from './quarantinedComponentReportActions';

function mapStateToProps({ router, quarantinedComponentReport }) {
  return {
    ...pick(['token'], router.currentParams),
    ...pick(
      ['loadError', 'componentOverview', 'violations', 'violationsLoading', 'violationsLoadError'],
      quarantinedComponentReport.viewState
    ),
  };
}

const mapDispatchToProps = {
  loadQuarantineReportData,
};

export default connect(mapStateToProps, mapDispatchToProps)(QuarantinedComponentReport);
