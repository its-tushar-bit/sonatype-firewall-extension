/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import AuditLogTable from './AuditLogTable';

export default function AuditLog(props) {
  const { loadComponentDetails, isLoadingComponentDetails, componentDetailsLoadError, ...tableProps } = props;

  return (
    <div className="nx-tile-content">
      <NxLoadWrapper
        loading={isLoadingComponentDetails}
        error={componentDetailsLoadError}
        retryHandler={loadComponentDetails}
      >
        {() => <AuditLogTable {...tableProps} />}
      </NxLoadWrapper>
    </div>
  );
}

AuditLog.propTypes = {
  ...AuditLogTable.propTypes,
  loadComponentDetails: PropTypes.func.isRequired,
  isLoadingComponentDetails: PropTypes.bool.isRequired,
  componentDetailsLoadError: PropTypes.string,
};
