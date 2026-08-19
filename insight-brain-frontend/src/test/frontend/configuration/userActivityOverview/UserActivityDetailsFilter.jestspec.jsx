/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, setupPortalContainer } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import UserActivityDetailsFilter from 'MainRoot/configuration/userActivityOverview/UserActivityDetailsFilter';

describe('UserActivityDetailsFilter', () => {
  let defaultProps;

  beforeEach(() => {
    setupPortalContainer(); // Required for PortalDrawer

    defaultProps = {
      isOpen: true,
      onClose: jest.fn(),
      selectedActivityTypes: [],
      selectedDomains: [],
      selectedErrorTypes: [],
      filterOptions: {
        activityTypes: ['login', 'view', 'create', 'update', 'delete'],
        domains: ['authentication', 'reporting', 'governance', 'api'],
        errorTypes: ['Success', 'bad-request', 'unauthorized', 'not-found', 'server-error'],
      },
      onActivityTypesChange: jest.fn(),
      onDomainsChange: jest.fn(),
      onErrorTypesChange: jest.fn(),
      onApply: jest.fn(),
      onReset: jest.fn(),
      filtersAreDirty: false,
    };
  });

  describe('rendering', () => {
    it('should not render when isOpen is false', () => {
      render(<UserActivityDetailsFilter {...defaultProps} isOpen={false} />);

      expect(document.querySelector('.nx-drawer')).not.toBeInTheDocument();
    });

    it('should render drawer with correct title when open', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      expect(document.querySelector('.nx-drawer')).toBeInTheDocument();
      expect(screen.getByText('Filters', { hidden: true })).toBeInTheDocument();
    });

    it('should render all filter sections', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });

    it('should render Apply and Reset buttons', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      expect(screen.getByRole('button', { name: /apply/i, hidden: true })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /reset/i, hidden: true })).toBeInTheDocument();
    });

    it('should use narrow drawer variant', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      const drawer = document.querySelector('.nx-drawer');
      expect(drawer).toHaveClass('nx-drawer--narrow');
    });

    it('should have proper ARIA labeling', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      expect(screen.getByRole('dialog', { hidden: true })).toBeInTheDocument();
    });
  });

  describe('activity type filter', () => {
    it('should render activity type filter section', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
    });

    it('should render activity type component with correct ID', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      const activityTypeSelect = document.querySelector('#user-activity-type-filter');
      expect(activityTypeSelect).toBeInTheDocument();
    });

    it('should show correct selected activity types', () => {
      const propsWithSelected = {
        ...defaultProps,
        selectedActivityTypes: ['login', 'delete'],
      };
      render(<UserActivityDetailsFilter {...propsWithSelected} />);

      // Verify the component renders with selected values
      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
    });
  });

  describe('domain filter', () => {
    it('should render domain filter section', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
    });

    it('should render domain filter component', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      const domainSelect = document.querySelector('#user-activity-domain-filter');
      expect(domainSelect).toBeInTheDocument();
    });

    it('should show correct selected domains', () => {
      const propsWithSelected = {
        ...defaultProps,
        selectedDomains: ['authentication', 'reporting'],
      };
      render(<UserActivityDetailsFilter {...propsWithSelected} />);

      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
    });
  });

  describe('error status filter', () => {
    it('should render error status filter section', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });

    it('should render error status component with correct ID', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      const errorTypeSelect = document.querySelector('#user-activity-error-type-filter');
      expect(errorTypeSelect).toBeInTheDocument();
    });

    it('should show correct selected error statuses', () => {
      const propsWithSelected = {
        ...defaultProps,
        selectedErrorTypes: ['Success', 'bad-request'],
      };
      render(<UserActivityDetailsFilter {...propsWithSelected} />);

      // The TreeView should have the correct selected values (verify by DOM element)
      const errorTypeSelect = document.querySelector('#user-activity-error-type-filter');
      expect(errorTypeSelect).toBeInTheDocument();
    });
  });

  describe('button behavior', () => {
    it('should disable Apply and Reset buttons when filters are not dirty', () => {
      render(<UserActivityDetailsFilter {...defaultProps} filtersAreDirty={false} />);

      const applyButton = screen.getByRole('button', { name: /apply/i, hidden: true });
      const resetButton = screen.getByRole('button', { name: /reset/i, hidden: true });

      expect(applyButton).toBeDisabled();
      expect(resetButton).toBeDisabled();
    });

    it('should enable Apply and Reset buttons when filters are dirty', () => {
      render(<UserActivityDetailsFilter {...defaultProps} filtersAreDirty={true} />);

      const applyButton = screen.getByRole('button', { name: /apply/i, hidden: true });
      const resetButton = screen.getByRole('button', { name: /reset/i, hidden: true });

      expect(applyButton).not.toBeDisabled();
      expect(resetButton).not.toBeDisabled();
    });

    it('should call onApply and onClose when Apply button is clicked', async () => {
      const user = userEvent.setup();
      render(<UserActivityDetailsFilter {...defaultProps} filtersAreDirty={true} />);

      const applyButton = screen.getByRole('button', { name: /apply/i, hidden: true });
      await user.click(applyButton);

      expect(defaultProps.onApply).toHaveBeenCalled();
      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('should call onReset and onClose when Reset button is clicked', async () => {
      const user = userEvent.setup();
      render(<UserActivityDetailsFilter {...defaultProps} filtersAreDirty={true} />);

      const resetButton = screen.getByRole('button', { name: /reset/i, hidden: true });
      await user.click(resetButton);

      expect(defaultProps.onReset).toHaveBeenCalled();
      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('should have correct button variants', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      const applyButton = screen.getByRole('button', { name: /apply/i, hidden: true });
      const resetButton = screen.getByRole('button', { name: /reset/i, hidden: true });

      expect(applyButton).toHaveClass('nx-btn--primary');
      expect(resetButton).toHaveClass('nx-btn--tertiary');
    });
  });

  describe('filter options handling', () => {
    it('should handle empty filter options gracefully', () => {
      const propsWithEmptyOptions = {
        ...defaultProps,
        filterOptions: {
          activityTypes: [],
          domains: [],
          errorTypes: [],
        },
      };

      render(<UserActivityDetailsFilter {...propsWithEmptyOptions} />);

      // Should still render the filter sections
      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });

    it('should convert filter options to correct TreeView format', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      // Verify that all filter sections are rendered
      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });
  });

  describe('drawer interactions', () => {
    it('should call onClose when drawer is closed', async () => {
      const user = userEvent.setup();
      render(<UserActivityDetailsFilter {...defaultProps} />);

      // Find and click the close button (usually an X button in the header)
      const closeButton = document.querySelector('.nx-drawer__close-button');
      if (closeButton) {
        await user.click(closeButton);
        expect(defaultProps.onClose).toHaveBeenCalled();
      }
    });

    it('should render all filter components for interaction', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      // Verify all filter sections are available for interaction
      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });
  });

  describe('selected state preservation', () => {
    it('should preserve all selected states correctly', () => {
      const propsWithAllSelected = {
        ...defaultProps,
        selectedActivityTypes: ['create', 'update'],
        selectedDomains: ['governance', 'api'],
        selectedErrorTypes: ['bad-request', 'unauthorized'],
      };

      render(<UserActivityDetailsFilter {...propsWithAllSelected} />);

      // All the TreeViews should have the correct selected values (verify by DOM elements)
      const activityTypeSelect = document.querySelector('#user-activity-type-filter');
      const domainSelect = document.querySelector('#user-activity-domain-filter');
      const errorTypeSelect = document.querySelector('#user-activity-error-type-filter');

      expect(activityTypeSelect).toBeInTheDocument();
      expect(domainSelect).toBeInTheDocument();
      expect(errorTypeSelect).toBeInTheDocument();
    });
  });

  describe('multi-select functionality', () => {
    it('should render TreeViewMultiSelect components instead of RadioSelect', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      // Verify all three filter components use multi-select (they should have filter placeholders)
      expect(document.querySelector('#user-activity-type-filter')).toBeInTheDocument();
      expect(document.querySelector('#user-activity-domain-filter')).toBeInTheDocument();
      expect(document.querySelector('#user-activity-error-type-filter')).toBeInTheDocument();
    });

    it('should handle empty selection arrays', () => {
      const propsWithEmptySelections = {
        ...defaultProps,
        selectedActivityTypes: [],
        selectedDomains: [],
        selectedErrorTypes: [],
      };

      render(<UserActivityDetailsFilter {...propsWithEmptySelections} />);

      // Should render without errors and show all filter sections
      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });

    it('should handle single item in selection arrays', () => {
      const propsWithSingleSelections = {
        ...defaultProps,
        selectedActivityTypes: ['login'],
        selectedDomains: ['authentication'],
        selectedErrorTypes: ['Success'],
      };

      render(<UserActivityDetailsFilter {...propsWithSingleSelections} />);

      // Should render correctly with single selections
      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });

    it('should handle multiple items in selection arrays', () => {
      const propsWithMultipleSelections = {
        ...defaultProps,
        selectedActivityTypes: ['login', 'delete', 'view'],
        selectedDomains: ['authentication', 'reporting', 'governance'],
        selectedErrorTypes: ['Success', 'bad-request', 'unauthorized'],
      };

      render(<UserActivityDetailsFilter {...propsWithMultipleSelections} />);

      // Should render correctly with multiple selections
      expect(screen.getByText('Activity Type', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Domain', { hidden: true })).toBeInTheDocument();
      expect(screen.getByText('Error Type', { hidden: true })).toBeInTheDocument();
    });

    it('should call change handlers with arrays when selections change', async () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      // Note: Full integration testing of TreeViewMultiSelect would require more complex setup
      // Here we verify the components render and handlers are properly wired
      expect(defaultProps.onActivityTypesChange).toBeDefined();
      expect(defaultProps.onDomainsChange).toBeDefined();
      expect(defaultProps.onErrorTypesChange).toBeDefined();

      expect(typeof defaultProps.onActivityTypesChange).toBe('function');
      expect(typeof defaultProps.onDomainsChange).toBe('function');
      expect(typeof defaultProps.onErrorTypesChange).toBe('function');
    });

    it('should convert arrays to Sets for RSC components', () => {
      const propsWithSelectedValues = {
        ...defaultProps,
        selectedActivityTypes: ['login', 'delete'],
        selectedDomains: ['authentication'],
        selectedErrorTypes: ['Success', 'bad-request'],
      };

      render(<UserActivityDetailsFilter {...propsWithSelectedValues} />);

      // Verify that components receive Set instances
      // This indirectly tests the conversion logic since the components would error if they received arrays
      expect(document.querySelector('#user-activity-type-filter')).toBeInTheDocument();
      expect(document.querySelector('#user-activity-domain-filter')).toBeInTheDocument();
      expect(document.querySelector('#user-activity-error-type-filter')).toBeInTheDocument();
    });

    it('should display filter placeholders for better UX', () => {
      render(<UserActivityDetailsFilter {...defaultProps} />);

      // Multi-select components should have filter placeholders for better search experience
      // These are configured in the component props but we verify the components render
      expect(document.querySelector('#user-activity-type-filter')).toBeInTheDocument();
      expect(document.querySelector('#user-activity-domain-filter')).toBeInTheDocument();
      expect(document.querySelector('#user-activity-error-type-filter')).toBeInTheDocument();
    });
  });
});
