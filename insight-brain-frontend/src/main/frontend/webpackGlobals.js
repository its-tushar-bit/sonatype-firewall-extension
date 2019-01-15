/* global CLM_SERVER_VERSION, CLM_BUILD_TIMESTAMP */
// constants provided by webpack DefinePlugin
window.clmServerVersion = CLM_SERVER_VERSION;
window.clmBuildTimestamp = CLM_BUILD_TIMESTAMP;
window.angularDebug = process.env.NODE_ENV !== 'production';
