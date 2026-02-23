/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ProprietaryComponentConfiguration from 'MainRoot/OrgsAndPolicies/proprietaryComponentConfig/ProprietaryComponentConfiguration';
import * as proprietarySelectors from 'MainRoot/OrgsAndPolicies/proprietarySelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/proprietarySlice';
import { render, screen, fireEvent, getAllByText, queryAllByText } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('ProprietaryComponentConfiguration', () => {
  let renderComponent,
    selectProprietaryLoadingSpy,
    selectProprietaryLoadErrorSpy,
    saveProprietaryConfigSpy,
    setMatcherValueSpy,
    addMatcherSpy,
    removeMatcherSpy;

  beforeEach(() => {
    selectProprietaryLoadingSpy = jest.spyOn(proprietarySelectors, 'selectIsLoading').mockReturnValue(false);
    selectProprietaryLoadErrorSpy = jest.spyOn(proprietarySelectors, 'selectLoadError').mockReturnValue(null);
    jest.spyOn(actions, 'loadProprietaryConfig').mockReturnValue({
      type: 'proprietary/loadProprietaryConfig/fulfilled',
      payload: {
        currentConfig: {
          id: 'b41667fa8cde4318aed6adcd4cdcd5e2',
          ownerId: 'ROOT_ORGANIZATION_ID',
          packages: ['initialPackage'],
          regexes: ['initialRegex'],
        },
        localMatchers: [
          { type: 'Package', matcher: 'initialPackage' },
          { type: 'Regular Expression', matcher: 'initialRegex' },
        ],
        proprietaryConfigs: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            proprietaryConfig: {
              id: 'b41667fa8cde4318aed6adcd4cdcd5e2',
              ownerId: 'ROOT_ORGANIZATION_ID',
              packages: ['initialPackage'],
              regexes: ['initialRegex'],
            },
          },
        ],
      },
    });

    saveProprietaryConfigSpy = jest.spyOn(actions, 'saveProprietaryConfig');
    setMatcherValueSpy = jest.spyOn(actions, 'setMatcherValue');
    addMatcherSpy = jest.spyOn(actions, 'addMatcher');
    removeMatcherSpy = jest.spyOn(actions, 'removeMatcher');

    renderComponent = () => render(<ProprietaryComponentConfiguration />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Proprietary Component Configuration')).toBeVisible();
  });

  it('renders loading indicator', () => {
    selectProprietaryLoadingSpy.mockReturnValue(true);
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders correct initial view', () => {
    renderComponent();

    const initialPackage = screen.getByText('initialPackage');
    const initialRegex = screen.getByText('initialRegex');

    const initialRegexIndicator = screen.getAllByText('RegEx');

    expect(initialRegex).toBeVisible();
    expect(initialPackage).toBeVisible();
    expect(initialRegexIndicator.length).toBe(1);
  });

  it('can not trigger save proprietary on initialize', () => {
    renderComponent();

    const updateButton = screen.getByRole('button', { name: 'Update' });
    expect(updateButton).toBeVisible();

    fireEvent.click(updateButton);
    expect(saveProprietaryConfigSpy).not.toHaveBeenCalled();
  });

  it('can not trigger save proprietary when there is duplicate', () => {
    renderComponent();

    const initialPackage = screen.getByText('initialPackage');
    expect(initialPackage).toBeVisible();

    const labelInput = screen.getAllByRole('textbox')[0];
    fireEvent.change(labelInput, { target: { value: 'initialPackage' } });
    expect(setMatcherValueSpy).toHaveBeenCalled();

    const addButton = screen.getByRole('button', { name: 'Add' });
    fireEvent.click(addButton);
    expect(addMatcherSpy).not.toHaveBeenCalled();

    const errorText = screen.getByText('Duplicate value name');
    expect(errorText).toBeVisible();
  });

  it('can not trigger save proprietary when there is invalid java name', () => {
    renderComponent();

    const initialPackage = screen.getByText('initialPackage');
    expect(initialPackage).toBeVisible();

    const labelInput = screen.getAllByRole('textbox')[0];
    fireEvent.change(labelInput, { target: { value: 'initial Package' } });
    expect(setMatcherValueSpy).toHaveBeenCalled();

    const addButton = screen.getByRole('button', { name: 'Add' });
    expect(addButton).toHaveAttribute('disabled');
    fireEvent.click(addButton);
    expect(addMatcherSpy).not.toHaveBeenCalled();

    const errorText = screen.getByText('Invalid Java package name');
    expect(errorText).toBeVisible();

    fireEvent.change(labelInput, { target: { value: '/initialPackage' } });
    expect(setMatcherValueSpy).toHaveBeenCalled();
    expect(addButton).toHaveAttribute('disabled');
    fireEvent.click(addButton);
    expect(addMatcherSpy).not.toHaveBeenCalled();

    expect(errorText).toBeVisible();

    fireEvent.change(labelInput, { target: { value: 'initialPackage/' } });
    expect(setMatcherValueSpy).toHaveBeenCalled();
    expect(addButton).toHaveAttribute('disabled');
    fireEvent.click(addButton);
    expect(addMatcherSpy).not.toHaveBeenCalled();

    expect(errorText).toBeVisible();

    fireEvent.change(labelInput, { target: { value: 'initial.Package.com' } });
    expect(setMatcherValueSpy).toHaveBeenCalled();
    expect(addButton).not.toHaveAttribute('disabled');
    fireEvent.click(addButton);
    expect(addMatcherSpy).toHaveBeenCalled();
    const errorTexts = screen.queryAllByText('Invalid Java package name');
    expect(errorTexts.length).toBe(0);
  });

  it('can not trigger save proprietary when there is incorret period', () => {
    renderComponent();

    const initialPackage = screen.getByText('initialPackage');
    expect(initialPackage).toBeVisible();

    const labelInput = screen.getAllByRole('textbox')[0];
    fireEvent.change(labelInput, { target: { value: '.initialPackage' } });
    expect(setMatcherValueSpy).toHaveBeenCalled();

    const addButton = screen.getByRole('button', { name: 'Add' });
    expect(addButton).toHaveAttribute('disabled');
    fireEvent.click(addButton);
    expect(addMatcherSpy).not.toHaveBeenCalled();

    const errorText = screen.getByText('Value cannot begin or end with a period “.”');
    expect(errorText).toBeVisible();

    fireEvent.change(labelInput, { target: { value: 'initialPackage.' } });
    expect(setMatcherValueSpy).toHaveBeenCalled();
    expect(addButton).toHaveAttribute('disabled');
    fireEvent.click(addButton);
    expect(addMatcherSpy).not.toHaveBeenCalled();

    expect(errorText).toBeVisible();
  });

  it('can trigger save proprietary', () => {
    renderComponent();

    const labelInputValue = 'packageValue';

    const labelInput = screen.getAllByRole('textbox')[0];
    fireEvent.change(labelInput, { target: { value: labelInputValue } });

    expect(setMatcherValueSpy).toHaveBeenCalled();
    expect(setMatcherValueSpy).toHaveBeenCalledWith(labelInputValue);
    expect(screen.getByDisplayValue(labelInputValue)).toBeVisible();

    const addButton = screen.getByRole('button', { name: 'Add' });
    const updateButton = screen.getByRole('button', { name: 'Update' });

    expect(addButton).toBeVisible();
    expect(updateButton).toBeVisible();

    fireEvent.click(addButton);
    expect(addMatcherSpy).toHaveBeenCalled();

    expect(addButton).toHaveAttribute('disabled');

    fireEvent.click(updateButton);
    expect(saveProprietaryConfigSpy).toHaveBeenCalled();
  });

  it('can trigger save proprietary on delete matcher', async () => {
    SpecUtil.requestIdleCallbackInvokeImmediateJest();

    renderComponent();

    const initialRegex = screen.queryAllByText('initialRegex');
    expect(initialRegex.length).toBe(1);

    const deleteButton = (await screen.findAllByRole('button', { name: 'Delete' }))[1];
    expect(deleteButton).toBeVisible();

    fireEvent.click(deleteButton);
    expect(removeMatcherSpy).toHaveBeenCalled();

    const initialRegexReinitialize = screen.queryAllByText('initialRegex');
    expect(initialRegexReinitialize.length).toBe(0);

    const updateButton = screen.getByRole('button', { name: 'Update' });

    fireEvent.click(updateButton);
    expect(saveProprietaryConfigSpy).toHaveBeenCalled();
  });

  it('renders list correct on add matcher', () => {
    renderComponent();

    const labelInputValue = 'packageValue';

    const labelInput = screen.getAllByRole('textbox')[0];
    fireEvent.change(labelInput, { target: { value: labelInputValue } });

    expect(setMatcherValueSpy).toHaveBeenCalled();
    expect(setMatcherValueSpy).toHaveBeenCalledWith(labelInputValue);

    const inputValue = screen.getAllByDisplayValue(labelInputValue);
    expect(inputValue.length).toBe(1);

    const addButton = screen.getByRole('button', { name: 'Add' });
    fireEvent.click(addButton);

    const localList = screen.getByRole('list', { name: 'Local' });
    const packageInListName = getAllByText(localList, labelInputValue);
    expect(packageInListName.length).toBe(1);
  });

  it('shows error message on error', () => {
    selectProprietaryLoadErrorSpy.mockReturnValue('Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });
});

describe('ProprietaryComponentConfiguration inheritance test', () => {
  let renderComponent;

  beforeEach(() => {
    jest.spyOn(actions, 'loadProprietaryConfig').mockReturnValue({
      type: 'proprietary/loadProprietaryConfig/fulfilled',
      payload: {
        currentConfig: {
          id: 'a41667fa8cde4318aed6adcd4cdcd5e2',
          ownerId: 'APP_ID',
          packages: ['initialPackage'],
          regexes: ['initialRegex'],
        },
        localMatchers: [
          { type: 'Package', matcher: 'initialPackage' },
          { type: 'Regular Expression', matcher: 'initialRegex' },
        ],
        proprietaryConfigs: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            proprietaryConfig: {
              id: 'b41667fa8cde4318aed6adcd4cdcd5e2',
              ownerId: 'ROOT_ORGANIZATION_ID',
              packages: ['inheritedInitialPackage'],
              regexes: ['inheritedInitialRegex'],
            },
          },
          {
            ownerId: 'APP_ID',
            ownerName: 'Some App',
            ownerType: 'organization',
            proprietaryConfig: {
              id: 'a41667fa8cde4318aed6adcd4cdcd5e2',
              ownerId: 'APP_ID',
              packages: ['initialPackage'],
              regexes: ['initialRegex'],
            },
          },
        ],
      },
    });

    renderComponent = () => render(<ProprietaryComponentConfiguration />);
  });

  it('renders inherited list correct on initialize', () => {
    renderComponent();

    const inheritedSubtitle = screen.getByText('Inherited from Root Organization');
    expect(inheritedSubtitle).toBeVisible();

    const listCount = screen.getAllByRole('list');
    expect(listCount.length).toBe(2);

    const localList = screen.getByRole('list', { name: 'Local' });
    const inheritedList = screen.getByRole('list', { name: 'Inherited from Root Organization' });

    expect(localList).toHaveClass('local-proprietary-component-matchers');
    expect(inheritedList.parentElement).toHaveClass('inherited-proprietary-component-matchers');

    const inheritedPackage = getAllByText(inheritedList, 'inheritedInitialPackage');
    expect(inheritedPackage.length).toBe(1);
    expect(queryAllByText(localList, 'inheritedInitialPackage').length).toBe(0);

    const inheritedRegex = getAllByText(inheritedList, 'inheritedInitialRegex');
    expect(inheritedRegex.length).toBe(1);
    expect(queryAllByText(localList, 'inheritedInitialRegex').length).toBe(0);

    const localPachage = getAllByText(localList, 'initialPackage');
    expect(localPachage.length).toBe(1);
    expect(queryAllByText(inheritedList, 'initialPackage').length).toBe(0);

    const localRegex = getAllByText(localList, 'initialRegex');
    expect(localRegex.length).toBe(1);
    expect(queryAllByText(inheritedList, 'initialRegex').length).toBe(0);

    const totalRegexes = screen.getAllByText('RegEx');
    expect(totalRegexes.length).toBe(2);
  });
});
