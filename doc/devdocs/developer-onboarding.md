<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Developer Onboarding Guide

Welcome to the Nexus IQ Server (insight-brain) development team! This guide will help you get up and running with the codebase quickly and effectively.

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture & Key Components](#architecture--key-components)
- [Development Environment Setup](#development-environment-setup)
- [First Time Setup](#first-time-setup)
- [Building & Running](#building--running)
- [Development Workflow](#development-workflow)
- [Testing](#testing)
- [Key Resources](#key-resources)
- [Team Guidelines](#team-guidelines)
- [Common Tasks](#common-tasks)
- [Troubleshooting](#troubleshooting)

## Project Overview

**Nexus IQ Server** is the on-premises server that customers use to evaluate their applications against security and license policies. It's part of the **Nexus Lifecycle** product umbrella.

### What Does It Do?
- **Scans** projects and generates component hashes
- **Evaluates** known vulnerabilities and license information against user-configured policies
- **Generates** detailed application scan reports
- **Provides** a web UI for policy management and report viewing

### Related Projects
- [`hosted-data-services`](https://github.com/sonatype/hosted-data-services) (HDS) - Provides vulnerability and license data
- [`insight-dto-model`](https://github.com/sonatype/insight-dto-model) - Shared data objects
- [`insight-scanner`](https://github.com/sonatype/insight-scanner) - Component scanning functionality
- [`react-shared-components`](https://github.com/sonatype/sonatype-react-shared-components) - Shared React components

## Architecture & Key Components

The project is organized as a multi-module Maven project:

### Core Modules
- **`insight-brain-service`** - Main backend service and application entry point
- **`insight-brain-frontend`** - React-based web UI
- **`insight-brain-data`** - Data models and DTOs
- **`insight-brain-db`** - Database access layer and migrations
- **`insight-brain-common`** - Shared utilities and common code

### Supporting Modules
- **`insight-brain-client`** - Client libraries for API access
- **`insight-brain-policy`** - Policy evaluation engine
- **`insight-brain-event`** - Event handling system
- **`insight-brain-tenancy`** - Multi-tenancy support
- **`keycloak-server`** - Authentication and authorization
- **`nexus-iq-server`** - Server packaging and deployment

### External Services
- **`gitlab-server`** - GitLab integration
- **`hds-mock-server`** - Mock HDS for testing

## Development Environment Setup

### Prerequisites

1. **Java 17**
   ```bash
   # Check your Java version
   java -version
   ```

2. **Maven 3.9.x**
   ```bash
   # Check your Maven version
   mvn -version
   ```

3. **Node.js & yarn** (for frontend development)
   - Check the `node.version` property in `insight-brain-frontend/pom.xml` for the exact version
   - Install yarn: `npm install -g yarn@<version>`

4. **Docker** (required for some tests)
   - [Install Docker Engine](https://docs.docker.com/install/)

### Repository Access Configuration

Configure Maven and npm to use Sonatype's internal repository:

- [**Maven** setup instructions](https://sonatype.atlassian.net/wiki/spaces/CDI/pages/1921712289/Setting+up+maven+to+use+sonatype.repo.sonatype.app)
- [**npm** setup instructions](https://sonatype.atlassian.net/wiki/spaces/CDI/pages/1947467777/Setting+up+npm+to+use+sonatype.repo.sonatype.app)

## First Time Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd insight-brain
```

### 2. Initial Build
```bash
# Quick build (skips tests - faster for first setup)
mvn clean install -Pquick

# Full build (includes all tests - takes longer)
mvn clean install
```

## Building & Running

### Quick Development Build
```bash
# Build without tests for faster iteration
mvn clean install -Pquick
```

### Running the Application

1. **Start the backend server:**
   ```bash
   cd insight-brain-service
   mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.spring.InsightBrainSpringApplication -Dexec.args='server src/test/resources/config-dev.yml' -Ddw.server.applicationConnectors[0].port=8072
   ```

2. **Access the application:**
   - URL: http://localhost:8072
   - Default credentials:
     - Username: `admin`
     - Password: `admin123`

3. **First-time setup:**
   - You'll need to add a license file on first launch
   - Download from [the product licensing page](https://sonatype.atlassian.net/wiki/spaces/ProdMgmt/pages/43516041/Product+Licensing)
   - Use: `[year]-sonatype-internal-lcc-lfc-1000apps-1000rm_users-1000lc_users-1000fw_users.lic`

### Frontend Development

For frontend development, you'll want to run the frontend in development mode:

```bash
cd insight-brain-frontend
# Follow the detailed instructions in insight-brain-frontend/README.md
```

### Performance Tips

**Maven Build Cache** (Maven 3.9.0+):
```bash
# Clear build cache
rm -rf ~/.m2/build-cache/v1/

# Build without cache
mvn clean install -Dmaven.build.cache.skipCache=true
```

**Maven Daemon** (faster builds):
```bash
# Install mvnd: https://github.com/apache/maven-mvnd#how-to-install-mvnd
# Use mvnd instead of mvn for faster builds
mvnd clean install -Pquick
```

## Development Workflow

### Before You Start Coding

1. **Talk to people first!** Don't start with code and a pull request
2. Reach out to a Product Manager and Engineer
3. Small, focused contributions are best
4. Read our [contributing guidelines](../../contributing.md)

### Code Changes

1. Create a feature branch
2. Make your changes
3. Run tests locally
4. Follow our coding standards
5. Submit a pull request

### Code Review Process

- All changes require review
- Reviews focus on code quality, maintainability, and adherence to standards
- Be prepared to iterate based on feedback

## Testing

### Running Tests

```bash
# Fast tests only (< 100ms per test)
mvn test -DexcludedGroups=SlowTest

# Skip functional tests (faster)
mvn test -Dskip-functional-test

# All tests (slow - includes Docker-based integration tests)
mvn test

# Enable browser selection for functional tests
mvn test -Dbrowser=firefox  # or chrome

# Enable slow motion for debugging timing issues
mvn test -Dslowmo.delay=500
```

### Test Requirements

- Tests using the IQ database must clean up after themselves
- Use `TemporaryEntity` rule in test classes:
  ```java
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();
  ```
- Enable leak detection locally: `-DdetectTestEntityLeaks`

### Frontend Testing

```bash
cd insight-brain-frontend
# See insight-brain-frontend/README.md for detailed frontend testing instructions
```

## Key Resources

### Documentation
- **[Main README](../../README.md)** - Project overview and basic setup
- **[API Guidelines](./api-guide-lines.md)** - REST API development standards
- **[Contributing Guide](../../contributing.md)** - Detailed contribution process
- **[DevDocs](../devdocs/)** - Technical documentation for developers
- **[ADRs](../adr/)** - Architectural Decision Records

### Code Standards
- **[JavaScript Coding Standards](../../JS-Coding-Standards.md)** - Frontend code conventions
- **[Code of Conduct](../../code-of-conduct.md)** - Team behavior expectations

### Configuration
- **Development config:** `insight-brain-service/src/test/resources/config-dev.yml`
- **License requirements:** Required for application functionality
- **Port configuration:** Default 8070, configurable via system properties

## Team Guidelines

### Communication
- Team Insight is the primary maintainer
- Reach out via Slack: `@iq-em` for engineering management
- Contact individuals: `@Nicholas Blair`, `@Tim Levett`, or `@Brandon Sedgwick`

### Contribution Expectations
- **Discovery first** - Talk before coding
- **Small focused changes** - Easier to review and merge
- **See it through** - Be prepared to support your changes through to completion
- **Follow the process** - Use our established workflow

### Code Quality
- Follow established patterns in the codebase
- Write tests for your changes
- Document complex logic
- Consider performance implications

## Common Tasks

### Adding New Configuration
- **Simple config:** Add to `ConfigurationProperty.PROPERTIES` array
- **Complex config:** Create dedicated table/DAO/service/resource
- Implement `ConfigurationListener` for cluster-aware changes

### Feature Flags
- Add to `SystemConfigurationPropertyFeature` enum
- Set `enabledWhenAbsent` appropriately for your use case
- Experimental features typically start with `enabledWhenAbsent = false`

### Database Changes
- Create migration scripts in `insight-brain-db/src/main/resources/db/`
- Follow existing patterns for schema changes
- Test with various database types

### Working with Customer Support
- Import database dumps using `DbImportFromSupportZip.java`
- Located in `insight-brain-service/src/test/java/com/sonatype/insight/brain/support/`

## Troubleshooting

### Build Issues
- **Maven repository access:** Ensure repo.s.c credentials are configured
- **Node/npm issues:** Check version compatibility in frontend pom.xml
- **Docker tests failing:** Ensure Docker is running and accessible
- **Slow builds:** Use `-Pquick` profile or Maven daemon

### Runtime Issues
- **Port conflicts:** Use `-Ddw.server.applicationConnectors[0].port=<port>` to change port
- **Database issues:** Check configuration in config-dev.yml
- **License errors:** Ensure valid license file is installed

### Frontend Issues
- **Build failures:** Check Node.js and yarn versions
- **M1/ARM64 issues:** Run `npm install node-sass@npm:sass` in frontend directory

### Getting Help
- Check existing [DevDocs](../devdocs/) for specific topics
- Search through [ADRs](../adr/) for architectural context
- Reach out to team members via Slack
- Create a DevDoc if you discover something others should know!

## Next Steps

Now that you're set up:

1. **Explore the codebase** - Start with the main service and frontend modules
2. **Read relevant DevDocs** - Check out topics related to your work area
3. **Join team discussions** - Participate in architectural and design conversations
4. **Make your first contribution** - Start small and follow the process
5. **Share your knowledge** - Create DevDocs for insights you gain along the way

Welcome to the team! 🎉

---

*This document is part of our living documentation. If you find anything outdated or missing, please update it or let the team know.*