/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import SAMLConfigurationDeleteModal from 'MainRoot/configuration/saml/SAMLConfigurationDeleteModal';

describe('SAMLConfigurationDeleteModal', () => {
  let renderComponent, props, deleteConfigurationSpy, toggleDeleteModalSpy;

  deleteConfigurationSpy = jest.fn();
  toggleDeleteModalSpy = jest.fn();

  props = {
    deleteConfiguration: deleteConfigurationSpy,
    toggleDeleteModal: toggleDeleteModalSpy,
  };

  beforeEach(() => {
    renderComponent = (props) => render(<SAMLConfigurationDeleteModal {...props} />);
  });

  it('renders component', () => {
    renderComponent({ ...props, isDeleteModalShown: true });

    const modalCancelButton = screen.getByRole('button', { name: 'Cancel' });
    const modalDeleteButton = screen.getByRole('button', { name: 'Delete' });

    expect(
      screen.queryByText(
        'Clicking "delete" will permanently remove this SAML configuration from the system. Are you sure you want to delete it?'
      )
    ).toBeInTheDocument();

    expect(modalCancelButton).toBeVisible();
    fireEvent.click(modalCancelButton);
    expect(toggleDeleteModalSpy).toHaveBeenCalled();

    expect(modalDeleteButton).toBeVisible();
    fireEvent.click(modalDeleteButton);
    expect(deleteConfigurationSpy).toHaveBeenCalled();
  });
});
