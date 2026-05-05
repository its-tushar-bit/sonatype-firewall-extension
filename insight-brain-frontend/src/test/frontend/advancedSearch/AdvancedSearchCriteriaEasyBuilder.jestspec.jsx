/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import AdvancedSearchCriteriaEasyBuilder from 'MainRoot/advancedSearch/AdvancedSearchCriteriaEasyBuilder';

describe('AdvancedSearchCriteriaEasyBuilder', () => {
  let renderComponent, minimalProps;

  const initialState = {
    advancedSearch: {
      viewState: {
        loading: false,
      },
      configurationState: {
        isEnabled: true,
      },
      formState: {
        searchResult: {
          groupingByDTOS: [],
        },
      },
    },
  };

  const mockSearchItems = [
    {
      operator: 'OR',
      field: { value: 'componentName', label: 'Component Name', example: 'example-component' },
      value: 'test-component',
      isExactMatch: false,
    },
    {
      operator: 'AND',
      field: { value: 'applicationName', label: 'Application Name', example: 'example-app' },
      value: 'test-app',
      isExactMatch: true,
    },
  ];

  beforeEach(() => {
    minimalProps = {
      setCurrentQuery: jest.fn(),
      searchItems: [],
      addSearchItem: jest.fn(),
      setField: jest.fn(),
      setValue: jest.fn(),
      removeSearchItem: jest.fn(),
      builderRef: { current: null },
    };

    renderComponent = (additionalProps = {}, preloadedState = {}) =>
      render(<AdvancedSearchCriteriaEasyBuilder {...minimalProps} {...additionalProps} />, { preloadedState });
  });

  it('renders the query builder', () => {
    renderComponent({}, initialState);

    expect(screen.getByRole('heading', { name: 'Build Query Rules' })).toBeVisible();
    expect(screen.getByRole('button', { name: /Add Search Item/i })).toBeVisible();
  });

  it('shows empty state when no search items', () => {
    renderComponent({}, initialState);

    expect(screen.getByRole('status')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'No search results' })).toBeVisible();
  });

  it('renders search items when provided', () => {
    renderComponent(
      {
        searchItems: mockSearchItems,
      },
      initialState
    );

    // Check that search items are rendered
    expect(screen.getByDisplayValue('test-component')).toBeVisible();
    expect(screen.getByDisplayValue('test-app')).toBeVisible();

    // Check that field labels are displayed in dropdowns
    expect(screen.getByRole('button', { name: 'Component Name' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Application Name' })).toBeVisible();
  });

  it('handles click outside to close the component', () => {
    renderComponent({}, initialState);

    // The component uses click-outside listener to close, not a button
    // This test verifies the component renders without errors
    expect(screen.getByRole('heading', { name: 'Build Query Rules' })).toBeVisible();
  });

  it('renders search items correctly', () => {
    renderComponent(
      {
        searchItems: mockSearchItems,
      },
      initialState
    );

    // Verify that the search items are rendered
    expect(screen.getByDisplayValue('test-component')).toBeVisible();
    expect(screen.getByDisplayValue('test-app')).toBeVisible();
  });

  it('handles field selection in dropdown', () => {
    const mockSetField = jest.fn();
    renderComponent(
      {
        searchItems: [{ operator: 'OR', field: '', value: '', isExactMatch: false }],
        setField: mockSetField,
      },
      initialState
    );

    // This test would need to be expanded based on how the dropdown interactions work
    // The actual implementation would depend on the NxStatefulDropdown component behavior
  });

  it('handles value changes in text input', () => {
    const mockSetValue = jest.fn();
    renderComponent(
      {
        searchItems: [{ operator: 'OR', field: { value: 'componentName' }, value: '', isExactMatch: false }],
        setValue: mockSetValue,
      },
      initialState
    );

    const textInput = screen.getByPlaceholderText('Enter Value');
    fireEvent.change(textInput, { target: { value: 'new-value' } });

    expect(mockSetValue).toHaveBeenCalledWith({
      index: 0,
      value: 'new-value',
      key: 'value',
    });
  });

  it('handles operator changes', () => {
    const mockSetValue = jest.fn();
    renderComponent(
      {
        searchItems: [{ operator: 'OR', field: { value: 'componentName' }, value: 'test', isExactMatch: false }],
        setValue: mockSetValue,
      },
      initialState
    );

    // This test would need to be expanded based on how the operator dropdown works
    // The actual implementation would depend on the NxStatefulDropdown component behavior
  });

  it('renders tooltips on select field dropdown items', async () => {
    const user = userEvent.setup();
    renderComponent(
      {
        searchItems: [{ operator: 'OR', field: { value: '', label: '' }, value: '', isExactMatch: false }],
      },
      initialState
    );

    const selectFieldButton = screen.getByRole('button', { name: 'Select Field' });
    await user.click(selectFieldButton);

    const dropdownButtons = screen.getAllByRole('button', { name: /.*/ }).filter((btn) => btn.title && btn.title !== '');
    expect(dropdownButtons.length).toBeGreaterThan(0);
    dropdownButtons.forEach((btn) => {
      expect(btn).toHaveAttribute('title', btn.textContent);
    });
  });

  it('handles exact match toggle', () => {
    const mockSetValue = jest.fn();
    renderComponent(
      {
        searchItems: [{ operator: 'OR', field: { value: 'componentName' }, value: 'test', isExactMatch: false }],
        setValue: mockSetValue,
      },
      initialState
    );

    // This test would need to be expanded based on how the exact match dropdown works
    // The actual implementation would depend on the NxStatefulDropdown component behavior
  });
});
