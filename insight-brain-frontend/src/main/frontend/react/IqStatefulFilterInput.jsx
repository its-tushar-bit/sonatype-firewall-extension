/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { NxFilterInput } from '@sonatype/react-shared-components';

const noop = () => {};
export default function IqStatefulFilterInput({ onChange = noop, defaultValue = '', ...props }) {
  const [value, setValue] = useState(defaultValue);

  const handleOnChange = (updatedValue) => {
    setValue(updatedValue);
    onChange(updatedValue);
  };

  return <NxFilterInput value={value} onChange={handleOnChange} {...props} />;
}

IqStatefulFilterInput.propTypes = {
  onChange: PropTypes.func,
  defaultValue: PropTypes.string,
};
