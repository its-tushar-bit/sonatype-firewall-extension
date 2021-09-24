/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxInfoAlert } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

const InnerSourceProducerAlert = ({ innerSourceProducerData, isInnerSource, onClick }) => {
  const { loading, loadError } = innerSourceProducerData;
  const showInnerSourceProducerAlert = isInnerSource && !loading && !loadError;

  if (!showInnerSourceProducerAlert) {
    return null;
  }

  return (
    <NxInfoAlert id="inner-source-producer-alert">
      This Component was brought in by an Innersource Component. Innersource Components are software components that are
      developed internally and shared with other internal projects.{' '}
      <a className="nx-text-link" onClick={onClick}>
        View the latest report
      </a>{' '}
      of the application that produced this Innersource Component.
    </NxInfoAlert>
  );
};

InnerSourceProducerAlert.propTypes = {
  onClick: PropTypes.func.isRequired,
  isInnerSource: PropTypes.bool.isRequired,
  innerSourceProducerData: PropTypes.shape({
    loading: PropTypes.bool.isRequired,
    loadError: PropTypes.bool,
    reportUrl: PropTypes.string,
    insufficientPermission: PropTypes.bool,
    latestInnerSourceComponentVersion: PropTypes.string,
    showInnerSourcePermissionsModal: PropTypes.bool,
    showInnerSourceProducerReportModal: PropTypes.bool,
  }),
};

export default InnerSourceProducerAlert;
