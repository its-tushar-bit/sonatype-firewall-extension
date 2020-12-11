/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import MaximizedContainer from '../react/MaximizedContainer';
import LoadWrapper from '../react/LoadWrapper';
import { isNilOrEmpty } from '../util/jsUtil';

export default function AdvancedLegalApplicationPage(props) {
  const {
    // actions
    loadApplicationReport,
    // state,
    viewStateApplicationReport,
    publicId,
    applicationReport
  } = props;

  useEffect(() => {
    loadApplicationReport(publicId);
  }, []);

  return (
    <MaximizedContainer id="advanced-legal-application-report">
      <div className="nx-page">
        <div className="nx-page-content">
          <main className="nx-page-main">
            <h2 className="nx-h2">Attribution Report for {publicId}</h2>

            <LoadWrapper loading={viewStateApplicationReport.loading}
                         error={viewStateApplicationReport.error}
                         retryHandler={loadApplicationReport}>
              {applicationReport && applicationReport.components.map(component => {
                const { licenseLegalData } = component;

                return (
                  <section key={component.hash} className="nx-tile">
                    <header className="nx-tile-header">
                      <div className="nx-tile-header__title">
                        <h2 className="nx-h2">{component.displayName}</h2>
                      </div>
                    </header>

                    {licenseLegalData &&
                      <div className="nx-tile-content">
                        {!isNilOrEmpty(licenseLegalData.effectiveLicenses) &&
                          <section className="nx-tile">
                            <header className="nx-tile-header">
                              <div className="nx-tile-header__title">
                                <h4 className="nx-h4">Licensed Under</h4>
                              </div>
                            </header>
                            <div className="nx-tile-content">
                              {licenseLegalData.effectiveLicenses.join(', ')}
                            </div>
                          </section>
                        }

                        {!isNilOrEmpty(licenseLegalData.copyrights) &&
                          <section className="nx-tile">
                            <header className="nx-tile-header">
                              <div className="nx-tile-header__title">
                                <h4 className="nx-h4">Copyright Statements</h4>
                              </div>
                            </header>
                            <div className="nx-tile-content">
                              {licenseLegalData.copyrights.map((copyright, index) =>
                                <p key={index}>{copyright}</p>
                              )}
                            </div>
                          </section>
                        }

                        {!isNilOrEmpty(licenseLegalData.noticeFiles) > 0 &&
                          <section className="nx-tile">
                            <header className="nx-tile-header">
                              <div className="nx-tile-header__title">
                                <h4 className="nx-h4">Notice Texts</h4>
                              </div>
                            </header>
                            <div className="nx-tile-content">
                              {licenseLegalData.noticeFiles.map((noticeFile, index) =>
                                <pre key={index}>{noticeFile.content}</pre>
                              )}
                            </div>
                          </section>
                        }

                        {!isNilOrEmpty(licenseLegalData.licenseFiles) &&
                          <section className="nx-tile">
                            <header className="nx-tile-header">
                              <div className="nx-tile-header__title">
                                <h4 className="nx-h4">License Texts</h4>
                              </div>
                            </header>
                            <div className="nx-tile-content">
                              {licenseLegalData.licenseFiles.map((licenseFile, index) =>
                                <pre key={index}>{licenseFile.content}</pre>
                              )}
                            </div>
                          </section>
                        }

                        {isNilOrEmpty(licenseLegalData.licenseFiles) &&
                          !isNilOrEmpty(licenseLegalData.effectiveLicenses) &&
                          <section className="nx-tile">
                            <header className="nx-tile-header">
                              <div className="nx-tile-header__title">
                                <h4 className="nx-h4">Standard License Text</h4>
                              </div>
                            </header>
                            <div className="nx-tile-content">
                              {licenseLegalData.effectiveLicenses.map((effectiveLicense, index) => {
                                const license = applicationReport.licenseLegalMetadata
                                    .find(licenseMetadata => licenseMetadata.licenseId === effectiveLicense);

                                if (license && license.licenseText) {
                                  return (
                                    <pre key={index}>
                                      {license.licenseText}
                                    </pre>
                                  );
                                }
                              }
                              )}
                            </div>
                          </section>
                        }
                      </div>
                    }
                  </section>
                );
              })}
            </LoadWrapper>
          </main>
        </div>
      </div>
    </MaximizedContainer>
  );
}

AdvancedLegalApplicationPage.propTypes = {
  loadApplicationReport: PropTypes.func.isRequired,
  viewStateApplicationReport: PropTypes.shape({
    loading: PropTypes.bool.isRequired,
    error: PropTypes.string
  }),
  publicId: PropTypes.string.isRequired,
  applicationReport: PropTypes.object
};
