/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';

const ViolationName = ({ policyExists, policyName }) =>
  policyExists ? (
    <span>
      Violation of <em>{policyName}</em>
    </span>
  ) : (
    <strike>
      Violation of <em>{policyName}</em>
    </strike>
  );

ViolationName.propTypes = {
  policyExists: PropTypes.bool,
  policyName: PropTypes.string,
};

export default ViolationName;
