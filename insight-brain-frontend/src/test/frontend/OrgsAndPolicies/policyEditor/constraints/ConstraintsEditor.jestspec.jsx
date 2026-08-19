/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';

import ConstraintsEditor from 'MainRoot/OrgsAndPolicies/policyEditor/constraints/ConstraintsEditor';
import { lensPath, set } from 'ramda';

describe('ConstraintsEditor', () => {
  let renderComponent;
  let editConstraintMap = {
    1660254072492: true,
    1660254145186: true,
  };
  const constraints = [
    {
      id: '1660254072492',
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: {
            isPristine: false,
            value: '7',
            trimmedValue: '7',
            validationErrors: [],
          },
        },
      ],
      operator: 'OR',
      name: {
        isPristine: false,
        value: 'New Age constraint',
        trimmedValue: 'New Age constraint',
        validationErrors: [],
      },
    },
    {
      id: '1660254145186',
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'younger than',
          value: {
            isPristine: false,
            value: '1825',
            trimmedValue: '1825',
            validationErrors: [],
          },
        },
      ],
      operator: 'AND',
      name: {
        isPristine: false,
        value: 'New Age constraint 2',
        trimmedValue: 'New Age constraint 2',
        validationErrors: [],
      },
    },
  ];
  const orgsAndPoliciesInitialState = {
    policy: {
      currentPolicy: {
        constraints,
      },
      originalPolicy: {
        constraints: [],
      },
      isInherited: false,
      hasEditIqPermission: true,
    },
    constraint: {
      editConstraintMap,
      conditionTypesMap: {
        AgeInDays: {
          enabled: true,
          threatCategory: 'QUALITY',
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          supportedOperators: ['older than', 'younger than'],
          name: 'Age',
          id: 'AgeInDays',
          valueType: {
            dataType: 'Integer',
            availableValues: null,
            allowMultiple: false,
            id: 'AgeInDaysValueType',
          },
        },
      },
      conditionTypes: [
        {
          enabled: true,
          threatCategory: 'QUALITY',
          valueTypeId: 'AgeInDaysValueType',
          valueHint: 'Enter term',
          autoUnquarantineSupported: false,
          supportedOperators: ['older than', 'younger than'],
          name: 'Age',
          id: 'AgeInDays',
          valueType: {
            dataType: 'Integer',
            availableValues: null,
            allowMultiple: false,
            id: 'AgeInDaysValueType',
          },
        },
      ],
    },
  };
  const editConstraintMapLens = lensPath(['constraint', 'editConstraintMap']);
  beforeEach(() => {
    renderComponent = (preloadedState) => render(<ConstraintsEditor />, { preloadedState });
  });

  it('renders editable constraints', () => {
    renderComponent({ orgsAndPolicies: orgsAndPoliciesInitialState });

    const constraintListReadOnly = screen.queryAllByTestId('read-only-constraint');
    const constraintListEditable = screen.getAllByTestId('editable-constraint');
    expect(constraintListReadOnly.length).toBe(0);
    expect(constraintListEditable.length).toBe(2);
  });

  it('renders disabled if there is no permission', () => {
    editConstraintMap = {
      1660254072492: true,
    };

    renderComponent({
      orgsAndPolicies: {
        ...orgsAndPoliciesInitialState,
        policy: { ...orgsAndPoliciesInitialState.policy, hasEditIqPermission: false },
        constraint: { ...orgsAndPoliciesInitialState.constraint, editConstraintMap },
      },
    });

    const addConstraintBtn = screen.getByText('Add Constraint').closest('button');
    expect(addConstraintBtn).toBeDisabled();

    const constraintListReadOnly = screen.getAllByTestId('read-only-constraint');
    expect(constraintListReadOnly.length).toBe(1);
    const constraintListReadOnlyButtons = within(constraintListReadOnly[0]).getAllByRole('button');
    expect(constraintListReadOnlyButtons.length).toBe(2);
    const addConstraintButton = constraintListReadOnlyButtons[0];
    expect(addConstraintButton).toBeDisabled();
    let deleteConstraintButton = constraintListReadOnlyButtons[1];
    expect(deleteConstraintButton).toBeDisabled();

    const constraintListEditable = screen.getAllByTestId('editable-constraint');
    expect(constraintListEditable.length).toBe(1);
    const constraintListEditableButtons = within(constraintListEditable[0]).getAllByRole('button');
    expect(constraintListEditableButtons.length).toBe(3);
    deleteConstraintButton = constraintListEditableButtons[0];
    expect(deleteConstraintButton).toBeDisabled();
  });

  it('renders disabled if the user is using SBOM Manager', () => {
    editConstraintMap = {};
    renderComponent({
      orgsAndPolicies: set(editConstraintMapLens, editConstraintMap, orgsAndPoliciesInitialState),
      router: {
        currentState: {
          name: 'sbomManager.management.edit.organization.policy',
        },
      },
    });

    const addConstraintBtn = screen.getByText('Add Constraint').closest('button');
    expect(addConstraintBtn).toBeDisabled();

    const constraintListReadOnly = screen.getAllByTestId('read-only-constraint');
    expect(constraintListReadOnly.length).toBe(2);
    const constraintListReadOnlyButtons = within(constraintListReadOnly[0]).getAllByRole('button');
    expect(constraintListReadOnlyButtons.length).toBe(2);
    const addConstraintButton = constraintListReadOnlyButtons[0];
    expect(addConstraintButton).toBeDisabled();
    let deleteConstraintButton = constraintListReadOnlyButtons[1];
    expect(deleteConstraintButton).toBeDisabled();
  });

  it('renders editable and read only constraints', () => {
    editConstraintMap = {
      1660254072492: true,
    };

    renderComponent({ orgsAndPolicies: set(editConstraintMapLens, editConstraintMap, orgsAndPoliciesInitialState) });

    const constraintListReadOnly = screen.getAllByTestId('read-only-constraint');
    const constraintListEditable = screen.getAllByTestId('editable-constraint');
    expect(constraintListReadOnly.length).toBe(1);
    expect(constraintListEditable.length).toBe(1);
  });

  it('renders read only constraints', () => {
    editConstraintMap = {};
    renderComponent({ orgsAndPolicies: set(editConstraintMapLens, editConstraintMap, orgsAndPoliciesInitialState) });

    const constraintListReadOnly = screen.getAllByTestId('read-only-constraint');
    const constraintListEditable = screen.queryAllByTestId('editable-constraint');
    expect(constraintListReadOnly.length).toBe(2);
    expect(constraintListEditable.length).toBe(0);
  });

  it('adds a constraint when the add constraint button is clicked', () => {
    renderComponent({ orgsAndPolicies: orgsAndPoliciesInitialState, productFeatures: { productFeatures: { 'custom-policies': true } } });

    let constraintListEditable = screen.getAllByTestId('editable-constraint');
    const addConstraintBtn = screen.getByText('Add Constraint');
    expect(constraintListEditable.length).toBe(2);

    fireEvent.click(addConstraintBtn);

    constraintListEditable = screen.getAllByTestId('editable-constraint');
    expect(constraintListEditable.length).toBe(3);
  });

  it('removes a constraint when clicking the delete button', () => {
    renderComponent({ orgsAndPolicies: orgsAndPoliciesInitialState, productFeatures: { productFeatures: { 'custom-policies': true } } });

    let constraintListEditable = screen.getAllByTestId('editable-constraint');
    const deleteConstraintBtn = within(constraintListEditable[0]).getAllByRole('button')[0];
    expect(constraintListEditable.length).toBe(2);

    fireEvent.click(deleteConstraintBtn);

    constraintListEditable = screen.getAllByTestId('editable-constraint');
    expect(constraintListEditable.length).toBe(1);
  });

  describe('Pro Tier Gating', () => {
    it('disables Add Constraint button when custom-policies feature is absent', () => {
      renderComponent({ orgsAndPolicies: orgsAndPoliciesInitialState, productFeatures: { productFeatures: {} }, productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } } });
      const addConstraintBtn = screen.getByText('Add Constraint');
      expect(addConstraintBtn.closest('button')).toBeDisabled();
    });

    it('makes constraints read-only when custom-policies feature is absent', () => {
      renderComponent({ orgsAndPolicies: orgsAndPoliciesInitialState, productFeatures: { productFeatures: {} }, productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } } });
      const constraintListEditable = screen.getAllByTestId('editable-constraint');
      expect(constraintListEditable.length).toBe(2);
    });
  });
});
