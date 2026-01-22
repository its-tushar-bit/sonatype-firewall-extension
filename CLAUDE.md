# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Nexus IQ Server** (insight-brain) implements a suite of products designed to secure the software supply chain, primarily focusing on open-source software (OSS) components. Their key products include:

* **Sonatype SBOM Manager:** This platform is designed to help organizations manage their Software Bill of Materials (SBOMs) effectively. It allows for the generation, ingestion, management, and storage of SBOMs in standard formats like CycloneDX and SPDX. By leveraging Sonatype Lifecycle's vulnerability data, SBOM Manager provides insights into components, vulnerabilities, and malware, enabling organizations to detect and mitigate security risks, demonstrate compliance with regulations (like NIS2 and US Executive Order on Cybersecurity), and streamline auditing processes. It also supports continuous monitoring and integrates with existing workflows.
* **Sonatype Lifecycle:** Lifecycle is Sonatype's software composition analysis (SCA) tool that helps manage open-source risks across the entire Software Development Lifecycle (SDLC). It allows organizations to define and automatically enforce policies for open-source components based on security, license, and quality risks. Lifecycle integrates with various development tools (IDEs, SCMs, CI/CD) to provide continuous monitoring, actionable insights, and automated remediation guidance, helping developers make secure open-source choices and accelerate their development processes.
* **Sonatype Firewall:** This product acts as a first line of defense against malicious open-source code entering your development pipelines. Sonatype Firewall automatically identifies and blocks known and suspected malicious components, including malware, AI models, and containers, before they can be downloaded into your repositories or workflows. It leverages AI behavioral analysis and policy enforcement to prevent risky components from reaching developers, thereby reducing remediation work and minimizing security incidents. Firewall can also continuously scan existing repositories to identify and quarantine previously introduced threats.

## Deployment Variants

**Nexus IQ Server** is available in two deployment variants, both implementing all three products listed above:

* **On-Premises IQ Server:** Traditional single-tenant deployment for individual organizations, with full administrative control and customization capabilities.
* **Multi-Tenant IQ (MTIQ):** A specialized variant designed to support multiple isolated tenants within a single deployment. This allows service providers and large enterprises to serve multiple organizations or business units while maintaining strict data isolation and security boundaries between tenants.

## Build Commands

### Maven Build Commands
- **Full build with tests**: `mvn clean install` (WARNING: takes a long time)
- **Quick build (skip tests)**: `mvn clean install -Pquick`
- **Fast tests only**: `mvn verify -DexcludedGroups=SlowTest`
- **Skip functional tests**: Add `-D skip-functional-test` to any mvn command
- **Local Chrome for functional tests**: `-Dwebdriver.chrome.driver=/your/path/to/chromedriver`
- **Single test class**: `mvn verify -Dtest=TestClassName -Dit.test=TestClassName`
- **Single test method**: `mvn verify -Dtest=TestClassName#testMethodName -Dit.test=TestClassName#testMethodName`

### Frontend Build Commands (insight-brain-frontend/)
- **Start dev server**: `yarn start`
- **Build for production**: `yarn build`
- **Run tests**: `yarn test` (runs both Jasmine and Jest tests)
- **Lint**: `yarn test-lint`
- **Jest tests**: `yarn jest`
- **Jasmine watch mode**: `yarn test-watch`
- **Jest watch mode**: `yarn jest-watch`
- **Individual test file**: `yarn jest -- <test-name>`

### Development Profiles
- **Quick profile**: `-Pquick` - skips tests, linting, and checks

## Architecture

### Multi-Module Maven Project Structure
- **insight-brain-service**: Main server application (Dropwizard + JAX-RS)
- **insight-brain-frontend**: React/Angular hybrid frontend (npm/webpack)
- **insight-brain-db**: Database layer (OpenJPA/PostgreSQL/H2)
- **insight-brain-data**: Data access layer
- **insight-brain-policy**: Policy engine (Drools)
- **insight-brain-client**: Client library
- **nexus-iq-server**: Main server bundle
- **nexus-mtiq-server**: Multi-tenant server variant

### Technology Stack
- **Backend**: Java 17, Dropwizard 3.x, JAX-RS, Guice DI
- **Database**: PostgreSQL (prod), H2 (dev/test/light prod), OpenJPA ORM
- **Frontend**: React 16, Redux, Angular 1.6 (legacy), Webpack, SCSS
- **Testing**: JUnit 4, Mockito, Selenium, Karma/Jasmine, Jest, React Testing Library
- **Security**: Apache Shiro, Keycloak SAML

### Key Configuration
- Main config files: `config.yml` (Dropwizard YAML)
- Development configs in `src/test/resources/config-*.yml`
- Database migrations in `insight-brain-db/src/main/resources/db/`

## Running the Application

### Server Deployment
Run from `insight-brain-service/` directory:
```bash
mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.service.InsightBrainService -Dexec.args='server src/test/resources/config-dev.yml'
```
Default credentials: admin/admin123

### Requirements
- Java 17
- Maven 3.9.x
- yarn (for frontend)
- Docker (for tests requiring external services)
- License file required on first launch

### Test Database Cleanup
Tests using IQ database must cleanup with `TemporaryEntity` rule:
```java
@Rule
public TemporaryEntity tempEntity = new TemporaryEntity();
```

## Development Notes
### Git Workflow
- Use descriptive branch names. The branch name should be prefixed with the Jira ticket ID. For example:
  `CLM-12345-some-meaningful-description`
- Keep commits focused and well-described. The commit title should be suffixed with the Jira ticket ID.
- When creating a PR in github, the first line in the PR description should be a link to the Jira ticket. For ex:
  Jira: https://sonatype.atlassian.net/browse/CLM-12345

### Maven Build Cache
Use Maven 3.9.0+ with build cache extension for faster builds.
Clear cache: `rm -rf ~/.m2/build-cache/v1/`

### Feature Flags
New experimental features use `SystemConfigurationPropertyFeature` enum in database.

### External Dependencies
- **HDS (hosted-data-services)**: Provides vulnerability/license data
- **insight-scanner**: Component scanning library
- **react-shared-components**: Shared React component library

### Code Quality
- Checkstyle and PMD are enforced
- License headers required (use `header.txt`)
- Git hooks configured in `githooks/` directory

### jakarta.inject Migration
The codebase has been migrated to `jakarta.inject` as part of the Jakarta EE 11 upgrade. Use `jakarta.inject` for all dependency injection.
Do not use `javax.inject` as it is no longer supported. Mixing javax and jakarta imports can cause runtime errors.
