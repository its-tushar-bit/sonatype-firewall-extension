import 'components-font-awesome/css/font-awesome.min.css';
import './lib/glyphicon/glyphicons.css';
import './lib/bootstrap.scss';
import './lib/bootstrap/bootstrap-slider-2.0.0.css';
import 'bootstrap-toggle/css/bootstrap2-toggle.css';
import './lib/glyphicon/halflings.css';
import 'angular-xeditable/dist/css/xeditable.css';
import 'plottable/plottable.css';
import './glyphicons-sonatype.css';
import './sonatype-icons.css';
import './scss/scss.scss';

import './lib/jquery-loader';
import 'angular';
import 'angular-aria';
import 'angular-cookies';
import 'angular-route';
import 'angular-sanitize';
import 'angular-ui-validate';
import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'angular-vs-repeat';
import 'angular-xeditable';
import 'ngUpload/ng-upload';
import './lib/bootstrap-loader';
import 'es6-collections';

// d3 has a commonjs impl and a es6 impl.  The commonjs impl doesn't work with rollup for
// unknown reasons and the es6 one isn't detected correctly, despite the jsnext config which
// should enable it.  As a workaround we point to the specific file that has the es6 imports
// See https://stackoverflow.com/questions/39909200/d3-4-0-import-statement-gives-moduleexports-wrapper
import * as d3 from 'd3/index.js';

import 'zeroclipboard';
import Fuse from 'fuse.js';
import 'jquery-ui/ui/effect';
import ClassyBrew from 'classybrew/src/classybrew';
import Plottable from 'plottable';

import './utility/Polyfills';
import './components/module';
import './directives/module';
import './changeDefaultAdminPasswordNotice/module';
import './components/iqRenderPlottable/iqRenderPlottable';
import './components/iqBackButton/iqBackButton';
import './labs/module';
import './ComponentDisplay/module';
import './dashboard/dashboard.module';
import './EditorTools';
import './mainHeader/module';
import './mainHeader/userMenu/CurrentUserService';
import './mainHeader/helpMenu/helpMenu';
import './mainHeader/userMenu/userMenu';
import './mainHeader/notificationsMenu/notificationsMenu';
import './mainHeader/systemConfigurationMenu/systemConfigurationMenu';
import './mainHeader/mainHeader';
import './MainModule';
import './ManagementApp';
import './ReportApp';
import './SessionSecurityModule';
import './configuration/ldap/ldap.module';
import './configuration/ldap/ldap.configuration.store';
import './configuration/ldap/LdapConfigurationController';
import './configuration/ldap/ldap.server.list.controller';
import './configuration/ldap/ldap.server.ordering.controller';
import './configuration/webhook/webhook.module';
import './configuration/webhook/webhook.view.controller';
import './configuration/webhook/webhook.list.controller';
import './configuration/webhook/webhook.edit.controller';
import './configuration/license/ProductLicenseModule';
import './configuration/systemNoticeConfiguration/systemNoticeConfigurationModule';
import './configuration/systemNoticeConfiguration/systemNoticeConfiguration';
import './dashboard/ComponentController';
import './policy/AppSecurityController';
import './report/ReportController';
import './report/ReportViolationsController';
import './report/violations/sortColumns.directive';
import './report/violations/sortable.directive';
import './report/repository.reevaluate.modal.controller';
import './report/repository.reevaluate.service';
import './report/repository.report.controller';
import './security/RoleModule';
import './security/UserModule';
import './security/user.list.controller';
import './security/userForm/userForm';
import './util/Globals';

import './owner.manager/owner.manager.module';
import './role.membership/role.membership.directive';
import './role.membership/role.membership.controller';

import './root.organization.migrate/root.organization.migrate.module';
import './root.organization.migrate/root.organization.migrate.directive';
import './root.organization.migrate/root.organization.migrate.modal.controller';
import './root.organization.migrate/root.organization.migrate.modal.service';

import './systemNotice/systemNoticeModule';
import './systemNotice/systemNoticeService';
import './systemNotice/systemNotice';

import Base64 from './lib/Base64';

window.Base64 = Base64;
window.Fuse = Fuse;
window.d3 = d3;
window.Plottable = Plottable;
window.classyBrew = ClassyBrew;
