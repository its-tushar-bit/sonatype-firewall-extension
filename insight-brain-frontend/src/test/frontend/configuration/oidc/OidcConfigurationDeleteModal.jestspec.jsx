/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import OidcConfigurationDeleteModal from 'MainRoot/configuration/oidc/OidcConfigurationDeleteModal';
import userEvent from '@testing-library/user-event';

describe('OidcConfigurationDeleteModal', () => {
  const mockDeleteConfiguration = jest.fn();
  const mockToggleDeleteModal = jest.fn();

  const defaultProps = {
    deleteConfiguration: mockDeleteConfiguration,
    toggleDeleteModal: mockToggleDeleteModal,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = (props = {}) => {
    return render(<OidcConfigurationDeleteModal {...defaultProps} {...props} />);
  };

  describe('Rendering', () => {
    it('should render the modal with correct title', () => {
      renderComponent();

      expect(screen.getByText('Delete OIDC Configuration?')).toBeInTheDocument();
    });

    it('should render the modal with id', () => {
      const { container } = renderComponent();

      expect(container.querySelector('#oidc-config-delete-modal')).toBeInTheDocument();
    });

    it('should render warning alert with message', () => {
      renderComponent();

      expect(screen.getByText(/This will remove the configured OIDC authentication/i)).toBeInTheDocument();
      expect(
        screen.getByText(/Users will need to authenticate using alternative methods after deletion/i)
      ).toBeInTheDocument();
    });

    it('should render OK button', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: /OK/i })).toBeInTheDocument();
    });

    it('should render Cancel button', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
    });

    it('should render modal with narrow variant', () => {
      const { container } = renderComponent();

      const modal = container.querySelector('.nx-modal');
      expect(modal).toHaveClass('nx-modal--narrow');
    });
  });

  describe('User Interactions', () => {
    it('should call deleteConfiguration when OK button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const okButton = screen.getByRole('button', { name: /OK/i });
      await user.click(okButton);

      expect(mockDeleteConfiguration).toHaveBeenCalledTimes(1);
    });

    it('should call toggleDeleteModal when Cancel button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      expect(mockToggleDeleteModal).toHaveBeenCalledTimes(1);
    });

    it('should not call deleteConfiguration when Cancel button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      expect(mockDeleteConfiguration).not.toHaveBeenCalled();
    });

    it('should not call toggleDeleteModal when OK button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const okButton = screen.getByRole('button', { name: /OK/i });
      await user.click(okButton);

      expect(mockToggleDeleteModal).not.toHaveBeenCalled();
    });
  });

  describe('Form Submission', () => {
    it('should handle form submission through OK button', async () => {
      const user = userEvent.setup();
      renderComponent();

      const okButton = screen.getByRole('button', { name: /OK/i });
      await user.click(okButton);

      expect(mockDeleteConfiguration).toHaveBeenCalledTimes(1);
    });

    it('should handle form cancellation through Cancel button', async () => {
      const user = userEvent.setup();
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      expect(mockToggleDeleteModal).toHaveBeenCalledTimes(1);
      expect(mockDeleteConfiguration).not.toHaveBeenCalled();
    });
  });

  describe('Warning Alert', () => {
    it('should display warning alert component', () => {
      const { container } = renderComponent();

      const alert = container.querySelector('.nx-alert--warning');
      expect(alert).toBeInTheDocument();
    });

    it('should contain warning text about authentication removal', () => {
      renderComponent();

      const warningText = screen.getByText(/This will remove the configured OIDC authentication/i);
      expect(warningText).toBeInTheDocument();
    });

    it('should contain warning text about alternative authentication', () => {
      renderComponent();

      const warningText = screen.getByText(/Users will need to authenticate using alternative methods/i);
      expect(warningText).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have heading with proper id for aria-labelledby', () => {
      renderComponent();

      const heading = screen.getByRole('heading', { name: /Delete OIDC Configuration/i });
      expect(heading).toHaveAttribute('id', 'oidc-delete-label-modal');
    });
  });

  describe('Prop Validation', () => {
    it('should accept deleteConfiguration function prop', () => {
      const customDelete = jest.fn();
      renderComponent({ deleteConfiguration: customDelete });

      expect(screen.getByRole('button', { name: /OK/i })).toBeInTheDocument();
    });

    it('should accept toggleDeleteModal function prop', () => {
      const customToggle = jest.fn();
      renderComponent({ toggleDeleteModal: customToggle });

      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
    });
  });
});
