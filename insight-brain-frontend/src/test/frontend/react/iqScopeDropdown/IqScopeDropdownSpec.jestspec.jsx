/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';

import { render, within, screen, fireEvent } from 'TestRoot/SpecUtil';

describe('IqScopeDropdown', () => {
  const availableScopes = [
    {
      id: 'id1',
      name: 'target1',
      label: 'Application',
      type: 'application',
    },
    {
      id: 'id2',
      name: 'target2',
      label: 'Organization',
      type: 'organization',
    },
    {
      id: 'ROOT_ORGANIZATION_ID',
      name: 'target3',
      label: 'Organization',
      type: 'organization',
    },
  ];
  const onChangeHandlerSpy = jest.fn();
  const getOptionText = ({ label, name }) => (label === 'Repository_container' ? name : `${label} - ${name}`);

  const renderComponent = () =>
    render(
      <IqScopeDropdown
        id="iq-scope-dropdown-id"
        onChangeHandler={onChangeHandlerSpy}
        availableScopes={availableScopes}
        getOptionText={getOptionText}
      />
    );

  it('renders proper number of options', () => {
    renderComponent();

    const select = screen.getByRole('combobox');
    const options = within(select).getAllByRole('option');

    expect(options.length).toBe(3);
    expect(options[0]).toBeVisible();
    expect(options[0]).toHaveTextContent('Application - target1');
    expect(options[1]).toBeVisible();
    expect(options[1]).toHaveTextContent('Organization - target2');
    expect(options[2]).toBeVisible();
    expect(options[2]).toHaveTextContent('Organization - target3');
  });

  it('renders selected value if option was changed', () => {
    renderComponent();

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'id2' } });

    expect(screen.getByRole('combobox')).toHaveValue('id2');
  });

  it('calls onChange handler if option is changed', () => {
    renderComponent();

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'id2' } });

    expect(onChangeHandlerSpy).toHaveBeenCalledWith('id2');
  });
});
