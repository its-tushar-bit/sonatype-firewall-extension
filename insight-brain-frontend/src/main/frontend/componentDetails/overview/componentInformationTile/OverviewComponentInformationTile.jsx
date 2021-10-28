/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import OverviewComponentInformation from './OverviewComponentInformation';

export default function OverviewComponentInformationTile({
  loading,
  loadError,
  loadReport,
  ...componentInformationProps
}) {
  useEffect(() => {
    loadReport();
  }, []);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadReport}>
      {() => <OverviewComponentInformation {...componentInformationProps} />}
    </NxLoadWrapper>
  );
}

OverviewComponentInformationTile.propTypes = {
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  loadReport: PropTypes.func.isRequired,
  ...OverviewComponentInformation.propTypes,
};
