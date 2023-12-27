/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxStatefulFilterDropdown } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function SastScanFindingsFilter({ className, options, selectedIds, onChange }) {
  return (
    <div className={className}>
      <NxStatefulFilterDropdown options={options} selectedIds={selectedIds} onChange={onChange} />
    </div>
  );
}

SastScanFindingsFilter.propTypes = {
  className: PropTypes.string,
  options: PropTypes.array.isRequired,
  selectedIds: PropTypes.any.isRequired,
  onChange: PropTypes.func.isRequired,
};
