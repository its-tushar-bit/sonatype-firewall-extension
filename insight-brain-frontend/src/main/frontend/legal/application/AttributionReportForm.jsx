/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxForm,
  NxFieldset,
  NxFormGroup,
  NxTextInput,
  NxCheckbox,
  nxTextInputStateHelpers,
  useToggle,
  NxButton,
  NxDropdown,
} from '@sonatype/react-shared-components';
import { getAttributionReportUrl } from '../../util/CLMLocation';
import AttributionAdditionalFiles from './AttributionAdditionalFiles';
import ConfirmationModal from './ConfirmationModal';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';

export default function AttributionReportForm(props) {
  const {
    applicationPublicId,
    stageTypeId,
    stateGo,
    $state,
    attributionReports,
    attributionReportTemplates,
    getAttributionReportTemplates,
    applyAttributionReportTemplateByIndex,
    setDirtyFlagToAttributionReport,
  } = props;
  // no way to set name on RSC checkboxes, so we need to create named hidden inputs to send values
  const { initialState, userInput } = nxTextInputStateHelpers;
  const NEW_TEMPLATE_INDEX = -1;
  const INCLUDE_STANDARD_LICENSE_TEXTS_PROP_NAME = 'includeStandardLicenseTexts';
  const [templateIndexToSelect, setTemplateIndexToSelect] = React.useState(null);
  const formReference = React.useRef();
  const [isDropDownOpen, setDropDownCollapsed] = useToggle(false);
  let isTemplatePristine = React.useRef(true);

  const validator = (inputName) => (val) => {
    const labelName = inputName === 'documentTitle' ? 'Report Title' : inputName;
    if (val.length === 0) {
      return `${labelName} cannot be empty`;
    }
    return null;
  };

  const defaultFormState = {
    documentTitle: initialState('Attribution Report for ' + applicationPublicId, validator('documentTitle')),
    header: initialState(''),
    footer: initialState(''),
    includeTableOfContents: true,
    includeStandardLicenseTexts: true,
    includeAppendix: true,
  };
  const [formState, setFormState] = React.useState(defaultFormState);

  React.useEffect(() => {
    getAttributionReportTemplates();
    applyAttributionReportTemplateByIndex(NEW_TEMPLATE_INDEX);
  }, []);

  React.useEffect(() => {
    if (attributionReports.selectedTemplateIndex >= 0) {
      isTemplatePristine.current = true;
      setInitialFormData();
    } else {
      isTemplatePristine.current = false;
      setFormState(defaultFormState);
    }
  }, [attributionReports.selectedTemplateIndex]);

  function setInitialFormData() {
    const {
      documentTitle,
      header,
      footer,
      includeTableOfContents,
      includeStandardLicenseTexts,
      includeAppendix,
    } = attributionReportTemplates.results[attributionReports.selectedTemplateIndex];
    setFormState({
      documentTitle: initialState(documentTitle.value || documentTitle, validator('documentTitle')),
      header: initialState(header.value || header),
      footer: initialState(footer.value || footer),
      includeTableOfContents,
      includeStandardLicenseTexts,
      includeAppendix,
    });
  }

  const getStateTemplate = () =>
    attributionReports.selectedTemplateIndex >= 0
      ? attributionReportTemplates.results[attributionReports.selectedTemplateIndex]
      : null;

  const textInputChangeHandler = (inputName) => (val) => {
    const stateTemplate = getStateTemplate();

    if (attributionReports.selectedTemplateIndex >= 0) {
      isTemplatePristine.current = stateTemplate[inputName] === val;
    } else {
      isTemplatePristine.current = defaultFormState[inputName].value === val;
    }

    if (isTemplatePristine.current) {
      isTemplatePristine.current = getTemplatePristinity(inputName);
    } else {
      setDirtyFlagToAttributionReport(true);
    }
    textInputSetter(inputName)(userInput(validator(inputName), val));
  };

  const getCurrentTemplateData = () => ({
    documentTitle: formState.documentTitle.value,
    header: formState.header.value,
    footer: formState.footer.value,
    includeTableOfContents: formState.includeTableOfContents,
    includeStandardLicenseTexts: formState.includeStandardLicenseTexts,
    includeAppendix: formState.includeAppendix,
  });

  const getTemplatePristinity = (inputToOmit) => {
    let stateTemplate = getStateTemplate() || defaultFormState;
    let currentTemplate = getCurrentTemplateData();
    const isPristine =
      !!stateTemplate &&
      !Object.keys(currentTemplate).find((templateProperty) => {
        if (
          templateProperty === 'templateName' ||
          templateProperty === 'lastUpdatedAt' ||
          templateProperty === 'id' ||
          inputToOmit === templateProperty
        )
          return false;
        return (
          (typeof stateTemplate[templateProperty] === 'object'
            ? stateTemplate[templateProperty].value
            : stateTemplate[templateProperty]) !== currentTemplate[templateProperty]
        );
      });
    setDirtyFlagToAttributionReport(!isPristine);
    return isPristine;
  };

  const toggle = (inputName) => () => {
    const newCheckState = !formState[inputName];
    const stateTemplate = getStateTemplate();
    let additionalState = {};

    if (inputName === INCLUDE_STANDARD_LICENSE_TEXTS_PROP_NAME && !newCheckState) {
      additionalState = {
        includeAppendix: false,
      };
    }

    if (attributionReports.selectedTemplateIndex >= 0) {
      isTemplatePristine.current = stateTemplate[inputName] === newCheckState;
    } else {
      isTemplatePristine.current = defaultFormState[inputName] === newCheckState;
    }

    if (isTemplatePristine.current) {
      isTemplatePristine.current = getTemplatePristinity(inputName);
    } else {
      setDirtyFlagToAttributionReport(true);
    }
    setFormState({
      ...formState,
      [inputName]: newCheckState,
      ...additionalState,
    });
  };

  const textInputSetter = (textInput) => (value) => {
    setFormState({
      ...formState,
      [textInput]: value,
    });
  };

  const reportUrl = getAttributionReportUrl(applicationPublicId, stageTypeId);

  const backHref =
    applicationPublicId && stageTypeId
      ? $state.href($state.get('legal.applicationDetails'), {
          applicationPublicId,
          stageTypeId,
        })
      : $state.href($state.get('legal.dashboard'));

  const handleDiscardedChangesConfirmation = () => {
    if (templateIndexToSelect === attributionReports.selectedTemplateIndex) {
      setInitialFormData();
    } else {
      isTemplatePristine.current = templateIndexToSelect !== NEW_TEMPLATE_INDEX;
      applyAttributionReportTemplateByIndex(templateIndexToSelect);
    }
    isTemplatePristine.current = true;
    setTemplateIndexToSelect(null);
  };

  const discardChangesConfirmation = (e, templateIndex) => {
    e.preventDefault();
    setDropDownCollapsed(true);
    isTemplatePristine.current = getTemplatePristinity();
    if (!isTemplatePristine.current) {
      setTemplateIndexToSelect(templateIndex);
    } else {
      isTemplatePristine.current = templateIndex !== NEW_TEMPLATE_INDEX;
      applyAttributionReportTemplateByIndex(templateIndex);
    }
  };

  const getDropdownDefaultText = () => {
    const notPristineMark = !isTemplatePristine.current ? ' (edited)' : '';
    return attributionReports.selectedTemplateIndex === NEW_TEMPLATE_INDEX
      ? 'Templates'
      : attributionReportTemplates.results[attributionReports.selectedTemplateIndex].templateName + notPristineMark;
  };

  const fileInputsChangeHandler = (fileInputs = []) => {
    if (fileInputs.length > 0) {
      setDirtyFlagToAttributionReport(true);
    } else {
      getTemplatePristinity();
    }
  };

  return (
    <main id="attribution-report-form-container" className="nx-page-main">
      <MenuBarBackButton href={backHref} text="Back" />
      <div className="nx-page-title">
        <h1 className="nx-h1">Create Attribution Report</h1>
        <div className="nx-btn-bar">
          <NxButton
            variant="tertiary"
            onClick={() => {
              stateGo('legal.attributionReportTemplate', {
                applicationPublicId,
                stageTypeId,
              });
            }}
          >
            Manage Templates
          </NxButton>
        </div>
      </div>
      <section className="nx-tile" aria-label="Attribution Report Settings">
        <NxForm
          submitBtnText="Generate Report"
          target="_blank"
          onSubmit={() => formReference.current.submit()}
          ref={formReference}
          action={reportUrl}
          method="post"
          encType="multipart/form-data"
          validationErrors={formState.documentTitle.validationErrors}
          submitMaskMessage="Generating Report…"
          submitErrorTitleMessage="There was an error generating the report: "
        >
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Attribution Report Settings</h2>
            </div>
            <NxDropdown
              label={getDropdownDefaultText()}
              isOpen={isDropDownOpen}
              onToggleCollapse={setDropDownCollapsed}
              disabled={attributionReportTemplates.results.length === 0}
            >
              {attributionReportTemplates.results.map((template, index) => (
                <button
                  onClick={(e) => discardChangesConfirmation(e, index)}
                  key={index}
                  className="nx-dropdown-button"
                >
                  {template.templateName}
                </button>
              ))}
            </NxDropdown>
          </header>
          <div className="nx-tile-content">
            <NxFormGroup label="Report Title" isRequired>
              <NxTextInput
                className="nx-text-input--long"
                validatable
                {...formState.documentTitle}
                name="title"
                onChange={textInputChangeHandler('documentTitle')}
                placeholder="Enter Report Title"
              />
            </NxFormGroup>
            <div className="nx-form-row">
              <NxFormGroup label="Document Header">
                <NxTextInput
                  className="nx-text-input"
                  name="header"
                  {...formState.header}
                  onChange={textInputChangeHandler('header')}
                  placeholder="Enter Document Header"
                />
              </NxFormGroup>
              <NxFormGroup label="Document Footer">
                <NxTextInput
                  className="nx-text-input"
                  name="footer"
                  {...formState.footer}
                  onChange={textInputChangeHandler('footer')}
                  placeholder="Enter Document Footer"
                />
              </NxFormGroup>
            </div>
            <NxFieldset label="Table of contents" isRequired>
              <NxCheckbox
                onChange={toggle('includeTableOfContents')}
                isChecked={formState.includeTableOfContents}
                id="table-of-contents-checkbox"
              >
                Include Table of Contents at the beginning of the report.
              </NxCheckbox>
            </NxFieldset>
            <NxFieldset label="Include Standard License Texts" isRequired>
              <NxCheckbox
                onChange={toggle(INCLUDE_STANDARD_LICENSE_TEXTS_PROP_NAME)}
                isChecked={formState.includeStandardLicenseTexts}
                id="include-standard-license-checkbox"
              >
                Include Standard License Text when no licenses files are found.
              </NxCheckbox>
            </NxFieldset>
            <NxFieldset label="Appendix" isRequired>
              <NxCheckbox
                onChange={toggle('includeAppendix')}
                isChecked={formState.includeAppendix}
                disabled={!formState.includeStandardLicenseTexts}
                id="appendix-checkbox"
              >
                Include Appendix that displays all Standard Licence Texts at the end of the report and inserts
                hyperlinks where relevant.
              </NxCheckbox>
            </NxFieldset>
            <input type="hidden" name="includeToc" value={formState.includeTableOfContents.toString()} />
            <input
              type="hidden"
              name={INCLUDE_STANDARD_LICENSE_TEXTS_PROP_NAME}
              value={formState.includeStandardLicenseTexts.toString()}
            />
            <input type="hidden" name="includeAppendix" value={formState.includeAppendix.toString()} />
            {templateIndexToSelect !== null && (
              <ConfirmationModal
                {...{
                  id: 'attribution-report-unsaved-dialog',
                  titleContent: <span>Unsaved Changes</span>,
                  cancelHandler: () => setTemplateIndexToSelect(null),
                  confirmationMessage: `This template has unsaved changes. Are you sure you want to continue? All unsaved changes will
                    be lost.`,
                  closeHandler: () => setTemplateIndexToSelect(null),
                  confirmationHandler: handleDiscardedChangesConfirmation,
                  confirmationButtonText: 'Continue',
                }}
              />
            )}
            <AttributionAdditionalFiles onFilesChange={fileInputsChangeHandler} />
          </div>
        </NxForm>
      </section>
    </main>
  );
}

AttributionReportForm.propTypes = {
  applicationPublicId: PropTypes.string,
  stageTypeId: PropTypes.string,
  stateGo: PropTypes.func,
  $state: PropTypes.object.isRequired,
  attributionReports: PropTypes.object,
  attributionReportTemplates: PropTypes.object,
  getAttributionReportTemplates: PropTypes.func.isRequired,
  applyAttributionReportTemplateByIndex: PropTypes.func.isRequired,
  setDirtyFlagToAttributionReport: PropTypes.func.isRequired,
};
