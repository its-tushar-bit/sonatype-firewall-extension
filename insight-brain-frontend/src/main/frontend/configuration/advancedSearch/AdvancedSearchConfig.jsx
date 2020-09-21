/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
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
  NxSubmitMask
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
                          className="nx-page-content">
        <div className="nx-page-main">
          <div id="advanced-search-config" className="iq-tile iq-tile--sys-prefs">
            <div className="iq-tile-header">
              <div className="iq-tile-header__title">
                <h2 className="nx-h2">Advanced Search Configuration</h2>
              </div>
            </div>
            <p className="nx-p">
              Advanced Search gives you robust search options to help you find exactly what you are looking for.
              Search terms give you the ability to scope your search to specific types of information relating to the
              following categories:
            </p>
            <div className="nx-list nx-list--bulleted">
              <ul>
                <li className="nx-list__item">
                  Organizations
                </li>
                <li className="nx-list__item">
                  Applications
                </li>
                <li className="nx-list__item">
                  Application Categories
                </li>
                <li className="nx-list__item">
                  Component Labels
                </li>
                <li className="nx-list__item">
                  Policies
                </li>
                <li className="nx-list__item">
                  Security Vulnerabilities
                </li>
              </ul>
            </div>
            <p className="nx-p">You can combine multiple search terms to craft an even more targeted search.
            </p>
            <p className="nx-p">
              For more information on how to use this feature,{' '}
              <NxExternalLink href='https://links.sonatype.com/products/nxiq/doc/advanced-search'>
                check out the documentation
              </NxExternalLink>.
            </p>
            <div>
              {submitMaskState !== null &&
              <NxSubmitMask success={submitMaskState} message={submitMaskMessage} />}
              <form className="nx-form" onSubmit={onSubmit}>
                <fieldset className="nx-fieldset">
                  <legend className="nx-label">Advanced Search Status</legend>
                  <div className="nx-form-group">
                    <NxCheckbox id="advanced-search-config-is-enabled-checkbox"
                                isChecked={isEnabled}
                                onChange={setIsEnabled}>
                      Enabled
                    </NxCheckbox>
                  </div>
                  <p className="nx-p">Note: It is recommended that you manually re-index after enabling this feature in
                    order for Advanced Search to index your historical data.
                  </p>
                </fieldset>
                <h3 className="nx-h3">Indexing</h3>
                <p className="nx-p">To ensure search results are as accurate as possible,  Advanced Search automatically
                  re-indexes when any changes are made to relevant IQ Server application data. Automatic indexing only
                  applies to data changes made whilst the feature is enabled. In order for Advanced Search to index
                  historical data, you must run a manual index. Re-indexing may impact the performance of IQ Server
                  while it is running, so it is recommended to do this during a time of low usage.
                </p>
                <p className="nx-p">If you would like to manually re-index, you can do so below:
                </p>
                <div className="nx-form-row">
                  <div className="nx-form-group">
                    <fieldset className="nx-fieldset">
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
          </div>
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
