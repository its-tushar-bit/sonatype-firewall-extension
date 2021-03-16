/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function MaxResultsInfoRow({colSpan, maxResults}) {
  return (
    <NxTableRow>
      <NxTableCell colSpan={colSpan} metaInfo>
        <span id="max-results-shown">First {maxResults} results shown</span>
      </NxTableCell>
    </NxTableRow>
  );
}

MaxResultsInfoRow.propTypes = {
  colSpan: PropTypes.number.isRequired,
  maxResults: PropTypes.number.isRequired
};
