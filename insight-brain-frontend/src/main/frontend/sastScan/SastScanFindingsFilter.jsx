/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxFormGroup, NxStatefulFilterDropdown } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function SastScanFindingsFilter({ className, title, options, selectedIds, onChange }) {
  return (
    <NxFormGroup className={className} label={title}>
      <NxStatefulFilterDropdown options={options} selectedIds={selectedIds} onChange={onChange} />
    </NxFormGroup>
  );
}

SastScanFindingsFilter.propTypes = {
  className: PropTypes.string,
  title: PropTypes.string.isRequired,
  options: PropTypes.array.isRequired,
  selectedIds: PropTypes.any.isRequired,
  onChange: PropTypes.func.isRequired,
};
