/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';
import ConfirmationModal from './ConfirmationModal';
import {
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxStatefulForm,
  NxFormGroup,
  NxLoadError,
  NxLoadingSpinner,
  NxTextInput,
  nxTextInputStateHelpers,
  NxFontAwesomeIcon,
  combineValidationErrors,
} from '@sonatype/react-shared-components';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

export default function AttributionReportTemplateForm(props) {
  const {
    applicationPublicId,
    stageTypeId,
    attributionReportTemplates,
    getAttributionReportTemplates,
    saveAttributionReportTemplate,
    deleteAttributionReportTemplateById,
    selectAttributionReportTemplate,
    $state,
    setDirtyFlagToAttributionReportTemplate,
  } = props;
  const { initialState, userInput } = nxTextInputStateHelpers;
  const defaultFormState = {
    id: null,
    templateName: initialState('New Template'),
    documentTitle: initialState('New Report Title'),
    header: initialState(''),
    footer: initialState(''),
    includeTableOfContents: true,
    includeStandardLicenseTexts: true,
    includeAppendix: true,
    includeSonatypeSpecialLicenses: false,
    includeInnerSource: false,
  };
  const [formState, setFormState] = React.useState(defaultFormState);
  const [templateIndexToSelect, setTemplateIndexToSelect] = React.useState(null);
  const [templateIndexToDelete, setTemplateIndexToDelete] = React.useState(null);
  const NEW_TEMPLATE_INDEX = -1;
  const INCLUDE_STANDARD_LICENSE_TEXTS_PROP_NAME = 'includeStandardLicenseTexts';
  const requestErrors = {
    saveError: null,
    loadError: null,
    deleteError: null,
    errorTitle: null,
    errorMessage: null,
  };
  let isTemplatePristine = React.useRef(true);

  if (attributionReportTemplates.error) {
    requestErrors[attributionReportTemplates.error.type] = attributionReportTemplates.error.message;
    if (requestErrors.saveError) requestErrors.errorTitle = 'An error occurred saving the template:';
    else if (requestErrors.deleteError) requestErrors.errorTitle = 'An error occurred deleting the template:';
    else if (requestErrors.loadError) requestErrors.errorTitle = 'An error occurred loading templates:';

    requestErrors.errorMessage = attributionReportTemplates.error.message;
  }

  const toggle = (inputName) => () => {
    const newCheckState = !formState[inputName];
    const stateTemplate = getStateTemplate();
    let additionalState = {};

    if (inputName === INCLUDE_STANDARD_LICENSE_TEXTS_PROP_NAME && !newCheckState) {
      additionalState = {
        includeAppendix: false,
      };
    }

    if (attributionReportTemplates.selectedTemplateIndex >= 0) {
      isTemplatePristine.current = stateTemplate[inputName] === newCheckState;
    } else {
      isTemplatePristine.current = defaultFormState[inputName] === newCheckState;
    }

    if (isTemplatePristine.current) {
      isTemplatePristine.current = getTemplatePristinity(inputName);
    } else {
      setDirtyFlagToAttributionReportTemplate(true);
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

  const submitHandler = () => {
    if (requestErrors.deleteError) {
      deleteAttributionReportTemplateById(
        attributionReportTemplates.results[attributionReportTemplates.selectedTemplateIndex].id
      );
    } else {
      isTemplatePristine.current = true;
      saveAttributionReportTemplate(getCurrentTemplateData());
    }
  };

  const getDuplicatedTemplateNameError = (templateName) => {
    if (
      formState &&
      attributionReportTemplates.results.find((template) => {
        return template.templateName === templateName && template.id !== formState.id;
      })
    ) {
      return 'Template name already exists';
    }
    return null;
  };

  const getTemplatePristineError = () => {
    return isTemplatePristine.current ? 'No changes made on template yet' : null;
  };

  const textInputsValidationErrors = combineValidationErrors(
    formState.documentTitle.validationErrors,
    formState.templateName.validationErrors,
    getTemplatePristineError(),
    getDuplicatedTemplateNameError(formState.templateName.value)
  );

  const validator = (inputName) => (val) => {
    const labelName = inputName === 'templateName' ? 'Template Name' : 'Report Title';
    const duplicatedTemplateError = getDuplicatedTemplateNameError(val);
    if (inputName === 'templateName' && duplicatedTemplateError) {
      return duplicatedTemplateError;
    }
    if (val.length === 0) {
      return `${labelName} cannot be empty`;
    }
    return null;
  };

  const getStateTemplate = () =>
    attributionReportTemplates.selectedTemplateIndex >= 0
      ? attributionReportTemplates.results[attributionReportTemplates.selectedTemplateIndex]
      : null;

  const getTemplatePristinity = (inputToOmit) => {
    let stateTemplate = getStateTemplate() || defaultFormState;
    let currentTemplate = getCurrentTemplateData();
    const isPristine =
      !!stateTemplate &&
      !Object.keys(currentTemplate).find((templateProperty) => {
        if (templateProperty === 'lastUpdatedAt' || templateProperty === 'id' || inputToOmit === templateProperty)
          return false;
        return (
          (typeof stateTemplate[templateProperty] === 'object'
            ? stateTemplate[templateProperty].value
            : stateTemplate[templateProperty]) !== currentTemplate[templateProperty]
        );
      });
    setDirtyFlagToAttributionReportTemplate(!isPristine);
    return isPristine;
  };

  const textInputChangeHandler = (inputName) => (val) => {
    const stateTemplate = getStateTemplate();

    if (attributionReportTemplates.selectedTemplateIndex >= 0) {
      isTemplatePristine.current = stateTemplate[inputName] === val;
    } else {
      isTemplatePristine.current = defaultFormState[inputName].value === val;
    }

    if (isTemplatePristine.current) {
      isTemplatePristine.current = getTemplatePristinity(inputName);
    } else {
      setDirtyFlagToAttributionReportTemplate(true);
    }

    textInputSetter(inputName)(userInput(validator(inputName), val));
  };

  const getCurrentTemplateData = () => ({
    id:
      attributionReportTemplates.selectedTemplateIndex >= 0
        ? attributionReportTemplates.results[attributionReportTemplates.selectedTemplateIndex].id
        : null,
    templateName: formState.templateName.value,
    documentTitle: formState.documentTitle.value,
    header: formState.header.value,
    footer: formState.footer.value,
    includeTableOfContents: formState.includeTableOfContents,
    includeStandardLicenseTexts: formState.includeStandardLicenseTexts,
    includeAppendix: formState.includeAppendix,
    includeSonatypeSpecialLicenses: formState.includeSonatypeSpecialLicenses,
    includeInnerSource: formState.includeInnerSource,
  });

  const deleteTemplateButton = attributionReportTemplates.selectedTemplateIndex >= 0 && !requestErrors.deleteError && (
    <NxButton
      id="attribution-report-delete-template"
      variant="tertiary"
      type="button"
      onClick={() =>
        setTemplateIndexToDelete(
          attributionReportTemplates.results[attributionReportTemplates.selectedTemplateIndex].id
        )
      }
    >
      Delete Template
    </NxButton>
  );

  const discardChangesConfirmation = (templateIndex) => {
    if (templateIndex !== NEW_TEMPLATE_INDEX && templateIndex === attributionReportTemplates.selectedTemplateIndex)
      return;
    let stateTemplate = getStateTemplate();
    isTemplatePristine.current = getTemplatePristinity();
    if (!stateTemplate || !isTemplatePristine.current) {
      setTemplateIndexToSelect(templateIndex);
    } else {
      isTemplatePristine.current = templateIndex !== NEW_TEMPLATE_INDEX;
      selectAttributionReportTemplate(templateIndex);
    }
  };

  function setInitialFormData() {
    const {
      id,
      templateName,
      documentTitle,
      header,
      footer,
      includeTableOfContents,
      includeStandardLicenseTexts,
      includeAppendix,
      includeSonatypeSpecialLicenses,
      includeInnerSource,
    } = attributionReportTemplates.results[attributionReportTemplates.selectedTemplateIndex];
    setFormState({
      id,
      templateName: initialState(templateName),
      documentTitle: initialState(documentTitle),
      header: initialState(header),
      footer: initialState(footer),
      includeTableOfContents,
      includeStandardLicenseTexts,
      includeAppendix,
      includeSonatypeSpecialLicenses,
      includeInnerSource,
    });
  }

  React.useEffect(() => {
    getAttributionReportTemplates();
    return () => setDirtyFlagToAttributionReportTemplate(false);
  }, []);

  React.useEffect(() => {
    if (attributionReportTemplates.selectedTemplateIndex >= 0) {
      isTemplatePristine.current = true;
      setInitialFormData();
    } else {
      isTemplatePristine.current = false;
      setFormState(defaultFormState);
    }
  }, [attributionReportTemplates.selectedTemplateIndex, attributionReportTemplates.results.length]);

  const backHref =
    applicationPublicId && stageTypeId
      ? $state.href($state.get('legal.attributionReport'), {
          applicationPublicId,
          stageTypeId,
        })
      : $state.href($state.get('legal.attributionReportMultiApp'));

  const formTitle = `${
    attributionReportTemplates.selectedTemplateIndex === NEW_TEMPLATE_INDEX ? 'Create' : 'Edit'
  } Template`;

  const handleDiscardedChangesConfirmation = () => {
    if (
      templateIndexToSelect === NEW_TEMPLATE_INDEX &&
      templateIndexToSelect === attributionReportTemplates.selectedTemplateIndex
    ) {
      isTemplatePristine.current = false;
      setFormState(defaultFormState);
    } else {
      isTemplatePristine.current = templateIndexToSelect !== NEW_TEMPLATE_INDEX;
      selectAttributionReportTemplate(templateIndexToSelect);
    }
    setTemplateIndexToSelect(null);
  };

  const getComponentContent = () => {
    if (attributionReportTemplates.loading) {
      return <NxLoadingSpinner />;
    } else if (requestErrors.loadError) {
      return (
        <NxLoadError
          titleMessage={requestErrors.errorTitle}
          error={requestErrors.loadError}
          retryHandler={getAttributionReportTemplates}
        />
      );
    } else
      return (
        <Fragment>
          <div className="nx-page-title">
            <h1 className="nx-h1">Manage Attribution Report Templates</h1>
            <div className="nx-btn-bar">
              <NxButton
                id="attribution-report-create-new-template"
                variant="tertiary"
                onClick={() => discardChangesConfirmation(NEW_TEMPLATE_INDEX)}
              >
                Create New Template
              </NxButton>
            </div>
          </div>
          <div className="iq-grid-container">
            <div>
              <h3 className="nx-h3">Templates</h3>
              <ul id="attribution-report-template-list" className="nx-list">
                {attributionReportTemplates.results.map((art, index) => (
                  <li className="nx-list__item nx-list__item--clickable" key={index}>
                    <a
                      className={
                        'nx-list__link ' +
                        (attributionReportTemplates.selectedTemplateIndex === index ? 'selected' : '')
                      }
                      onClick={() => discardChangesConfirmation(index)}
                    >
                      {art.templateName}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
            <section className="nx-tile" aria-label="Attribution Report Settings">
              <NxStatefulForm
                additionalFooterBtns={deleteTemplateButton}
                submitBtnText="Save Template"
                onSubmit={submitHandler}
                validationErrors={textInputsValidationErrors}
                submitErrorTitleMessage={requestErrors.errorTitle}
                submitError={requestErrors.errorMessage}
                submitMaskState={attributionReportTemplates.submitMaskState}
                submitMaskSuccessMessage="Success"
              >
                <header className="nx-tile-header">
                  <div className="nx-tile-header__title">
                    <h2 id="attribution-report-form-title" className="nx-h2">
                      {formTitle}
                    </h2>
                  </div>
                </header>
                <div className="nx-tile-content">
                  <NxFormGroup label="Template Name" isRequired>
                    <NxTextInput
                      className="nx-text-input--long"
                      validatable
                      {...formState.templateName}
                      name="templateName"
                      onChange={textInputChangeHandler('templateName')}
                      placeholder="New Template"
                      maxLength="250"
                    />
                  </NxFormGroup>
                  <hr />
                  <h3 className="nx-h3">Attribution Report Settings</h3>
                  <NxFormGroup label="Report Title" isRequired>
                    <NxTextInput
                      className="nx-text-input--long"
                      validatable
                      {...formState.documentTitle}
                      name="title"
                      onChange={textInputChangeHandler('documentTitle')}
                      placeholder="Enter Report Title"
                      maxLength="250"
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
                        maxLength="50"
                      />
                    </NxFormGroup>
                    <NxFormGroup label="Document Footer">
                      <NxTextInput
                        className="nx-text-input"
                        name="footer"
                        {...formState.footer}
                        onChange={textInputChangeHandler('footer')}
                        placeholder="Enter Document Footer"
                        maxLength="50"
                      />
                    </NxFormGroup>
                  </div>
                  <NxFieldset label="Table of contents">
                    <NxCheckbox
                      onChange={toggle('includeTableOfContents')}
                      isChecked={formState.includeTableOfContents}
                      id="table-of-contents-checkbox"
                    >
                      Include Table of Contents at the beginning of the report.
                    </NxCheckbox>
                  </NxFieldset>
                  <NxFieldset label="Include Standard License Texts">
                    <NxCheckbox
                      onChange={toggle(INCLUDE_STANDARD_LICENSE_TEXTS_PROP_NAME)}
                      isChecked={formState.includeStandardLicenseTexts}
                      id="include-standard-license-checkbox"
                    >
                      Include Standard License Text when no licenses files are found.
                    </NxCheckbox>
                  </NxFieldset>
                  <NxFieldset
                    label="Appendix"
                    sublabel={
                      'Displays all Standard License Texts at the end of the report and inserts ' +
                      'hyperlinks where relevant.'
                    }
                  >
                    <NxCheckbox
                      onChange={toggle('includeAppendix')}
                      isChecked={formState.includeAppendix}
                      disabled={!formState.includeStandardLicenseTexts}
                      id="appendix-checkbox"
                    >
                      Include Appendix.
                    </NxCheckbox>
                  </NxFieldset>
                  <NxFieldset
                    label="Sonatype Special Licenses"
                    sublabel="(No Sources, Unassigned, Commercial, Not Supported, etc)."
                  >
                    <NxCheckbox
                      onChange={toggle('includeSonatypeSpecialLicenses')}
                      isChecked={formState.includeSonatypeSpecialLicenses}
                      id="sonatype-special-licenses-checkbox"
                    >
                      Include Sonatype special license detections.
                    </NxCheckbox>
                  </NxFieldset>
                  <NxFieldset
                    label="InnerSource Components"
                    sublabel={
                      'InnerSource components are internally ' +
                      'developed components that are shared with other internal projects.'
                    }
                  >
                    <NxCheckbox
                      onChange={toggle('includeInnerSource')}
                      isChecked={formState.includeInnerSource}
                      id="include-inner-source-checkbox"
                    >
                      Include InnerSource components.
                    </NxCheckbox>
                  </NxFieldset>
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
                  {templateIndexToDelete !== null && (
                    <ConfirmationModal
                      {...{
                        id: 'attribution-report-delete-confirmation-dialog',
                        titleContent: (
                          <Fragment>
                            <NxFontAwesomeIcon icon={faTrashAlt} />
                            <span>Delete Report</span>
                          </Fragment>
                        ),
                        cancelHandler: () => setTemplateIndexToDelete(null),
                        confirmationMessage: `You are about to delete "${
                          attributionReportTemplates.results[attributionReportTemplates.selectedTemplateIndex]
                            .templateName
                        }". This action cannot be undone.`,
                        closeHandler: () => setTemplateIndexToDelete(null),
                        confirmationHandler: () => {
                          deleteAttributionReportTemplateById(templateIndexToDelete);
                          setTemplateIndexToDelete(null);
                        },
                        confirmationButtonText: 'Delete',
                      }}
                    />
                  )}
                </div>
              </NxStatefulForm>
            </section>
          </div>
        </Fragment>
      );
  };

  return (
    <main id="attribution-report-template-form-container" className="nx-page-main">
      <MenuBarBackButton href={backHref} text="Back" />
      {getComponentContent()}
    </main>
  );
}

AttributionReportTemplateForm.propTypes = {
  applicationPublicId: PropTypes.string,
  stageTypeId: PropTypes.string,
  attributionReportTemplates: PropTypes.object,
  getAttributionReportTemplates: PropTypes.func,
  saveAttributionReportTemplate: PropTypes.func,
  deleteAttributionReportTemplateById: PropTypes.func,
  selectAttributionReportTemplate: PropTypes.func,
  $state: PropTypes.object.isRequired,
  setDirtyFlagToAttributionReportTemplate: PropTypes.func.isRequired,
};
