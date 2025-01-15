/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import { openVulnerabilityDetailsModal } from '../../vulnerabilityDetails/vulnerabilityDetailsModalActions';
import applicationReportActions from './../applicationReportActions';

import ApplicationReportRawData from './ApplicationReportRawData';

const mapStateToProps = ({ applicationReport, vulnerabilityDetailsModal, router }) => {
  const { reportRawData, rawDataSubstringFilters, rawDataNumericFilters } = applicationReport;
  const { derivedComponentName, licenseSortKey, securityCode } = rawDataSubstringFilters;
  const { cvssScore } = rawDataNumericFilters;

  return {
    ...pick(['loadError', 'metadata', 'rawSortConfiguration'], applicationReport),
    ...pick(['vulnerabilityId'], vulnerabilityDetailsModal),
    ...pick(['ownerId', 'scanId', 'publicId'], router.currentParams),
    displayedEntries: (reportRawData || {}).displayedEntries,
    pendingLoadsSize: applicationReport.pendingLoads.size,
    derivedComponentNameSubstringFilter: derivedComponentName,
    licenseSortKeySubstringFilter: licenseSortKey,
    securityCodeSubstringFilter: securityCode,
    cvssScore,
  };
};

const mapDispatchToProps = {
  openVulnerabilityDetailsModal,
  ...pick(
    [
      'loadReportRawData',
      'setRawDataNumericMinFilter',
      'setRawDataNumericMaxFilter',
      'setRawDataStringFieldFilter',
      'setRawSortingParameters',
    ],
    applicationReportActions()
  ),
};

export default connect(mapStateToProps, mapDispatchToProps)(ApplicationReportRawData);
