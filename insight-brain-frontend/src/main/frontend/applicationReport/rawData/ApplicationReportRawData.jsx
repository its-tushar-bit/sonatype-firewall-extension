/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxInfoAlert } from '@sonatype/react-shared-components';
import BackButton from 'MainRoot/applicationReport/BackButton';
import VulnerabilityDetailsModal from '../../vulnerabilityDetails/VulnerabilityDetailsModalContainer';
import ApplicationReportRawDataTable, { tablePropTypes } from './ApplicationReportRawDataTable';
import ApplicationReportRawDataHeader, { metadataPropType } from './ApplicationReportRawDataHeader';

export default function ApplicationReportRawData(props) {
  const { metadata, loadReportRawData, openVulnerabilityDetailsModal, scanId, publicId } = props;

  useEffect(() => {
    loadReportRawData();
  }, []);

  function openModal(entry) {
    const { securityCode, componentIdentifier, identificationSource } = entry;
    openVulnerabilityDetailsModal({
      vulnerabilityId: securityCode,
      componentIdentifier,
      extraQueryParameters: {
        identificationSource: identificationSource,
        scanId,
        ownerId: publicId,
        ownerType: 'application',
      },
    });
  }

  return (
    <main id="application-report-raw-data" className="nx-page-main nx-viewport-sized">
      <BackButton />
      {metadata && <ApplicationReportRawDataHeader metadata={metadata} />}
      <NxInfoAlert>
        Please note that the data appearing on this page is the raw data and not the result of policy evaluation
      </NxInfoAlert>
      <ApplicationReportRawDataTable {...props} openModal={openModal} />
      <VulnerabilityDetailsModal />
    </main>
  );
}

const routerCurrentParams = {
  ownerType: PropTypes.string,
  ownerId: PropTypes.string,
  scanId: PropTypes.string,
};

ApplicationReportRawData.propTypes = {
  metadata: metadataPropType,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  selectComponent: PropTypes.func,
  ...routerCurrentParams,
  ...tablePropTypes,
};
