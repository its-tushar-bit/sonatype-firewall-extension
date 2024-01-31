/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import classnames from 'classnames';

import { NxH2, NxPolicyViolationIndicator } from '@sonatype/react-shared-components';

const ViolationDetailsTileHeaderMainTitle = ({ policyExists, policyName, threatLevelCategory }) => {
  const titleClassnames = classnames('nx-tile-header__title', {
    'nx-tile-header__title--disabled': !policyExists,
  });
  let titleThreatLevelCategory;
  let nonExistingPolicyText;
  let violationNameText = (
    <span>
      Violation of <em>{policyName}</em>
    </span>
  );

  if (policyExists) {
    titleThreatLevelCategory = threatLevelCategory;
    nonExistingPolicyText = null;
  } else {
    titleThreatLevelCategory = null;
    violationNameText = <strike>{violationNameText}</strike>;
    nonExistingPolicyText = <span>Policy no longer exists</span>;
  }

  return (
    <div className={titleClassnames}>
      <NxH2>
        {violationNameText}
        <NxPolicyViolationIndicator threatLevelCategory={titleThreatLevelCategory} />
      </NxH2>
      {nonExistingPolicyText}
    </div>
  );
};

ViolationDetailsTileHeaderMainTitle.propTypes = {
  policyExists: PropTypes.bool,
  policyName: PropTypes.string,
  threatLevelCategory: PropTypes.number,
};

export default ViolationDetailsTileHeaderMainTitle;
