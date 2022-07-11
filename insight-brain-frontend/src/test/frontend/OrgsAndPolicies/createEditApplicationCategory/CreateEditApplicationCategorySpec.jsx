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

describe('CreateEditApplicationCategory create', () => {
  let renderComponent,
    selectIsLoadingSpy,
    selectLoadErrorSpy,
    saveApplicationCategorySpy,
    setCategoryNameSpy,
    setCategoryDescriptionSpy;

  beforeEach(() => {
    selectIsLoadingSpy = spyOn(categorySelectors, 'selectIsLoading').and.returnValue(false);
    selectLoadErrorSpy = spyOn(categorySelectors, 'selectLoadError').and.returnValue(null);

    saveApplicationCategorySpy = spyOn(actions, 'saveApplicationCategory').and.callThrough();
    setCategoryNameSpy = spyOn(actions, 'setCategoryName').and.callThrough();
    setCategoryDescriptionSpy = spyOn(actions, 'setCategoryDescription').and.callThrough();

    renderComponent = () => render(<CreateEditApplicationCategory />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('New Application Category')).toBeVisible();
  });

  it('renders loading indicator', () => {
    selectIsLoadingSpy.and.returnValue(true);
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('doesn"t allow to create category after initialization', () => {
    renderComponent();

    const createButton = screen.getByRole('button', { name: 'Submit disabled: There are no changes to save' });

    expect(createButton).toBeVisible();
    expect(createButton).toHaveClassName('disabled');

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
      name: 'Submit disabled: Unable to save: fields with invalid or missing data',
    });
    expect(createButton).toBeVisible();
    expect(createButton).toHaveClassName('disabled');

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
    expect(createButton).not.toHaveClassName('disabled');

    fireEvent.click(createButton);
    expect(saveApplicationCategorySpy).toHaveBeenCalled();
  });

  it('shows error message on error', () => {
    selectLoadErrorSpy.and.returnValue('Error');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });
});

describe('CreateEditApplicationCategory edit', () => {
  let renderComponent, saveApplicationCategorySpy, setCategoryNameSpy, removeCategorySpy;

  beforeEach(() => {
    spyOn(categorySelectors, 'selectLoadError').and.returnValue(null);
    spyOn(categorySelectors, 'selectIsLoading').and.returnValue(false);
    spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
      categoryId: '353653653653',
    });

    spyOn(actions, 'loadCategoryEditor').and.returnValue({
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

    saveApplicationCategorySpy = spyOn(actions, 'saveApplicationCategory').and.callThrough();
    removeCategorySpy = spyOn(actions, 'removeApplicationCategory').and.callThrough();
    setCategoryNameSpy = spyOn(actions, 'setCategoryName').and.callThrough();

    renderComponent = () => render(<CreateEditApplicationCategory />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();

    expect(screen.getByText('Edit Application Category')).toBeVisible();
  });

  it('doesn"t allow to update category without any changes', () => {
    renderComponent();
    const updateButton = screen.getByRole('button', { name: 'Submit disabled: There are no changes to save' });
    const deleteButton = screen.getByRole('button', { name: 'Delete' });

    expect(updateButton).toBeVisible();
    expect(deleteButton).toBeVisible();
    expect(updateButton).toHaveClassName('disabled');

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
    expect(updateButton).not.toHaveClassName('disabled');

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
    expect(updateButton).not.toHaveClassName('disabled');
    fireEvent.change(categoryNameInput, { target: { value: 'initialCategory' } });

    const updateButtonReinitialize = screen.getByRole('button', {
      name: 'Submit disabled: There are no changes to save',
    });
    expect(updateButtonReinitialize).toHaveClassName('disabled');
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
