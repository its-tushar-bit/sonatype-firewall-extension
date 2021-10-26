/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../enzymeUtils';
import { NxFieldset, NxForm, NxRadio, NxTextInput, NxThreatIndicator } from '@sonatype/react-shared-components';
import EditLicensesForm from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesForm';

describe('EditLicensesForm', () => {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    saveFormSpy,
    deleteLicenseOverrideSpy,
    setLicenseStatusSpy,
    setLicenseScopeSpy,
    setLicenseCommentSpy,
    resetFormFieldsSpy,
    setSelectedLicensesSpy,
    onCloseSpy;

  beforeEach(() => {
    onCloseSpy = jasmine.createSpy('onClose');
    resetFormFieldsSpy = jasmine.createSpy('resetFormFields');
    setLicenseScopeSpy = jasmine.createSpy('setLicenseScope');
    setLicenseStatusSpy = jasmine.createSpy('setLicenseStatus');
    setLicenseCommentSpy = jasmine.createSpy('setLicenseComment');
    setSelectedLicensesSpy = jasmine.createSpy('setSelectedLicenses');
    saveFormSpy = jasmine.createSpy('saveForm');
    deleteLicenseOverrideSpy = jasmine.createSpy('deleteLicenseOverride');
    minimalProps = {
      onClose: onCloseSpy,
      resetFormFields: resetFormFieldsSpy,
      saveForm: saveFormSpy,
      deleteLicenseOverride: deleteLicenseOverrideSpy,
      setLicenseStatus: setLicenseStatusSpy,
      setLicenseScope: setLicenseScopeSpy,
      setLicenseComment: setLicenseCommentSpy,
      setSelectedLicenses: setSelectedLicensesSpy,
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
      selectableLicenses: [],
      submitMaskState: null,
      submitError: null,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(EditLicensesForm, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(EditLicensesForm, minimalProps);
  });

  it('renders a NxForm', () => {
    const component = getShallowComponent(),
      form = component.find(NxForm);

    expect(form).toExist();
  });

  describe('renders license info section', () => {
    const licensesProps = {
      declaredlicenses: [{ license: { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }, threatLevel: 10 }],
      observedlicenses: [{ license: { licenseId: 'No-Sources', licenseName: 'No Sources' }, threatLevel: 5 }],
      effectiveLicenses: [{ license: { licenseId: 'Apache-2.0', licenseName: 'Apache-2.0' }, threatLevel: 9 }],
    };

    it('renders license name', () => {
      const wrapper = getShallowComponent({ ...licensesProps }),
        ddList = wrapper.find('dd'),
        declaredlicenses = ddList.at(0),
        observedlicenses = ddList.at(1),
        effectiveLicenses = ddList.at(2);

      expect(ddList.length).toBe(3);
      expect(declaredlicenses.text()).toContain('Apache-2.0');
      expect(observedlicenses.text()).toContain('No Sources');
      expect(effectiveLicenses.text()).toContain('Apache-2.0');
    });

    it('renders license <NxThreatIndicator/>', () => {
      const wrapper = getShallowComponent({ ...licensesProps }),
        threatIndicators = wrapper.find(NxThreatIndicator);

      expect(threatIndicators.length).toBe(3);
      expect(threatIndicators.at(0)).toExist();
      expect(threatIndicators.at(0)).toHaveProp('policyThreatLevel', 10);
      expect(threatIndicators.at(1)).toExist();
      expect(threatIndicators.at(1)).toHaveProp('policyThreatLevel', 5);
      expect(threatIndicators.at(2)).toExist();
      expect(threatIndicators.at(2)).toHaveProp('policyThreatLevel', 9);
    });
  });

  it('calls onClose and resetFormFields when the cancel button is clicked', () => {
    const component = getShallowComponent(),
      form = component.find(NxForm);

    form.simulate('cancel');

    expect(onCloseSpy).toHaveBeenCalledTimes(1);
    expect(resetFormFieldsSpy).toHaveBeenCalledTimes(1);
  });

  it('renders a form group with a text area for the comments', () => {
    const component = getShallowComponent(),
      commentsSection = component.find('.iq-edit-licenses-form__comment'),
      textArea = commentsSection.find(NxTextInput);

    expect(commentsSection).toHaveClassName('.nx-form-group');
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

  it('renders license scopes', () => {
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
      targetRadios = licenseScopeSection.find(NxRadio);

    expect(licenseScopeSection).toHaveProp('label', 'Scope');
    expect(targetRadios.length).toBe(4);

    expect(targetRadios.at(0)).toHaveProp('name', 'license-scope-target');
    expect(targetRadios.at(0)).toHaveProp('value', 'owf');
    expect(targetRadios.at(0)).toHaveProp('isChecked', true);
    expect(targetRadios.at(0)).toHaveText('Application - OWF (Acknowledged)');

    expect(targetRadios.at(1)).toHaveProp('name', 'license-scope-target');
    expect(targetRadios.at(1)).toHaveProp('value', 'asdf');
    expect(targetRadios.at(1)).toHaveProp('isChecked', false);
    expect(targetRadios.at(1)).toHaveText('Organization - asdf');

    expect(targetRadios.at(2)).toHaveProp('name', 'license-scope-target');
    expect(targetRadios.at(2)).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    expect(targetRadios.at(2)).toHaveProp('isChecked', false);
    expect(targetRadios.at(2)).toHaveText('Organization - Root Organization (Acknowledged)');

    expect(targetRadios.at(3)).toHaveProp('name', 'license-scope-target');
    expect(targetRadios.at(3)).toHaveProp('value', 'sdc');
    expect(targetRadios.at(3)).toHaveProp('isChecked', false);
    expect(targetRadios.at(3)).toHaveText('Application - SDC (Selected)');
  });

  it('calls setLicenseScope and setLicenseStatus when changing to scope without a licenseOverride', () => {
    const component = getShallowComponent(),
      waiverTargetsSection = component.find('.iq-edit-licenses-form__scope'),
      targetRadios = waiverTargetsSection.find(NxRadio),
      radio1 = targetRadios.at(0),
      radio2 = targetRadios.at(1);

    expect(radio1).toHaveProp('isChecked', true);
    expect(radio2).toHaveProp('isChecked', false);

    radio2.simulate('change', 'asdf');

    expect(setLicenseScopeSpy).toHaveBeenCalledWith({
      ownerId: 'asdf',
      ownerName: 'asdf',
      ownerType: 'organization',
      licenseOverride: null,
    });
    expect(setLicenseStatusSpy).toHaveBeenCalledWith('OPEN');
  });

  it('calls setLicenseScope, setLicenseStatus, setSelectedLicenses when changing to scope with a licenseOverride', () => {
    const component = getShallowComponent(),
      waiverTargetsSection = component.find('.iq-edit-licenses-form__scope'),
      targetRadios = waiverTargetsSection.find(NxRadio),
      radio1 = targetRadios.at(0),
      radio3 = targetRadios.at(2);

    expect(radio1).toHaveProp('isChecked', true);
    expect(radio3).toHaveProp('isChecked', false);

    radio3.simulate('change', 'ROOT_ORGANIZATION_ID');

    expect(setLicenseScopeSpy).toHaveBeenCalledWith({
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
      licenseOverride: {
        id: '82823b22b17d4925a358763058b82184',
        ownerId: 'ROOT_ORGANIZATION_ID',
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
    });
    expect(setLicenseStatusSpy).toHaveBeenCalledWith('ACKNOWLEDGED');
    expect(setSelectedLicensesSpy).toHaveBeenCalledWith([]);
  });

  it('renders default status options with inherited option', () => {
    const component = getShallowComponent(),
      statusSection = component.find('.iq-edit-licenses-form__status'),
      selectComponent = statusSection.find('select'),
      options = selectComponent.find('option');

    expect(statusSection.find(NxFieldset)).toExist();
    expect(statusSection).toHaveProp('label', 'Status');
    expect(selectComponent).toExist();
    expect(options.length).toBe(4);

    expect(options.at(0)).toHaveText('Open');
    expect(options.at(0)).toHaveProp('value', 'OPEN');

    expect(options.at(1)).toHaveText('Acknowledged');
    expect(options.at(1)).toHaveProp('value', 'ACKNOWLEDGED');

    expect(options.at(2)).toHaveText('Confirmed');
    expect(options.at(2)).toHaveProp('value', 'CONFIRMED');

    expect(options.at(3)).toHaveText('Inherit Status (Acknowledged)');
    expect(options.at(3)).toHaveProp('value', 'DELETE');
  });

  it('does not render inherited status for root hierarchy', () => {
    const component = getShallowComponent({
        scope: { ownerId: 'ROOT_ORGANIZATION_ID', ownerName: 'Root Organization', ownerType: 'organization' },
      }),
      statusSection = component.find('.iq-edit-licenses-form__status'),
      selectComponent = statusSection.find('select'),
      options = selectComponent.find('option');

    expect(options.length).toBe(3);
    expect(options.at(2).text()).not.toContain('Inherit Status');
  });

  it('calls setLicenseStatus and setSelectedLicenses when changing status when scope has licenseOverride', () => {
    const component = getShallowComponent({
        scope: { licenseOverride: { licenseIds: ['id'] } },
      }),
      selectComponent = component.find('select'),
      mockEvent = { currentTarget: { value: 'ACKNOWLEDGED' } };

    selectComponent.simulate('change', mockEvent);

    expect(setLicenseStatusSpy).toHaveBeenCalledWith('ACKNOWLEDGED');
    expect(setSelectedLicensesSpy).toHaveBeenCalledWith(['id']);
  });

  it('calls setLicenseStatus and setSelectedLicenses(with default value) when changing status and scope has no licenseOverride', () => {
    const component = getShallowComponent({
        scope: {},
      }),
      selectComponent = component.find('select'),
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
      selectComponent = statusSection.find('select'),
      options = selectComponent.find('option');

    expect(options.at(1)).toHaveText('Acknowledged');
    expect(options.at(1)).toHaveProp('value', 'ACKNOWLEDGED');
  });

  it('renders checkbox for each selectableLicense', () => {
    const component = getMountedComponent({
        status: 'SELECTED',
        selectableLicenses: [
          { licenseId: 'id', licenseName: 'value' },
          { licenseId: 'id2', licenseName: 'value2' },
        ],
      }),
      checkBoxContainer = component.find('.iq-edit-licenses-form__selected-licenses'),
      checkboxes = checkBoxContainer.find('input');

    expect(checkboxes.length).toBe(2);
  });

  it('calls setSelectedLicenses with new id when selected license does not exist in licenseIds', () => {
    const component = getMountedComponent({
        status: 'SELECTED',
        selectableLicenses: [{ licenseId: 'id', licenseName: 'value' }],
      }),
      checkBoxContainer = component.find('.iq-edit-licenses-form__selected-licenses'),
      checkboxes = checkBoxContainer.find('input');

    checkboxes.at(0).simulate('change', { target: { checked: true } });

    expect(setSelectedLicensesSpy).toHaveBeenCalledWith(['id']);
  });

  it('calls setSelectedLicenses to remove id when selected license exist in licenseIds', () => {
    const component = getMountedComponent({
        status: 'SELECTED',
        selectableLicenses: [{ licenseId: 'id', licenseName: 'value' }],
        licenseIds: ['id'],
      }),
      checkBoxContainer = component.find('.iq-edit-licenses-form__selected-licenses'),
      checkboxes = checkBoxContainer.find('input');

    checkboxes.at(0).simulate('change', { target: { checked: true } });

    expect(setSelectedLicensesSpy).toHaveBeenCalledWith([]);
  });

  it('calls saveForm ', () => {
    const component = getShallowComponent(),
      form = component.find(NxForm);

    form.simulate('submit');

    expect(saveFormSpy).toHaveBeenCalledTimes(1);
  });

  it('calls deleteLicenseOverride when the status is "DELETE"', () => {
    const component = getShallowComponent({ status: 'DELETE' }),
      form = component.find(NxForm);

    form.simulate('submit');

    expect(deleteLicenseOverrideSpy).toHaveBeenCalledTimes(1);
  });
});
