/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import Coordinates from 'MainRoot/OrgsAndPolicies/policyEditor/constraints/Coordinates';

import 'TestRoot/SpecUtil';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('Coordinates', () => {
  let renderComponent, onFormatChangeSpy, onInputChangeSpy, fields;

  beforeEach(() => {
    onFormatChangeSpy = jest.fn().mockName('onFormatChange');
    onInputChangeSpy = jest.fn().mockName('onInputChange');
    fields = {
      format: 'maven',
      artifactId: initUserInput('artifactId'),
      classifier: initUserInput('classifier'),
      extension: initUserInput('extension'),
      groupId: initUserInput('groupId'),
      version: initUserInput('version'),
    };

    const minimalProps = {
      onFormatChange: onFormatChangeSpy,
      onInputChange: onInputChangeSpy,
      fields,
      constraintIdx: 1,
      conditionIdx: 1,
    };
    renderComponent = (additionalProps) => render(<Coordinates {...minimalProps} {...additionalProps} />);
  });

  it('renders select and 5 input fields if format is maven', () => {
    renderComponent();

    expect(screen.getByText('maven')).toBeVisible();
    expect(screen.getByDisplayValue('groupId')).toBeInTheDocument();
    expect(screen.getByDisplayValue('artifactId')).toBeInTheDocument();
    expect(screen.getByDisplayValue('version')).toBeInTheDocument();
    expect(screen.getByDisplayValue('classifier')).toBeInTheDocument();
    expect(screen.getByDisplayValue('extension')).toBeInTheDocument();
  });

  it('renders select and 3 input fields if format is a-name', () => {
    renderComponent({
      fields: {
        format: 'a-name',
        name: initUserInput('name'),
        qualifier: initUserInput('qualifier'),
        version: initUserInput('version'),
      },
    });

    expect(screen.getByText('maven')).toBeVisible();
    expect(screen.getByDisplayValue('name')).toBeInTheDocument();
    expect(screen.getByDisplayValue('qualifier')).toBeInTheDocument();
    expect(screen.getByDisplayValue('version')).toBeInTheDocument();
  });

  it('renders select and 4 input fields if format is pypi', () => {
    renderComponent({
      fields: {
        format: 'pypi',
        name: initUserInput('name'),
        version: initUserInput('version'),
        qualifier: initUserInput('qualifier'),
        extension: initUserInput('extension'),
      },
    });

    expect(screen.getByText('maven')).toBeVisible();
    expect(screen.getByDisplayValue('name')).toBeInTheDocument();
    expect(screen.getByDisplayValue('qualifier')).toBeInTheDocument();
    expect(screen.getByDisplayValue('extension')).toBeInTheDocument();
    expect(screen.getByDisplayValue('version')).toBeInTheDocument();
  });

  it('calls onFormatChange if coordinates format is changed', () => {
    renderComponent();

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'a-name' } });

    expect(onFormatChangeSpy).toHaveBeenCalled();
  });

  it('calls onInputChange if coordinates input is changed', () => {
    renderComponent();

    fireEvent.change(screen.getByDisplayValue('groupId'), { target: { value: 'new-group-id', name: 'groupId' } });

    expect(onInputChangeSpy).toHaveBeenCalledWith('new-group-id', 'groupId', 1, 1, 'maven');
  });
});
