/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import ReadOnlyConstraint from 'MainRoot/OrgsAndPolicies/policyEditor/constraints/ReadOnlyConstraint';

import 'TestRoot/SpecUtil';

describe('ReadOnlyConstraint', () => {
  let renderComponent, updateEditConstraintIdSpy, deleteConstraintSpy, constraint, conditionTypesMap;

  beforeEach(() => {
    updateEditConstraintIdSpy = jest.fn().mockName('updateEditConstraintId');
    deleteConstraintSpy = jest.fn().mockName('deleteConstraint');
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
      ],
    };

    const minimalProps = {
      constraint,
      constraintIdx: 0,
      cannotBeRemoved: true,
      readOnly: true,
      conditionTypesMap,
      updateEditConstraintId: updateEditConstraintIdSpy,
      deleteConstraint: deleteConstraintSpy,
    };

    renderComponent = (additionalProps) => render(<ReadOnlyConstraint {...minimalProps} {...additionalProps} />);
  });

  it('renders read-only constraint name', () => {
    renderComponent();
    expect(screen.getByText('third')).toBeVisible();
  });

  it('renders read-only constraint subheader  when operator = OR', () => {
    renderComponent();
    expect(screen.getByText('is in violation if any of the following are true:')).toBeVisible();
  });

  it('renders read-only constraint subheader when operator = AND', () => {
    constraint.operator = 'AND';
    renderComponent();
    expect(screen.getByText('is in violation if all of the following are true:')).toBeVisible();
  });

  it('renders read-only constraint conditions', () => {
    renderComponent();
    const conditions = screen.getAllByRole('listitem');

    expect(within(conditions[1]).getByText('Format is composer')).toBeVisible();
    expect(within(conditions[2]).getByText('Age older than 2 Years')).toBeVisible();
  });

  it('renders error alert if condition is not supported', () => {
    renderComponent();
    expect(
      screen.getByText('Format condition is not supported by your license. Please revise the constraint.')
    ).toBeVisible();
  });

  it('renders one combined error alert if multiple condition are not supported for the constraint', () => {
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

  it('does not call delete constraint handler if constraint cannot be removed', () => {
    renderComponent();

    const deleteButton = screen.getAllByRole('button')[1];
    fireEvent.click(deleteButton);

    expect(deleteConstraintSpy).not.toHaveBeenCalled();
  });

  it('calls delete constraint handler if constraint can be removed', () => {
    const constraintIdx = 0;
    renderComponent({ cannotBeRemoved: false });

    const deleteButton = screen.getAllByRole('button')[1];
    fireEvent.click(deleteButton);

    expect(deleteConstraintSpy).toHaveBeenCalledWith(constraintIdx);
  });

  it('does not call update constraint id handler if constraint is read only', () => {
    renderComponent();

    const editButton = screen.getAllByRole('button')[0];
    fireEvent.click(editButton);

    expect(updateEditConstraintIdSpy).not.toHaveBeenCalled();
  });

  it('calls update constraint id handler if constraint can be edited', () => {
    const constraintId = '202';
    renderComponent({ readOnly: false });

    const editButton = screen.getAllByRole('button')[0];
    fireEvent.click(editButton);

    expect(updateEditConstraintIdSpy).toHaveBeenCalledWith(constraintId);
  });
});
