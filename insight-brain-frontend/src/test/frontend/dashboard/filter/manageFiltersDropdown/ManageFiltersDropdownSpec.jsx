/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { NxDropdown } from '@sonatype/react-shared-components';

import DocumentClickListenerWrapper from '../../../../../main/frontend/react/DocumentClickListenerWrapper';
import ManageFiltersDropdown
  from '../../../../../main/frontend/dashboard/filter/manageFiltersDropdown/ManageFiltersDropdown';

describe('ManageFiltersDropdown', function() {
  let props, getShallowComponent, applyDefaultFilter, applySavedFilter, toggleFiltersDropdown, selectFilterToDelete,
      handleDocumentClick;

  beforeEach(function() {
    applyDefaultFilter = jasmine.createSpy('applyDefaultFilter');
    applySavedFilter = jasmine.createSpy('applySavedFilter');
    toggleFiltersDropdown = jasmine.createSpy('toggleFiltersDropdown');
    selectFilterToDelete = jasmine.createSpy('selectFilterToDelete');
    handleDocumentClick = jasmine.createSpy('handleDocumentClick');

    props = {
      showDirtyAsterisk: false,
      appliedFilterName: 'filter 1234',
      filtersDropdownOpen: true,
      savedFilters: [
        {
          name: 'foo'
        },
        {
          name: 'bar'
        }
      ],
      applyDefaultFilter,
      applySavedFilter,
      toggleFiltersDropdown,
      selectFilterToDelete,
      handleDocumentClick
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ManageFiltersDropdown, props);
  });

  it('renders NxDropdown component with the variant secondary', function() {
    const component = getShallowComponent();
    expect(component).toMatchSelector(NxDropdown);
    expect(component).toHaveProp('variant', 'secondary');
  });

  it('renders open dropdown when filtersDropdownOpen is true', function() {
    const component = getShallowComponent();
    expect(component).toHaveProp('isOpen', true);
  });

  it('renders closed dropdown when filtersDropdownOpen is false', function() {
    const component = getShallowComponent({
      filtersDropdownOpen: false
    });
    expect(component).toHaveProp('isOpen', false);
  });

  describe('dropdown toggle', function() {
    it('renders appliedFilterName as dropdown toggle label', function() {
      const component = getShallowComponent(),
          labelProp = component.prop('label');

      const asteriskVDom = labelProp.props.children[0];
      const labelVDom = labelProp.props.children[1];

      expect(asteriskVDom).toBe(false);
      expect(labelVDom.props).toEqual({
        className: 'iq-manage-filters-dropdown__label',
        children: 'filter 1234'
      });
    });

    it('renders appliedFilterName with asterisk as dropdown toggle label if showDirtyAsterisk is true', function() {
      const component = getShallowComponent({ showDirtyAsterisk: true }),
          labelProp = component.prop('label');

      const asteriskVDom = labelProp.props.children[0];
      const labelVDom = labelProp.props.children[1];

      expect(asteriskVDom.props).toEqual({
        className: 'iq-manage-filters-dropdown__dirty-asterisk',
        children: '*'
      });
      expect(labelVDom.props).toEqual({
        className: 'iq-manage-filters-dropdown__label',
        children: 'filter 1234'
      });
    });

    it('renders "Default" as dropdown toggle label if appliedFilterName is null', function() {
      const component = getShallowComponent({ appliedFilterName: null }),
          labelProp = component.prop('label');

      const asteriskVDom = labelProp.props.children[0];
      const labelVDom = labelProp.props.children[1];

      expect(asteriskVDom).toBe(false);
      expect(labelVDom.props).toEqual({
        className: 'iq-manage-filters-dropdown__label',
        children: 'Default'
      });
    });

    it('renders "Default" with asterisk as dropdown toggle label if showDirtyAsterisk is true', function() {
      const component = getShallowComponent({ appliedFilterName: null, showDirtyAsterisk: true }),
          labelProp = component.prop('label');

      const asteriskVDom = labelProp.props.children[0];
      const labelVDom = labelProp.props.children[1];

      expect(asteriskVDom.props).toEqual({
        className: 'iq-manage-filters-dropdown__dirty-asterisk',
        children: '*'
      });
      expect(labelVDom.props).toEqual({
        className: 'iq-manage-filters-dropdown__label',
        children: 'Default'
      });
    });
  });

  describe('dropdown menu', function() {
    it('wraps options with DocumentClickListenerWrapper', function() {
      const component = getShallowComponent(),
          defaultOption = component.find(DocumentClickListenerWrapper).childAt(0);

      expect(defaultOption).toHaveText('Default');
    });

    it('renders default option and empty list message if no savedFilters provided', function() {
      const component = getShallowComponent({ savedFilters: [] }),
          options = component.find(DocumentClickListenerWrapper).children(),
          defaultOption = options.at(0),
          emptyListMessage = options.at(1);

      expect(options.length).toBe(2);
      expect(defaultOption).toHaveText('Default');
      expect(defaultOption.find('.nx-btn--delete-filter')).not.toExist();
      expect(emptyListMessage).toHaveText('No saved filters');
    });

    it('renders savedFilters options with delete buttons', function() {
      const component = getShallowComponent({ appliedFilterName: null }),
          options = component.find(DocumentClickListenerWrapper).children();

      expect(options.length).toBe(3);
      expect(options.at(0)).toHaveText('Default');
      expect(options.at(0).find('.nx-btn--delete-filter')).not.toExist();

      expect(options.at(1).find('.nx-dropdown-button')).toHaveText('foo');
      expect(options.at(1).find('.nx-btn--delete-filter')).toExist();

      expect(options.at(2).find('.nx-dropdown-button')).toHaveText('bar');
      expect(options.at(2).find('.nx-btn--delete-filter')).toExist();
    });

    it('renders default option with selected class if appliedFilterName is null', function() {
      const component = getShallowComponent({ appliedFilterName: null }),
          options = component.find('.iq-manage-filters-dropdown__option');

      expect(options.at(0)).toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(1)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(2)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
    });

    it('renders saved filter option with selected class if corresponding filter is applied', function() {
      const component = getShallowComponent({ appliedFilterName: 'bar' }),
          options = component.find('.iq-manage-filters-dropdown__option');

      expect(options.at(0)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(1)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(2)).toHaveClassName('iq-manage-filters-dropdown__option--selected');
    });

    describe('option click handler', function() {
      it('calls applySavedFilter callback with filter object if saved filter option is clicked', function() {
        const component = getShallowComponent(),
            options = component.find('.iq-manage-filters-dropdown__option');

        options.at(1).find('.nx-dropdown-button--select-filter').simulate('click');
        expect(applySavedFilter).toHaveBeenCalledWith({ name: 'foo' });

        options.at(2).find('.nx-dropdown-button--select-filter').simulate('click');
        expect(applySavedFilter).toHaveBeenCalledWith({ name: 'bar' });
      });

      it('calls applyDefaultFilter callback if default option is clicked', function() {
        const component = getShallowComponent(),
            defaultOption = component.find('.iq-manage-filters-dropdown__option').at(0);

        defaultOption.find('.nx-dropdown-button--select-filter').simulate('click');
        expect(applyDefaultFilter).toHaveBeenCalled();
      });
    });

    describe('delete filter button click handler', function() {
      it('fires selectFilterToDelete action with filter name', function() {
        const savedFilterOption = getShallowComponent().find('.iq-manage-filters-dropdown__option').at(1);

        savedFilterOption.find('.nx-btn--delete-filter').simulate('click');
        expect(selectFilterToDelete).toHaveBeenCalledWith('foo');
      });
    });
  });

  describe('onKeyDown handler', function() {

    describe('when the Escape key is pressed', function() {
      it('fires the action to close the dropdown if open', function() {
        const component = getShallowComponent();

        component.simulate('keyDown', {key: 'Escape'});
        expect(toggleFiltersDropdown).toHaveBeenCalledWith(false);
      });

      it('doesn\'t fire the action to close the dropdown if already closed', function() {
        const component = getShallowComponent({
          filtersDropdownOpen: false
        });

        component.simulate('keyDown', {key: 'Escape'});
        expect(toggleFiltersDropdown).not.toHaveBeenCalled();
      });
    });

    describe('when the Esc key is pressed (IE11)', function() {
      it('fires the action to close the dropdown if open', function() {
        const component = getShallowComponent();

        component.simulate('keyDown', {key: 'Esc'});
        expect(toggleFiltersDropdown).toHaveBeenCalledWith(false);
      });

      it('doesn\'t fire the action to close the dropdown if already closed', function() {
        const component = getShallowComponent({
          filtersDropdownOpen: false
        });

        component.simulate('keyDown', {key: 'Esc'});
        expect(toggleFiltersDropdown).not.toHaveBeenCalled();
      });
    });

    describe('when other key is pressed', function() {
      it('doesn\'t fire the action to close the dropdown if open', function() {
        const element = getShallowComponent();

        element.simulate('keyDown', {key: '3'});
        expect(toggleFiltersDropdown).not.toHaveBeenCalled();
      });
    });
  });

  describe('onToggleCollapse handler', function() {
    it('fires the action to close the dropdown if open', function() {
      const component = getShallowComponent();

      component.simulate('toggleCollapse');
      expect(toggleFiltersDropdown).toHaveBeenCalledWith(false);
    });

    it('fires the action to open the dropdown if closed', function() {
      const component = getShallowComponent({
        filtersDropdownOpen: false
      });

      component.simulate('toggleCollapse');
      expect(toggleFiltersDropdown).toHaveBeenCalledWith(true);
    });
  });

  describe('onDocumentClick handler', function() {
    it('fires handleDocumentClick action', function() {
      const component = getShallowComponent();
      component.find(DocumentClickListenerWrapper).simulate('documentClick');
      expect(handleDocumentClick).toHaveBeenCalled();
    });
  });
});
