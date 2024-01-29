/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useState } from 'react';
import * as PropTypes from 'prop-types';

import { NxFilterInput, NxDropdown } from '@sonatype/react-shared-components';
import './DropdownFilterInput.scss';

/**
 * A Dropdown with a Filter Input element which operates on the list of elements in the dropdown
 */
function DropdownFilterInput(props) {
  const { children, filterFn, onToggleCollapse: onToggleCollapseProp, isOpen, ...nxDropdownProps } = props;

  const [filterValue, setFilterValue] = useState('');

  // Intercept toggle collapse
  // so that NxDropdown does not close menu
  // when filter is focused.
  const onToggleCollapse = () => {
    if (!isOpen || !document.activeElement?.closest('.nx-dropdown-menu-filter')) {
      onToggleCollapseProp();
    }
  };

  const filterChildren = () => {
    if (!children || !filterFn) {
      // don't need a filter if no children or no filter function
      return children;
    }
    return [
      <NxFilterInput
        key="__filter"
        className="nx-dropdown-menu-filter"
        onChange={setFilterValue}
        value={filterValue}
      />,
      ...children.filter((child) => filterFn(child, filterValue)),
    ];
  };

  return (
    <NxDropdown {...nxDropdownProps} isOpen={isOpen} onToggleCollapse={onToggleCollapse}>
      {filterChildren()}
    </NxDropdown>
  );
}

DropdownFilterInput.propTypes = {
  children: PropTypes.arrayOf(PropTypes.element),
  filterFn: PropTypes.func,
  nxDropdownProps: PropTypes.object,
};

export default DropdownFilterInput;
