/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

const InnerSourceProducerAlert = ({ innerSourceProducerData, isInnerSource, ownerApplicationName, onClick }) => {
  const { loading, loadError } = innerSourceProducerData;
  const showInnerSourceProducerAlert = isInnerSource && ownerApplicationName && !loading && !loadError;

  if (!showInnerSourceProducerAlert) {
    return null;
  }

  return (
    <NxInfoAlert id="inner-source-producer-alert">
      This InnerSource component was produced by the application <b>{ownerApplicationName}</b>.{' '}
      <NxTextLink onClick={onClick}>View the latest report</NxTextLink>{' '}
    </NxInfoAlert>
  );
};

InnerSourceProducerAlert.propTypes = {
  onClick: PropTypes.func.isRequired,
  isInnerSource: PropTypes.bool.isRequired,
  ownerApplicationName: PropTypes.string,
  innerSourceProducerData: PropTypes.shape({
    loading: PropTypes.bool.isRequired,
    loadError: PropTypes.string,
    reportUrl: PropTypes.string,
    insufficientPermission: PropTypes.bool,
    latestInnerSourceComponentVersion: PropTypes.string,
    showInnerSourcePermissionsModal: PropTypes.bool,
    showInnerSourceProducerReportModal: PropTypes.bool,
  }),
};

export default InnerSourceProducerAlert;
