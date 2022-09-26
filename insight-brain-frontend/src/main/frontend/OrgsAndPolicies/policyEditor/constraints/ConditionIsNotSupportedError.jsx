/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { join } from 'ramda';
import { NxErrorAlert } from '@sonatype/react-shared-components';
import { getUnsupportedConditions } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';

export default function ConditionIsNotSupportedError({ constraint, conditionTypesMap }) {
  const list = getUnsupportedConditions(conditionTypesMap, constraint);
  const hasUnsupportedConditions = list.size > 0;
  const multiConditions = list.size > 1;

  return hasUnsupportedConditions ? (
    <NxErrorAlert>
      {join(', ', Array.from(list))} {multiConditions ? 'conditions are' : 'condition is'} not supported by your
      license. Please revise the constraint.
    </NxErrorAlert>
  ) : null;
}

ConditionIsNotSupportedError.propTypes = {
  constraint: PropTypes.object,
  conditionTypesMap: PropTypes.object,
};
