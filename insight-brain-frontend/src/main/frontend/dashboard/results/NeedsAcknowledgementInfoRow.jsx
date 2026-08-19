/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxInfoAlert, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function NeedsAcknowledgementInfoRow({ colSpan }) {
  return (
    <NxTableRow>
      <NxTableCell colSpan={colSpan} metaInfo>
        <NxInfoAlert id="needs-acknowledgement">
          {"Select your filter criteria and click 'apply' to see results."}
        </NxInfoAlert>
      </NxTableCell>
    </NxTableRow>
  );
}

NeedsAcknowledgementInfoRow.propTypes = {
  colSpan: PropTypes.number.isRequired,
};
