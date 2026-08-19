/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import { waiverType } from 'MainRoot/util/waiverUtils';
import WaiverRow from 'MainRoot/waivers/WaiverRow';

export default function ComponentWaiversPopoverTable({
  componentName,
  componentNameWithoutVersion,
  waivers = [],
  setWaiverToDelete,
}) {
  return (
    <NxTable className="iq-policy-violations-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell className="iq-waivers-table__duration">DURATION</NxTableCell>
          <NxTableCell>WAIVER DETAILS</NxTableCell>
          <NxTableCell> </NxTableCell>
        </NxTableRow>
      </NxTableHead>
      <NxTableBody emptyMessage="No existing component waivers">
        {waivers?.map((waiver) => (
          <WaiverRow
            waiver={waiver}
            deleteWaiver={() => setWaiverToDelete(waiver)}
            key={waiver.policyWaiverId}
            componentName={waiver.componentName ? waiver.componentName : componentName}
            componentNameWithoutVersion={componentNameWithoutVersion}
          />
        ))}
      </NxTableBody>
    </NxTable>
  );
}

ComponentWaiversPopoverTable.propTypes = {
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  setWaiverToDelete: PropTypes.func.isRequired,
};
