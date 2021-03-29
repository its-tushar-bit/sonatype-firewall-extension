/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {useState} from 'react';
import * as PropTypes from 'prop-types';

import {
  NxOverflowTooltip,
  NxFilterInput,
  NxDropdown
} from '@sonatype/react-shared-components';
import './DropdownFilterInput.scss';

/**
 * A Dropdown with a Filter Input element which operates on the list of elements in the dropdown
 */
function DropdownFilterInput(props) {
  const {
    children,
    filterFn
  } = props;

  const filteredChildren = () => {
    if (!filterFn || !children) {
      return children;
    }
    return children.filter(child => filterFn(child, filterValue));
  };

  const [filterValue, setFilterValue] = useState('');

  const wrappedChildren = filteredChildren() && React.Children.map(filteredChildren(), child => (
    <NxOverflowTooltip>{child}</NxOverflowTooltip>
  ));
  return (
    <NxDropdown {...props}>
      <NxFilterInput className="nx-dropdown-menu-filter" onChange={setFilterValue} value={filterValue}/>
      {wrappedChildren}
    </NxDropdown>
  );
}

DropdownFilterInput.propTypes = {
  children: PropTypes.arrayOf(PropTypes.element),
  filterFn: PropTypes.func
};

export default DropdownFilterInput;
