/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';

import ConditionIsNotSupportedError from 'MainRoot/OrgsAndPolicies/policyEditor/constraints/ConditionIsNotSupportedError';

describe('ConditionIsNotSupportedError', () => {
  let renderComponent, conditionTypesMap, constraint;

  beforeEach(() => {
    conditionTypesMap = {
      AgeInDays: {
        enabled: true,
        id: 'AgeInDays',
        name: 'Age',
      },
      ComponentFormat: {
        enabled: false,
        id: 'ComponentFormat',
        name: 'Format',
      },
    };

    constraint = {
      id: '202',
      name: {
        isPristine: true,
        trimmedValue: 'third',
        validationErrors: null,
        value: 'third',
      },
      operator: 'OR',
      conditions: [
        {
          conditionIndex: 0,
          conditionTypeId: 'ComponentFormat',
          operator: 'is',
          value: {
            isPristine: true,
            trimmedValue: 'composer',
            validationErrors: null,
            value: 'composer',
          },
        },
        {
          conditionIndex: 2,
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: {
            isPristine: true,
            trimmedValue: '730',
            validationErrors: null,
            value: '730',
          },
        },
        {
          conditionIndex: 3,
          conditionTypeId: 'AgeInDays',
          operator: 'younger than',
          value: {
            isPristine: true,
            trimmedValue: '120',
            validationErrors: null,
            value: '120',
          },
        },
      ],
    };

    const minimalProps = {
      constraint,
      conditionTypesMap,
    };

    renderComponent = (additionalProps) =>
      render(<ConditionIsNotSupportedError {...minimalProps} {...additionalProps} />);
  });

  it('renders error alert if condition is not supported', () => {
    renderComponent();
    expect(
      screen.getByText('Format condition is not supported by your license. Please revise the constraint.')
    ).toBeVisible();
  });

  it('renders combined error alert with unique names if conditions are not supported', () => {
    renderComponent({
      conditionTypesMap: {
        AgeInDays: {
          enabled: false,
          id: 'AgeInDays',
          name: 'Age',
        },
        ComponentFormat: {
          enabled: false,
          id: 'ComponentFormat',
          name: 'Format',
        },
      },
    });
    expect(
      screen.getByText('Format, Age conditions are not supported by your license. Please revise the constraint.')
    ).toBeVisible();
  });
});
