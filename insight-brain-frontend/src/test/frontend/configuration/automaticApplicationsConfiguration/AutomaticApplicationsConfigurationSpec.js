/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxErrorAlert, NxStatefulForm } from '@sonatype/react-shared-components';
import AutomaticApplicationsConfiguration from '../../../../main/frontend/configuration/automaticApplicationsConfiguration/AutomaticApplicationsConfiguration';

import * as enzymeUtils from '../../enzymeUtils';

describe('AutomaticApplicationConfiguration', function () {
  let initialProps, initialFunctions, getShallow, getMounted, mock$state;

  beforeEach(() => {
    initialProps = {
      formState: {
        enabled: false,
        parentOrganizationId: '',
      },
      loading: false,
      loadError: null,
      updateError: null,
      submitMaskState: null,
      isDirty: false,
      organizations: [],
      $state: mock$state,
    };

    mock$state = jasmine.createSpyObj('$state', ['get', 'href']);

    getShallow = enzymeUtils.getShallowComponent(AutomaticApplicationsConfiguration, initialProps);
  });

  describe('component load', function () {
    it('passes error when a load error exist', function () {
      const errorMsg = 'Error on page load';
      const autoApplicationConfig = getShallow({ loadError: errorMsg, $state: mock$state }).find(NxStatefulForm);
      expect(autoApplicationConfig).toHaveProp('loadError', errorMsg);
    });

    it('renders form when page load successfully', function () {
      const autoApplicationConfig = getShallow();
      expect(autoApplicationConfig.find(NxStatefulForm)).toExist();
    });
  });

  describe('toggle', function () {
    it('calls toggleAutomaticApplicationEnabled action when clicked', function () {
      const toggleAutomaticApplicationEnabledSpy = jasmine.createSpy('toggleAutomaticApplicationEnabled');
      const autoApplicationConfig = getShallow({
        toggleAutomaticApplicationEnabled: toggleAutomaticApplicationEnabledSpy,
      });
      const toggle = autoApplicationConfig.find('.nx-toggle--no-gap');
      toggle.simulate('change');
      expect(toggleAutomaticApplicationEnabledSpy).toHaveBeenCalled();
    });
  });
  describe('dropdown', function () {
    let organizations;

    beforeAll(() => {
      organizations = [
        { id: '1', name: 'organization1' },
        { id: '2', name: 'organization2' },
        { id: '3', name: 'organization3' },
      ];
    });

    it('renders alert message when does not have any organization', function () {
      const autoApplicationConfig = getShallow();
      const alert = autoApplicationConfig.find(NxErrorAlert);
      expect(alert).toExist();
      expect(alert).toHaveText('No parent organizations found');
    });

    it('renders select element when have at least one organization', function () {
      const autoApplicationConfig = getShallow({ organizations });
      expect(autoApplicationConfig.find('#parent-organization-selector')).toExist();
    });

    it('renders select element with empty value when no option is selected', function () {
      const autoApplicationConfig = getShallow({ organizations });
      const select = autoApplicationConfig.find('#parent-organization-selector');
      expect(select).toHaveProp('value', '');
    });

    it('renders select element with correct organization value', function () {
      const autoApplicationConfig = getShallow({
        organizations,
        formState: {
          enabled: false,
          parentOrganizationId: organizations[2].id,
        },
      });
      const select = autoApplicationConfig.find('#parent-organization-selector');
      expect(select).toHaveProp('value', organizations[2].id);
    });

    it('renders all organization options', function () {
      const autoApplicationConfig = getShallow({ organizations });
      const options = autoApplicationConfig.find('#parent-organization-selector > option');
      expect(options.length).toBe(4);
      expect(options.at(1).text()).toBe(organizations[0].name);
      expect(options.at(2).text()).toBe(organizations[1].name);
      expect(options.at(3).text()).toBe(organizations[2].name);
    });

    it('calls setParentOrganization when option changes', function () {
      const organizationId = '2';
      const setParentOrganization = jasmine.createSpy('setParentOrganization');
      const autoApplicationConfig = getShallow({ organizations, setParentOrganization });
      const dropdown = autoApplicationConfig.find('#parent-organization-selector');
      dropdown.simulate('change', { target: { value: organizationId } });
      expect(setParentOrganization).toHaveBeenCalledWith(organizationId);
    });

    it('disables select element when toggleAutomaticApplicationEnabled is disabled', function () {
      const autoApplicationConfig = getShallow({
        organizations,
        formState: {
          enabled: false,
          parentOrganizationId: organizations[2].id,
        },
      });
      const select = autoApplicationConfig.find('#parent-organization-selector');
      expect(select).toHaveProp('disabled', true);
    });
  });

  describe('cancel button', function () {
    let cancelButtonId, cancelButtonSelector, mountedComponent;

    beforeAll(() => {
      cancelButtonId = '#auto-app-config-cancel';
      cancelButtonSelector = `${cancelButtonId} button`;
    });

    beforeEach(() => {
      initialFunctions = {
        load: jasmine.createSpy('load'),
        update: jasmine.createSpy('update'),
      };
      getMounted = enzymeUtils.getMountedComponent(AutomaticApplicationsConfiguration, {
        ...initialProps,
        ...initialFunctions,
      });
    });

    afterEach(() => {
      mountedComponent.unmount();
    });

    it('is disabled when does not have any changes in form', function () {
      mountedComponent = getMounted();
      expect(mountedComponent.find(cancelButtonSelector)).toHaveProp('disabled', true);
    });

    it('is enabled when have changes in form', function () {
      mountedComponent = getMounted({ isDirty: true });
      expect(mountedComponent.find(cancelButtonSelector)).toHaveProp('disabled', false);
    });

    it('calls resetForm when clicked', function () {
      const resetFormSpy = jasmine.createSpy('resetForm');
      mountedComponent = getMounted({ resetForm: resetFormSpy, isDirty: true });
      mountedComponent.find(cancelButtonSelector).simulate('click');
      expect(resetFormSpy).toHaveBeenCalled();
    });
  });

  describe('NxStatefulForm', function () {
    it('is rendered with validationErrors if does not have any change', function () {
      const autoApplicationConfig = getShallow();
      const form = autoApplicationConfig.find(NxStatefulForm);

      expect(form).toHaveProp('validationErrors', 'There are no changes to update.');
    });

    it('is rendered with validationErrors if parent organization is not set', function () {
      const autoApplicationConfig = getShallow({ isDirty: true });
      const form = autoApplicationConfig.find(NxStatefulForm);

      expect(form).toHaveProp('validationErrors', 'Unable to update: fields with invalid or missing data.');
    });
  });

  describe('on form submit', function () {
    it('calls update when the form is submitted', function () {
      const update = jasmine.createSpy('update');
      const autoApplicationConfig = getShallow({ update });
      const form = autoApplicationConfig.find(NxStatefulForm);
      form.simulate('submit');

      expect(update).toHaveBeenCalled();
    });
  });
});
