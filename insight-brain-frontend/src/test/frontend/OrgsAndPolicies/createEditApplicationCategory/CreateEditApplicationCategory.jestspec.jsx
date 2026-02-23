/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import CreateEditApplicationCategory from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/CreateEditApplicationCategory';
import * as categorySelectors from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('CreateEditApplicationCategory create', () => {
  let renderComponent,
    selectIsLoadingSpy,
    selectLoadErrorSpy,
    saveApplicationCategorySpy,
    setCategoryNameSpy,
    setCategoryDescriptionSpy;

  beforeEach(() => {
    selectIsLoadingSpy = jest.spyOn(categorySelectors, 'selectIsLoading').mockReturnValue(false);
    selectLoadErrorSpy = jest.spyOn(categorySelectors, 'selectLoadError').mockReturnValue(null);

    saveApplicationCategorySpy = jest.spyOn(actions, 'saveApplicationCategory');
    setCategoryNameSpy = jest.spyOn(actions, 'setCategoryName');
    setCategoryDescriptionSpy = jest.spyOn(actions, 'setCategoryDescription');

    renderComponent = () => render(<CreateEditApplicationCategory />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('New Application Category')).toBeVisible();
  });

  it('renders loading indicator', () => {
    selectIsLoadingSpy.mockReturnValue(true);
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('doesn"t allow to create category after initialization', () => {
    renderComponent();

    const createButton = screen.getByRole('button', { name: 'Create' });

    expect(createButton).toBeVisible();

    fireEvent.click(createButton);
    expect(saveApplicationCategorySpy).not.toHaveBeenCalled();
  });

  it('doesn"t allow to create a category with only category Name field filled', () => {
    renderComponent();

    const categoryNameInputValue = 'categoryValue';

    const categoryNameInput = screen.getByRole('textbox', { name: 'Category Name' });
    fireEvent.change(categoryNameInput, { target: { value: categoryNameInputValue } });

    expect(setCategoryNameSpy).toHaveBeenCalled();
    expect(setCategoryNameSpy).toHaveBeenCalledWith(categoryNameInputValue);
    expect(screen.getByDisplayValue(categoryNameInputValue)).toBeVisible();

    const createButton = screen.getByRole('button', {
      name: 'Create',
    });
    expect(createButton).toBeVisible();

    fireEvent.click(createButton);
    expect(saveApplicationCategorySpy).not.toHaveBeenCalled();
  });

  it('creates a category with filled category Name and Description fields', () => {
    renderComponent();

    const categoryNameInputValue = 'categoryNameValue';
    const categoryDescriptionInputValue = 'categoryDescriptionValue';

    const categoryNameInput = screen.getByRole('textbox', { name: 'Category Name' });
    const categoryDescriptionField = screen.getByRole('textbox', { name: 'Brief Description' });

    fireEvent.change(categoryNameInput, { target: { value: categoryNameInputValue } });
    expect(setCategoryNameSpy).toHaveBeenCalled();
    expect(setCategoryNameSpy).toHaveBeenCalledWith(categoryNameInputValue);
    expect(screen.getByDisplayValue(categoryNameInputValue)).toBeVisible();

    fireEvent.change(categoryDescriptionField, { target: { value: categoryDescriptionInputValue } });
    expect(setCategoryDescriptionSpy).toHaveBeenCalled();
    expect(setCategoryDescriptionSpy).toHaveBeenCalledWith(categoryDescriptionInputValue);
    expect(screen.getByDisplayValue(categoryDescriptionInputValue)).toBeVisible();

    const createButton = screen.getByRole('button', { name: 'Create' });
    expect(createButton).toBeVisible();
    expect(createButton).not.toHaveClass('disabled');

    fireEvent.click(createButton);
    expect(saveApplicationCategorySpy).toHaveBeenCalled();
  });

  it('shows error message on error', () => {
    selectLoadErrorSpy.mockReturnValue('Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });
});

describe('CreateEditApplicationCategory edit', () => {
  let renderComponent, saveApplicationCategorySpy, setCategoryNameSpy, removeCategorySpy;

  beforeEach(() => {
    jest.spyOn(categorySelectors, 'selectLoadError').mockReturnValue(null);
    jest.spyOn(categorySelectors, 'selectIsLoading').mockReturnValue(false);
    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
      categoryId: '353653653653',
    });

    jest.spyOn(actions, 'loadCategoryEditor').mockReturnValue({
      type: 'applicationCategories/createEdit/loadCategoryEditor/fulfilled',
      payload: {
        currentCategory: {
          color: 'light-red',
          description: 'initialDesc',
          name: 'initialCategory',
        },
        siblings: [],
      },
    });

    saveApplicationCategorySpy = jest.spyOn(actions, 'saveApplicationCategory');
    removeCategorySpy = jest.spyOn(actions, 'removeApplicationCategory');
    setCategoryNameSpy = jest.spyOn(actions, 'setCategoryName');

    renderComponent = () => render(<CreateEditApplicationCategory />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Edit Application Category')).toBeVisible();
  });

  it('doesn"t allow to update category without any changes', () => {
    renderComponent();
    const updateButton = screen.getByRole('button', { name: 'Update' });
    const deleteButton = screen.getByRole('button', { name: 'Delete' });

    expect(updateButton).toBeVisible();
    expect(deleteButton).toBeVisible();

    fireEvent.click(updateButton);
    expect(saveApplicationCategorySpy).not.toHaveBeenCalled();
  });

  it('updates category if name was changed', () => {
    renderComponent();

    const categoryNameInputValue = 'categoryValue';

    const categoryNameInput = screen.getByRole('textbox', { name: 'Category Name' });
    fireEvent.change(categoryNameInput, { target: { value: categoryNameInputValue } });

    expect(setCategoryNameSpy).toHaveBeenCalled();
    expect(setCategoryNameSpy).toHaveBeenCalledWith(categoryNameInputValue);
    expect(screen.getByDisplayValue(categoryNameInputValue)).toBeVisible();

    const updateButton = screen.getByRole('button', { name: 'Update' });
    expect(updateButton).toBeVisible();
    expect(updateButton).not.toHaveClass('disabled');

    fireEvent.click(updateButton);
    expect(saveApplicationCategorySpy).toHaveBeenCalled();
  });

  it('if same category values, than button is disabled', () => {
    renderComponent();

    const categoryNameInputValue = 'categoryValue';

    const categoryNameInput = screen.getByRole('textbox', { name: 'Category Name' });

    fireEvent.change(categoryNameInput, { target: { value: categoryNameInputValue } });

    expect(setCategoryNameSpy).toHaveBeenCalled();
    expect(screen.getByDisplayValue(categoryNameInputValue)).toBeVisible();

    const updateButton = screen.getByRole('button', { name: 'Update' });
    expect(updateButton).not.toHaveClass('disabled');
    fireEvent.change(categoryNameInput, { target: { value: 'initialCategory' } });

    const updateButtonReinitialize = screen.getByRole('button', {
      name: 'Update',
    });
    fireEvent.click(updateButtonReinitialize);
    expect(saveApplicationCategorySpy).not.toHaveBeenCalled();
  });

  it('delete category working', () => {
    renderComponent();

    const deleteButton = screen.getByRole('button', { name: 'Delete' });
    expect(deleteButton).toBeVisible();
    fireEvent.click(deleteButton);

    expect(screen.getByText('Delete Application Category')).toBeVisible();

    const modalDeleteButton = screen.getByRole('button', { name: 'Continue' });
    fireEvent.click(modalDeleteButton);

    expect(removeCategorySpy).toHaveBeenCalled();
  });
});
