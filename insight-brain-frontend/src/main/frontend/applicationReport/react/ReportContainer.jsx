/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import * as applicationReportActions from '../applicationReportActions';
import { connect } from 'react-redux';
import ReportPage from './ReportPage';
import { stateGo } from '../../reduxUiRouter/routerActions';

function mapStateToProps({ applicationReport, router }) {
  return {
    ...pick(
      [
        'metadata',
        'selectedReport',
        'sortConfiguration',
        'aggregate',
        'exactValueFilters',
        'substringFilters',
        'loadError',
      ],
      applicationReport
    ),
    ...pick(
      ['publicId', 'scanId', 'unknownjs', 'embeddable', 'policyViolationId'],
      router.currentParams
    ),
    loading:
      !applicationReport.loadError &&
      (!!applicationReport.pendingLoads.size || !applicationReport.metadata),
  };
}

const mapDispatchToProps = { ...applicationReportActions, stateGo };

const ReportPageContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(ReportPage);
export default ReportPageContainer;

ReportPageContainer.propTypes = pick(['$state'], ReportPage.propTypes);
