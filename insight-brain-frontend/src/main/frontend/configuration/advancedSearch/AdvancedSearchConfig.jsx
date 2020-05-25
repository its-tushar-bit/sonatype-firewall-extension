/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import MaximizedContainer from '../../react/MaximizedContainer';
import NxExternalLink from '../../react/NxExternalLink';
import {
  NxButton,
  NxCheckbox,
  NxErrorAlert,
  NxFontAwesomeIcon,
  NxModal,
  NxStatefulSubmitMask
} from '@sonatype/react-shared-components';

import { faSyncAlt } from '@fortawesome/free-solid-svg-icons';

export default function AdvancedSearchConfig(props) {
  // Actions
  const {
    load,
    save,
    setIsEnabled,
    resetForm,
    reIndex,
    closeReIndexModal
  } = props;

  // View State
  const {
    loading,
    submitMaskState,
    submitMaskMessage,
    isDirty,
    showReIndexModal,
    error,
    isAuthorized
  } = props;

  // Form State
  const {
    isEnabled,
    lastIndexTime
  } = props;

  useEffect(load, []);

  const reIndexingModal = (
    <NxModal id="advanced-search-re-indexing-modal" onClose={closeReIndexModal}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Re-Indexing</h2>
      </header>
      <div className="nx-modal-content">
        <NxFontAwesomeIcon icon={faSyncAlt} spin={true} />
        Re-indexing is in progress. Closing this modal will not interrupt the process.
      </div>
      <footer className="nx-modal-footer">
        <div className="nx-btn-bar">
          <NxButton type="button"
                    id="advanced-search-re-indexing-modal-close-button"
                    onClick={closeReIndexModal}
                    className="nx-btn">
            Close
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );

  function onSubmit(evt) {
    evt.preventDefault();
    save();
  }

  function reIndexHandler(evt) {
    evt.preventDefault();
    reIndex();
  }

  return (
    isAuthorized ? <LoadWrapper loading={loading} error={error}>
      <MaximizedContainer id="advanced-search-config-page-container"
                          className="iq-body-container iq-body-container--single-pane">
        <div id="advanced-search-config" className="iq-tile iq-tile--sys-prefs">
          <Fragment>
            <div className="iq-tile-header">
              <div className="iq-tile-header__title">
                <h2>Advanced Search</h2>
              </div>
            </div>
            <p>
              Here you can enable the early access Advanced Search feature. Please read the <NxExternalLink
                href="https://links.sonatype.com/products/nxiq/doc/advanced-search">documentation
              </NxExternalLink>.
              Advanced Search, once enabled, should be periodically re-indexed.
              Re-indexing may impact the performance of IQ Server while it is running, so it is recommended to do this
              during a time of low usage.
            </p>
            <div>
              As this feature is early access, there are a number of caveats:
              <ul>
                <li>
                  The resulting dataset does not adhere to any configured permissions (all results are displayed)
                </li>
                <li>
                  To see new data in the results a re-index is required.
                  This can either be automated using the provided REST API or manually from the UI
                </li>
                <li>
                  The syntax and/or keyword fields may subsequently change on different releases
                </li>
              </ul>
            </div>
            <p>
              This feature is in development and functionality can be expected to change.
              Please provide any feedback you may have <NxExternalLink
                href='https://links.sonatype.com/products/nxiq/feedback/advanced-search'>here</NxExternalLink>.
            </p>
            <div>
              {submitMaskState !== null &&
              <NxStatefulSubmitMask success={submitMaskState} message={submitMaskMessage} />}
              <form className="nx-form" onSubmit={onSubmit}>
                <fieldset className="nx-fieldset">
                  <legend className="nx-label">Advanced Search</legend>
                  <div className="nx-form-group">
                    <NxCheckbox id="advanced-search-config-is-enabled-checkbox"
                                isChecked={isEnabled}
                                onChange={setIsEnabled}>
                      Opt-In to Experimental Advanced Search
                    </NxCheckbox>
                  </div>
                </fieldset>
                <div className="nx-form-row">
                  <div className="nx-form-group">
                    <fieldset className="nx-fieldset">
                      <legend className="nx-label">Indexing</legend>
                      <div className="nx-form-group">
                        <button id="advanced-search-config-re-index-button"
                                onClick={reIndexHandler}
                                disabled={!isEnabled}
                                className="nx-btn">
                          Re-Index
                        </button>
                      </div>
                    </fieldset>
                  </div>
                  <div className="nx-form-group">
                    <span className="nx-sub-label">Last Indexed:&nbsp;
                      {lastIndexTime ? new Date(lastIndexTime).toLocaleString() : ''}</span>
                  </div>
                </div>
                <div className='iq-tile-footer'>
                  <div className="nx-btn-bar">
                    <NxButton type="submit"
                              id="advanced-search-config-save"
                              variant="primary"
                              disabled={!isDirty}>
                      Save
                    </NxButton>
                    <NxButton type="button"
                              id="advanced-search-config-cancel"
                              onClick={resetForm}
                              disabled={!isDirty}>
                      Cancel
                    </NxButton>
                  </div>
                </div>
              </form>
            </div>
          </Fragment>
        </div>
        {
          showReIndexModal && reIndexingModal
        }
      </MaximizedContainer>
    </LoadWrapper> :
    <NxErrorAlert>
      It appears you do not have permission to access this page.
      If you believe this to be incorrect please contact your administrator.
    </NxErrorAlert>
  );
}

AdvancedSearchConfig.propTypes = {
  isEnabled: PropTypes.bool.isRequired,
  setIsEnabled: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
  load: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  error: PropTypes.object,
  submitMaskState: PropTypes.bool,
  submitMaskMessage: PropTypes.string,
  reIndex: PropTypes.func.isRequired,
  showReIndexModal: PropTypes.bool.isRequired,
  closeReIndexModal: PropTypes.func.isRequired,
  lastIndexTime: PropTypes.number,
  isAuthorized: PropTypes.bool.isRequired
};
