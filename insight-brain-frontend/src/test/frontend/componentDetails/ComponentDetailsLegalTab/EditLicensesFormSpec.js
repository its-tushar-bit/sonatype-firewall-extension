/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import {
  NxStatefulForm,
  NxLoadingSpinner,
  NxTextInput,
  NxThreatIndicator,
  NxFormSelect,
  NxFormGroup,
} from '@sonatype/react-shared-components';
import EditLicensesForm from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesForm';
import * as OverriddenField from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/OverriddenField';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';

describe('EditLicensesForm', () => {
  let minimalProps,
    mountedComponent,
    getShallowComponent,
    getMountedComponent,
    saveFormSpy,
    deleteLicenseOverrideSpy,
    setLicenseStatusSpy,
    setLicenseScopeSpy,
    setLicenseCommentSpy,
    resetFormFieldsSpy,
    setSelectedLicensesSpy,
    onCloseSpy,
    setShowUnsavedChangesModalSpy;

  beforeEach(() => {
    onCloseSpy = jasmine.createSpy('onClose');
    resetFormFieldsSpy = jasmine.createSpy('resetFormFields');
    setLicenseScopeSpy = jasmine.createSpy('setLicenseScope');
    setLicenseStatusSpy = jasmine.createSpy('setLicenseStatus');
    setLicenseCommentSpy = jasmine.createSpy('setLicenseComment');
    setSelectedLicensesSpy = jasmine.createSpy('setSelectedLicenses');
    saveFormSpy = jasmine.createSpy('saveForm');
    deleteLicenseOverrideSpy = jasmine.createSpy('deleteLicenseOverride');
    setShowUnsavedChangesModalSpy = jasmine.createSpy('setShowUnsavedChangesModal');
    minimalProps = {
      onClose: onCloseSpy,
      resetFormFields: resetFormFieldsSpy,
      saveForm: saveFormSpy,
      deleteLicenseOverride: deleteLicenseOverrideSpy,
      setLicenseStatus: setLicenseStatusSpy,
      setLicenseScope: setLicenseScopeSpy,
      setLicenseComment: setLicenseCommentSpy,
      setSelectedLicenses: setSelectedLicensesSpy,
      setShowUnsavedChangesModal: setShowUnsavedChangesModalSpy,
      identificationSource: 'Sonatype',
      status: 'ACKNOWLEDGED',
      comment: {
        isPristine: true,
        value: 'license comment',
        trimmedValue: '',
        validationErrors: null,
      },
      licenseIds: [],
      isDirty: false,
      scope: {
        ownerId: 'owf',
        ownerName: 'OWF',
        ownerType: 'application',
        licenseOverride: {
          id: 'b6c0724bb1824f3a9b0e77232e555218',
          ownerId: '77e76d350f9d4203b81468dd696209f3',
          status: 'ACKNOWLEDGED',
          comment: '',
          licenseIds: [],
          componentIdentifier: {
            format: 'a-name',
            coordinates: {
              name: 'bson',
              qualifier: '',
              version: '0.0.4',
            },
          },
        },
      },
      availableLicenseScopes: [
        {
          ownerId: 'owf',
          ownerName: 'OWF',
          ownerType: 'application',
          licenseOverride: {
            id: 'b6c0724bb1824f3a9b0e77232e555218',
            ownerId: '77e76d350f9d4203b81468dd696209f3',
            status: 'ACKNOWLEDGED',
            comment: '',
            licenseIds: [],
            componentIdentifier: {
              format: 'a-name',
              coordinates: {
                name: 'bson',
                qualifier: '',
                version: '0.0.4',
              },
            },
          },
        },
        {
          ownerId: 'asdf',
          ownerName: 'asdf',
          ownerType: 'organization',
          licenseOverride: null,
        },
        {
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          licenseOverride: {
            id: '82823b22b17d4925a358763058b82184',
            ownerId: 'ROOT_ORGANIZATION_ID',
            status: 'ACKNOWLEDGED',
            comment: 'some comment',
            licenseIds: [],
            componentIdentifier: {
              format: 'a-name',
              coordinates: {
                name: 'bson',
                qualifier: '',
                version: '0.0.4',
              },
            },
          },
        },
      ],
      selectableLicenses: [],
      submitMaskState: null,
      submitError: null,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(EditLicensesForm, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(EditLicensesForm, minimalProps);
  });

  afterEach(() => {
    if (mountedComponent?.exists()) {
      mountedComponent.unmount();
    }
  });

  it('renders a NxStatefulForm', () => {
    const component = getShallowComponent(),
      form = component.find(NxStatefulForm);

    expect(form).toExist();
  });

  describe('renders license info section', () => {
    const licensesProps = {
      declaredLicenses: [
        {
          licenses: [
            {
              license: {
                licenseId: 'Apache-2.0',
                licenseName: 'Apache-2.0',
              },
              threatLevel: 10,
            },
          ],
        },
      ],
      observedLicenses: [
        {
          licenses: [
            {
              license: {
                licenseId: 'No Sources',
                licenseName: 'No Sources',
              },
              threatLevel: 5,
            },
          ],
        },
      ],
      effectiveLicenses: [
        {
          licenses: [
            {
              license: {
                licenseId: 'Apache-2.0',
                licenseName: 'Apache-2.0',
              },
              threatLevel: 10,
            },
          ],
        },
      ],
    };

    it('renders license name', () => {
      const wrapper = getShallowComponent({ ...licensesProps }),
        ddList = wrapper.find('dd'),
        effectiveLicenses = ddList.at(0),
        declaredLicenses = ddList.at(1),
        observedLicenses = ddList.at(2);

      expect(ddList.length).toBe(3);
      expect(declaredLicenses.text()).toContain('Apache-2.0');
      expect(observedLicenses.text()).toContain('No Sources');
      expect(effectiveLicenses.text()).toContain('Apache-2.0');
    });

    it('renders Not Provided as license name if component was claimed', () => {
      const unspecifiedLicense = [
        {
          licenses: [
            {
              license: {
                licenseId: 'UNSPECIFIED',
                licenseName: 'Not Provided',
              },
              threatLevel: 5,
            },
          ],
        },
      ];
      const component = getShallowComponent({
        effectiveLicenses: unspecifiedLicense,
        declaredLicenses: unspecifiedLicense,
        observedLicenses: unspecifiedLicense,
        identificationSource: 'Manual',
      });
      const ddList = component.find('dd'),
        effectiveLicenses = ddList.at(0),
        declaredLicenses = ddList.at(1),
        observedLicenses = ddList.at(2);

      expect(ddList.length).toBe(3);
      expect(declaredLicenses.text()).toContain('Not Provided (Claimed Component)');
      expect(observedLicenses.text()).toContain('Not Provided (Claimed Component)');
      expect(effectiveLicenses.text()).toContain('Not Provided');
    });

    it('renders license <NxThreatIndicator/>', () => {
      const wrapper = getShallowComponent({ ...licensesProps }),
        threatIndicators = wrapper.find(NxThreatIndicator);
      expect(threatIndicators.length).toBe(3);
      expect(threatIndicators.at(0)).toExist();
      expect(threatIndicators.at(0)).toHaveProp('policyThreatLevel', 10);
      expect(threatIndicators.at(1)).toExist();
      expect(threatIndicators.at(1)).toHaveProp('policyThreatLevel', 10);
      expect(threatIndicators.at(2)).toExist();
      expect(threatIndicators.at(2)).toHaveProp('policyThreatLevel', 5);
    });
  });

  it('calls onClose and resetFormFields when the cancel button is clicked', () => {
    const component = getShallowComponent(),
      form = component.find(NxStatefulForm);

    form.simulate('cancel');

    expect(onCloseSpy).toHaveBeenCalledTimes(1);
    expect(resetFormFieldsSpy).toHaveBeenCalledTimes(1);
  });

  it('calls setShowUnsavedChangesModal when the form is dirty and the cancel button is clicked', () => {
    const customMinimalProps = {
      ...minimalProps,
      isDirty: true,
    };

    const component = enzymeUtils.getShallowComponent(EditLicensesForm, customMinimalProps)(),
      form = component.find(NxStatefulForm);

    form.simulate('cancel');

    expect(onCloseSpy).toHaveBeenCalledTimes(0);
    expect(resetFormFieldsSpy).toHaveBeenCalledTimes(0);
    expect(setShowUnsavedChangesModalSpy).toHaveBeenCalledWith(true);
  });

  it('renders a form group with a text area for the comments', () => {
    const component = getShallowComponent(),
      commentsSection = component.find('.iq-edit-licenses-form__comment'),
      textArea = commentsSection.find(NxTextInput);

    expect(commentsSection).toMatchSelector(NxFormGroup);
    expect(textArea).toHaveProp('type', 'textarea');
    expect(textArea).toHaveProp('value', 'license comment');
    expect(textArea).toHaveProp('isPristine', true);
  });

  it('calls setLicenseComment when updating comment', () => {
    const component = getShallowComponent(),
      commentsSection = component.find('.iq-edit-licenses-form__comment'),
      textArea = commentsSection.find(NxTextInput);

    textArea.simulate('change', 'Foo');

    expect(setLicenseCommentSpy).toHaveBeenCalledWith('Foo');
  });

  it('renders license scopes dropdown', () => {
    const component = getShallowComponent({
        availableLicenseScopes: [
          ...minimalProps.availableLicenseScopes,
          {
            ownerId: 'sdc',
            ownerName: 'SDC',
            ownerType: 'application',
            licenseOverride: {
              id: 'b6c0724bb1812f3a9b0e77232e555214',
              ownerId: '77e76d350f3d4203b81468dd696209f3',
              status: 'SELECTED',
              comment: '',
              licenseIds: [],
              componentIdentifier: {
                format: 'a-name',
                coordinates: {
                  name: 'bson',
                  qualifier: '',
                  version: '0.0.4',
                },
              },
            },
          },
        ],
      }),
      licenseScopeSection = component.find('.iq-edit-licenses-form__scope'),
      scopeSelect = licenseScopeSection.find(IqScopeDropdown);

    expect(licenseScopeSection).toHaveProp('label', 'Scope');
    expect(scopeSelect).toExist();
  });

  it('renders default status options with inherited option', () => {
    const component = getShallowComponent(),
      statusSection = component.find('.iq-edit-licenses-form__status'),
      selectComponent = statusSection.find(NxFormSelect).at(0),
      options = selectComponent.find('option');

    expect(statusSection.find(NxFormGroup)).toExist();
    expect(statusSection).toHaveProp('label', 'Status');
    expect(selectComponent).toExist();
    expect(options.length).toBe(5);

    expect(options.at(0)).toHaveText('Open');
    expect(options.at(0)).toHaveProp('value', 'OPEN');

    expect(options.at(1)).toHaveText('Acknowledged');
    expect(options.at(1)).toHaveProp('value', 'ACKNOWLEDGED');

    expect(options.at(2)).toHaveText('Overridden');
    expect(options.at(2)).toHaveProp('value', 'OVERRIDDEN');

    expect(options.at(3)).toHaveText('Confirmed');
    expect(options.at(3)).toHaveProp('value', 'CONFIRMED');

    expect(options.at(4)).toHaveText('Inherit Status (Acknowledged)');
    expect(options.at(4)).toHaveProp('value', 'DELETE');
  });

  it('does not render inherited status for root hierarchy', () => {
    const component = getShallowComponent({
        scope: { ownerId: 'ROOT_ORGANIZATION_ID', ownerName: 'Root Organization', ownerType: 'organization' },
      }),
      statusSection = component.find('.iq-edit-licenses-form__status'),
      selectComponent = statusSection.find(NxFormSelect).at(0),
      options = selectComponent.find('option');

    expect(options.length).toBe(4);
    expect(options.at(3).text()).not.toContain('Inherit Status');
  });

  it('calls setLicenseStatus and setSelectedLicenses when changing status', () => {
    const component = getShallowComponent({
        scope: {},
      }),
      selectComponent = component.find(NxFormSelect).at(0),
      mockEvent = { currentTarget: { value: 'ACKNOWLEDGED' } };

    selectComponent.simulate('change', mockEvent);

    expect(setLicenseStatusSpy).toHaveBeenCalledWith('ACKNOWLEDGED');
    expect(setSelectedLicensesSpy).toHaveBeenCalledWith([]);
  });

  it('does not render checkboxes for selectedLicenses when status is not SELECTED', () => {
    const component = getShallowComponent({
        status: 'Confirmed',
        selectableLicenses: [
          { licenseId: 'id', licenseName: 'value' },
          { licenseId: 'id2', licenseName: 'value2' },
        ],
      }),
      checkBoxContainer = component.find('.iq-edit-licenses-form__selected-licenses'),
      checkboxes = checkBoxContainer.find('input');

    expect(checkboxes.length).toBe(0);
  });

  it('renders SELECTED status option', () => {
    const component = getShallowComponent({
        status: 'SELECTED',
      }),
      statusSection = component.find('.iq-edit-licenses-form__status'),
      selectComponent = statusSection.find(NxFormSelect).at(0),
      options = selectComponent.find('option');

    expect(options.at(1)).toHaveText('Acknowledged');
    expect(options.at(1)).toHaveProp('value', 'ACKNOWLEDGED');
  });

  it('renders checkbox for each selectableLicense', () => {
    mountedComponent = getMountedComponent({
      status: 'SELECTED',
      selectableLicenses: [
        { licenseId: 'id', licenseName: 'value' },
        { licenseId: 'id2', licenseName: 'value2' },
      ],
    });
    const checkBoxContainer = mountedComponent.find('.iq-edit-licenses-form__selected-licenses'),
      checkboxes = checkBoxContainer.find('input');

    expect(checkboxes.length).toBe(2);
  });

  it('calls setSelectedLicenses with new id when selected license does not exist in licenseIds', () => {
    mountedComponent = getMountedComponent({
      status: 'SELECTED',
      selectableLicenses: [{ licenseId: 'id', licenseName: 'value' }],
    });
    const checkBoxContainer = mountedComponent.find('.iq-edit-licenses-form__selected-licenses'),
      checkboxes = checkBoxContainer.find('input');

    checkboxes.at(0).simulate('change', { target: { checked: true } });

    expect(setSelectedLicensesSpy).toHaveBeenCalledWith(['id']);
  });

  it('calls setSelectedLicenses to remove id when selected license exist in licenseIds', () => {
    mountedComponent = getMountedComponent({
      status: 'SELECTED',
      selectableLicenses: [{ licenseId: 'id', licenseName: 'value' }],
      licenseIds: ['id'],
    });
    const checkBoxContainer = mountedComponent.find('.iq-edit-licenses-form__selected-licenses'),
      checkboxes = checkBoxContainer.find('input');

    checkboxes.at(0).simulate('change', { target: { checked: true } });

    expect(setSelectedLicensesSpy).toHaveBeenCalledWith([]);
  });

  describe('Overridden field loading spinner', () => {
    beforeEach(() => jasmine.clock().install());
    afterEach(() => jasmine.clock().uninstall());

    it('renders NxLoadingSpinner', () => {
      mountedComponent = getMountedComponent({
        status: 'OVERRIDDEN',
      });
      const spinner = mountedComponent.find(NxLoadingSpinner);

      expect(spinner).toExist();
    });

    it('hides NxLoadingSpinner', () => {
      spyOn(OverriddenField, 'default').and.returnValue('overridden field');
      mountedComponent = getMountedComponent({
        status: 'OVERRIDDEN',
      });
      let spinner = mountedComponent.find(NxLoadingSpinner);
      expect(spinner).toExist();

      jasmine.clock().tick(0);
      mountedComponent.update();

      spinner = mountedComponent.find(NxLoadingSpinner);
      expect(spinner).not.toExist();
    });
  });

  it('calls saveForm ', () => {
    const component = getShallowComponent(),
      form = component.find(NxStatefulForm);

    form.simulate('submit');

    expect(saveFormSpy).toHaveBeenCalledTimes(1);
  });

  it('calls deleteLicenseOverride when the status is "DELETE"', () => {
    const component = getShallowComponent({ status: 'DELETE' }),
      form = component.find(NxStatefulForm);

    form.simulate('submit');

    expect(deleteLicenseOverrideSpy).toHaveBeenCalledTimes(1);
  });
});
