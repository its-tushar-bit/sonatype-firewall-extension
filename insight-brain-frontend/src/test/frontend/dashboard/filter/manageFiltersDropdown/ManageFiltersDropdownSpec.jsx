/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { shallow } from 'enzyme';

import * as enzymeUtils from '../../../enzymeUtils';
import { NxDropdown } from '@sonatype/react-shared-components';
import ManageFiltersDropdown from '../../../../../main/frontend/dashboard/filter/manageFiltersDropdown/ManageFiltersDropdown';

describe('ManageFiltersDropdown', function () {
  let props,
    getShallowComponent,
    applyDefaultFilter,
    applySavedFilter,
    toggleFiltersDropdown,
    selectFilterToDelete,
    handleDocumentClick,
    DeleteFilterModalContainerMock;

  beforeEach(function () {
    applyDefaultFilter = jasmine.createSpy('applyDefaultFilter');
    applySavedFilter = jasmine.createSpy('applySavedFilter');
    toggleFiltersDropdown = jasmine.createSpy('toggleFiltersDropdown');
    selectFilterToDelete = jasmine.createSpy('selectFilterToDelete');
    handleDocumentClick = jasmine.createSpy('handleDocumentClick');

    DeleteFilterModalContainerMock = jasmine
      .createSpy('DeleteFilterModalContainer')
      .and.returnValue(<div>Delete Filter Modal</div>);

    props = {
      showDirtyAsterisk: false,
      appliedFilterName: 'filter 1234',
      filtersDropdownOpen: true,
      savedFilters: [
        {
          name: 'foo',
        },
        {
          name: 'bar',
        },
      ],
      applyDefaultFilter,
      applySavedFilter,
      toggleFiltersDropdown,
      selectFilterToDelete,
      handleDocumentClick,
      DeleteFilterModal: DeleteFilterModalContainerMock,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ManageFiltersDropdown, props);
  });

  it('renders provided DeleteFilterModal', function () {
    const component = getShallowComponent();
    expect(component.childAt(0)).toMatchSelector(DeleteFilterModalContainerMock);
  });

  it('renders NxDropdown component', function () {
    const component = getShallowComponent();
    expect(component.childAt(1)).toMatchSelector(NxDropdown);
  });

  it('renders closed dropdown by default', function () {
    const component = getShallowComponent();
    expect(component.find(NxDropdown)).toHaveProp('isOpen', false);
  });

  describe('dropdown toggle', function () {
    it('renders appliedFilterName as dropdown toggle label', function () {
      const component = getShallowComponent().find(NxDropdown),
        labelProp = component.prop('label');

      const asteriskVDom = labelProp.props.children[0];
      const labelVDom = shallow(labelProp.props.children[1]);

      expect(asteriskVDom).toBe(false);
      expect(labelVDom).toHaveText('filter 1234');
    });

    it('renders appliedFilterName with asterisk as dropdown toggle label if showDirtyAsterisk is true', function () {
      const component = getShallowComponent({ showDirtyAsterisk: true }).find(NxDropdown),
        labelProp = component.prop('label');

      const asteriskVDom = shallow(labelProp.props.children[0]);
      const labelVDom = shallow(labelProp.props.children[1]);

      expect(asteriskVDom).toHaveClassName('iq-manage-filters-dropdown__dirty-asterisk');
      expect(asteriskVDom).toHaveText('*');

      expect(labelVDom).toHaveText('filter 1234');
    });

    it('renders "Default" as dropdown toggle label if appliedFilterName is null', function () {
      const component = getShallowComponent({ appliedFilterName: null }).find(NxDropdown),
        labelProp = component.prop('label');

      const asteriskVDom = labelProp.props.children[0];
      const labelVDom = shallow(labelProp.props.children[1]);

      expect(asteriskVDom).toBe(false);
      expect(labelVDom).toHaveText('Default');
    });

    it('renders "Default" with asterisk as dropdown toggle label if showDirtyAsterisk is true', function () {
      const component = getShallowComponent({
          appliedFilterName: null,
          showDirtyAsterisk: true,
        }).find(NxDropdown),
        labelProp = component.prop('label');

      const asteriskVDom = shallow(labelProp.props.children[0]);
      const labelVDom = shallow(labelProp.props.children[1]);

      expect(asteriskVDom).toHaveClassName('iq-manage-filters-dropdown__dirty-asterisk');
      expect(asteriskVDom).toHaveText('*');

      expect(labelVDom).toHaveText('Default');
    });
  });

  describe('dropdown menu', function () {
    it('renders default option and empty list message if no savedFilters provided', function () {
      const component = getShallowComponent({ savedFilters: [] }),
        options = component.find(NxDropdown).children(),
        defaultOption = options.at(0),
        emptyListMessage = options.at(1);

      expect(options.length).toBe(2);
      expect(defaultOption).toHaveText('Default');
      expect(defaultOption.find('.nx-btn--delete-filter')).not.toExist();
      expect(emptyListMessage).toHaveText('No saved filters');
    });

    it('renders savedFilters options with delete buttons', function () {
      const component = getShallowComponent({ appliedFilterName: null }),
        options = component.find(NxDropdown).children();

      expect(options.length).toBe(3);
      expect(options.at(0)).toHaveText('Default');
      expect(options.at(0).find('.nx-btn--delete-filter')).not.toExist();

      expect(options.at(1).find('.nx-dropdown-button')).toHaveText('foo');
      expect(options.at(1).find('.nx-btn--delete-filter')).toExist();
      expect(options.at(1).find('.nx-btn--delete-filter')).toHaveProp('title', 'Delete');

      expect(options.at(2).find('.nx-dropdown-button')).toHaveText('bar');
      expect(options.at(2).find('.nx-btn--delete-filter')).toExist();
      expect(options.at(2).find('.nx-btn--delete-filter')).toHaveProp('title', 'Delete');
    });

    it('renders default option with selected class if appliedFilterName is null', function () {
      const component = getShallowComponent({ appliedFilterName: null }),
        options = component.find('.iq-manage-filters-dropdown__option');

      expect(options.at(0)).toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(1)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(2)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
    });

    it('renders saved filter option with selected class if corresponding filter is applied', function () {
      const component = getShallowComponent({ appliedFilterName: 'bar' }),
        options = component.find('.iq-manage-filters-dropdown__option');

      expect(options.at(0)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(1)).not.toHaveClassName('iq-manage-filters-dropdown__option--selected');
      expect(options.at(2)).toHaveClassName('iq-manage-filters-dropdown__option--selected');
    });

    describe('option click handler', function () {
      it('calls applySavedFilter callback with filter object if saved filter option is clicked', function () {
        const component = getShallowComponent(),
          options = component.find('.iq-manage-filters-dropdown__option');

        options.at(1).find('.nx-dropdown-button--select-filter').simulate('click');
        expect(applySavedFilter).toHaveBeenCalledWith({ name: 'foo' });

        options.at(2).find('.nx-dropdown-button--select-filter').simulate('click');
        expect(applySavedFilter).toHaveBeenCalledWith({ name: 'bar' });
      });

      it('calls applyDefaultFilter callback if default option is clicked', function () {
        const component = getShallowComponent(),
          defaultOption = component.find('.iq-manage-filters-dropdown__option').at(0);

        defaultOption.find('.nx-dropdown-button--select-filter').simulate('click');
        expect(applyDefaultFilter).toHaveBeenCalled();
      });
    });

    describe('delete filter button click handler', function () {
      it('fires selectFilterToDelete action with filter name', function () {
        const savedFilterOption = getShallowComponent().find('.iq-manage-filters-dropdown__option').at(1);

        savedFilterOption.find('.nx-btn--delete-filter').simulate('click');
        expect(selectFilterToDelete).toHaveBeenCalledWith('foo');
      });
    });
  });

  describe('onToggleCollapse handler', function () {
    it('opens and closes the dropdown', function () {
      const component = getShallowComponent();

      expect(component.find(NxDropdown)).toHaveProp('isOpen', false);
      component.find(NxDropdown).prop('onToggleCollapse')();
      component.update();
      expect(component.find(NxDropdown)).toHaveProp('isOpen', true);

      component.find(NxDropdown).prop('onToggleCollapse')();
      component.update();
      expect(component.find(NxDropdown)).toHaveProp('isOpen', false);
    });
  });
});
