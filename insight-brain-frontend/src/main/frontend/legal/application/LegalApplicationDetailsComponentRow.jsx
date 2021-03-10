/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function LegalApplicationDetailsComponentRow({ row }) {

  return (
    <NxTableRow key={ row.applicationName }>
      <NxTableCell>1</NxTableCell>
      <NxTableCell>2</NxTableCell>
      <NxTableCell>3</NxTableCell>
      <NxTableCell>4</NxTableCell>
    </NxTableRow>
  );
}

LegalApplicationDetailsComponentRow.propTypes = {
  row: PropTypes.any
};
