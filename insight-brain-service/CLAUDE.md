# CLAUDE.md - Insight Brain Service

This file provides guidance to Claude Code (claude.ai/code) when working with the **insight-brain-service** module specifically.

## Module Overview

**insight-brain-service** is the main backend service module for Nexus IQ Server. It contains the core REST API, business logic, and service orchestration layer that powers all four Sonatype products: **Lifecycle**, **SBOM Manager**, **Firewall**, and **Developer**.

## Key Responsibilities

- **REST API Layer**: JAX-RS endpoints for all client interactions
- **Business Logic**: Service layer orchestrating data access, policy evaluation, and external integrations
- **Security**: Authentication, authorization, audit logging
- **Integration Layer**: HDS client, SCM integrations, external service connectors
- **Task Orchestration**: Scheduled jobs, background processing, async operations
- **Configuration Management**: Feature flags, system settings, multi-tenancy support

## Architecture

### Framework Stack
- **Dropwizard 5.x**: Main application framework with embedded Jetty
- **JAX-RS**: REST API endpoints with Jersey
- **Guice**: Dependency injection container
- **Sisu**: Classpath scanning for Guice
- **dropwizard-guicey**: Guice/Dropwizard integration
- **Apache Shiro**: Security framework for authentication/authorization
- **jOOQ**: Database access layer (via insight-brain-data)
- **Quartz**: Job scheduling for background tasks

### Main Entry Point
- **Main Class**: `com.sonatype.insight.brain.spring.InsightBrainSpringApplication`
- **Configuration**: Dropwizard YAML configuration (`config-dev.yml` for development)
- **Port**: Default 8070, configurable via `-Ddw.server.applicationConnectors[0].port=XXXX`

### Package Structure
Key packages under `com.sonatype.insight.brain`:

- **`api/`** - Public API constants and utilities
- **`audit/`** - Audit logging framework and filters
- **`hds/`** - HDS (Hosted Data Services) client integration
- **`security/`** - Security filters, authentication, authorization
- **`organization/`** - Application and organization management
- **`policy/`** - Policy evaluation and violation handling
- **`git/`** - SCM integration (GitHub, GitLab, Bitbucket, etc.)
- **`dashboard/`** - Dashboard data aggregation services
- **`firewall/`** - Firewall-specific functionality
- **`integration/`** - External system integrations
- **`landing/`** - UI serving and static content
- **`service/`** - Core service bootstrap and configuration

## Development Commands

### Building
Before running the service, you need to build it:
```bash
# Quick build (recommended for development) - skips tests, linting, and checks
mvn clean install -Pquick

# Full build with tests (slower)
mvn clean install

# Build only this module and its dependencies (from root directory)
mvn clean install -pl insight-brain-service -am
```

### Running Locally
From the `insight-brain-service/` directory:
```bash
# Standard development server
mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.spring.InsightBrainSpringApplication -Dexec.args='server src/test/resources/config-dev.yml'

# Custom port (e.g., 8072)
mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.spring.InsightBrainSpringApplication -Dexec.args='server src/test/resources/config-dev.yml' -Ddw.server.applicationConnectors[0].port=8072

# Using compiled JAR
java -jar target/insight-brain-service-*-SNAPSHOT-server.jar server src/test/resources/config-dev.yml
```

**Tip**: When iterating on frontend changes alongside functional tests, use the esbuild dev server mode instead of running this server directly — see the "Fast Frontend Development Loop with Functional Tests" section in the root `CLAUDE.md` or `insight-brain-frontend/CLAUDE.md`.

### Testing
```bash
# Run all service tests
mvn verify

# Run specific test class
mvn verify -Dtest=HdsClientTest -Dit.test=HdsClientTest

# Run integration tests (slow)
mvn verify

# Skip slow tests
mvn verify -DexcludedGroups=SlowTest
```

### Configuration Files
- **Development**: `src/test/resources/config-dev.yml`
- **MTIQ Development**: `src/test/resources/config-mtiq-dev.yml`
- **Test**: `src/test/resources/config-test.yml`

## Key Service Patterns

### REST Resource Pattern
```java
@Path("/api/v2/example")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Example", description = "Example resource operations")
@Timed
public class ExampleResource {
    @Inject
    private ExampleService service;
    
    @GET
    @Operation(
        description = "Retrieve all examples. " +
            "<p>" +
            "Permissions Required: View IQ Elements"
    )
    @ApiResponse(responseCode = "200", description = "List of examples")
    @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
    public List<ExampleDTO> getExamples() {
        return service.getExamples();
    }
}
```

### Service Layer Pattern
```java
@Named
@Singleton
public class ExampleService {
    @Inject
    private ExampleDAO dao;
    
    public void updateExample(String id, ExampleDTO dto) {
        try (TransactionContext tx = dao.createTransactionContext()) {
            tx.begin();
            // Business logic here
            tx.commit();
        }
    }
}
```

**⚠️ Thread Safety Warning**: `@Singleton` beans must be thread-safe and should not contain mutable instance state unless explicitly managing multi-tenancy concerns (e.g., using `TenantReference`). In a multi-tenant environment, singleton services are shared across all tenants and threads.

### HDS Integration Pattern
```java
@Named
@Singleton
public class ExampleHdsService {
    @Inject
    private HdsClient hdsClient;
    
    public ComponentDetails getComponentDetails(ComponentIdentifier id) {
        return hdsClient.get(ComponentDetails.class, "/rest/component/" + id);
    }
}
```

## Security Considerations

### Authentication
- **Default Credentials**: admin/admin123 (development only)
- **Production**: LDAP, SAML, or local user management
- **API Tokens**: Support for automation and CI/CD integration

### Authorization
- **Role-Based**: Admin, Developer, User roles
- **Organization-Scoped**: Multi-tenant permission model
- **Policy-Based**: Custom authorization logic via Shiro

### Audit Logging
All sensitive operations should use `@Audited` annotation with constants from `AuditEvent` enum:
```java
@POST
@Audited(AuditEvent.CREATE_APPLICATION)
public ApplicationDTO createApplication(ApplicationDTO app) {
    // Implementation
}
```

## Configuration Management

### Feature Flags
New features should use `SystemConfigurationPropertyFeature`:
```java
// In SystemConfigurationPropertyFeature enum
NEW_FEATURE(SystemConfigurationProperty.NEW_FEATURE, false);

// In service code
if (SystemConfigurationPropertyFeature.NEW_FEATURE.isEnabled()) {
    // New feature code
}
```

### Environment Variable Support
Feature flags support environment variable overrides:
```java
NEW_FEATURE(SystemConfigurationProperty.NEW_FEATURE, false) {
    @Override
    public boolean isEnabled(TransactionContext tx) {
        String envVar = System.getenv("NXIQ_ENABLE_NEW_FEATURE");
        return envVar == null ? super.isEnabled(tx) : Boolean.parseBoolean(envVar);
    }
}
```

## Database Integration

### TemporaryEntity Rule
Tests that modify database state must use cleanup:
```java
@Rule
public TemporaryEntity tempEntity = new TemporaryEntity();

@Test
public void testDatabaseOperation() {
    Application app = tempEntity.newApplication();
    // Test logic
}
```

### Transaction Management
Use `TransactionContext` for database operations:
```java
public void updateData(String id, ExampleDTO dto) {
    try (TransactionContext tx = exampleDAO.createTransactionContext()) {
        tx.begin();
        // Database operations
        Example entity = exampleDAO.findById(tx, id);
        entity.updateFromDto(dto);
        exampleDAO.update(tx, entity);
        tx.commit();
    }
}
```

## External Integrations

### HDS (Hosted Data Services)
- **Purpose**: Vulnerability data, license information, component intelligence
- **Client**: `HdsClient` with specialized subclasses (`FirewallAuditHdsClient`, etc.)
- **Configuration**: `hdsUrl` in config file

### SCM Integration
- **Supported**: GitHub, GitLab, Bitbucket, Azure DevOps
- **Features**: Pull request commenting, branch monitoring, policy evaluation
- **Services**: `SourceControlService`, `PullRequestCommentingService`

## Testing Guidelines

### Test Categories
- **Unit Tests**: Fast, isolated component testing with mocks
- **Integration Tests**: Database and external service integration
- **Functional Tests**: End-to-end API testing (separate module)

### Abstract Test Base Classes

Choose the appropriate base class for your test needs:

#### **Unit Testing**
```java
@RunWith(MockitoJUnitRunner.class)
public class ExampleServiceTest {
    @Mock
    private ExampleDAO mockDao;
    
    @InjectMocks
    private ExampleService service;
    
    @Test
    public void testExample() {
        when(mockDao.findById("123")).thenReturn(expectedEntity);
        // Test logic
    }
}
```

#### **Database Testing**
- **`AbstractDataTest`** - For testing data access layer components with database
  ```java
  public class ExampleDAOTest extends AbstractDataTest {
      @Test
      public void testDatabaseOperation() {
          // Database available via tempEntity
      }
  }
  ```

#### **Service Integration Testing**
- **`BrainInjectedTest`** - For testing services with dependency injection but without full server
  ```java
  public class ExampleServiceIntegrationTest extends BrainInjectedTest {
      @Inject
      private ExampleService service;
  }
  ```

- **`AbstractBaseIntegrationTest`** - For comprehensive integration tests with full application context
  ```java
  public class ExampleIntegrationTest extends AbstractBaseIntegrationTest {
      // Full application context available
  }
  ```

#### **REST API Testing**
- **`AbstractResourceTest`** - For end-to-end REST API testing from endpoint to database
  ```java
  public class ExampleResourceTest extends AbstractResourceTest {
      @Test
      public void testRestEndpoint() {
          // HTTP client available for REST calls
          // Database available via tempEntity
      }
  }
  ```

#### **Authorization Testing**
- **`AbstractResourceAuthzTest`** - For testing REST endpoint authorization logic
  ```java
  public class ExampleResourceAuthzTest extends AbstractResourceAuthzTest {
      @Test
      public void testUnauthorizedAccess() {
          // Test authorization scenarios
      }
  }
  ```

#### **Audit Testing**
- **`AbstractAuditTest`** - For testing audit logging functionality
  ```java
  public class ExampleAuditTest extends AbstractAuditTest {
      @Test
      public void testAuditLogging() {
          // Audit log output captured and verifiable
          assertAuditContains(AuditEvent.CREATE_APPLICATION);
      }
  }
  ```

#### **Multi-Tenant Testing**
- **`AbstractMultiTenantBaseIntegrationTest`** - For testing multi-tenant scenarios (nexus-mtiq-server module)
  ```java
  public class ExampleMultiTenantTest extends AbstractMultiTenantBaseIntegrationTest {
      @Test
      public void testTenantIsolation() {
          // Test data isolation between tenants
      }
  }
  ```

## Build and Deployment

### Dependencies
- **Core Dependencies**: insight-brain-data, insight-brain-policy, insight-brain-db
- **Runtime Dependencies**: PostgreSQL/H2, OpenSearch (optional)
- **External Dependencies**: HDS, license server

### Build Artifacts
- **JAR**: `insight-brain-service-*-SNAPSHOT-server.jar` (fat JAR with dependencies)

## Troubleshooting

### Common Issues
- **License Required**: First launch requires license file from product licensing page
- **Port Conflicts**: Default port 8070 may conflict with webpack-dev-server - run on 8072 instead when using webpack-dev-server
- **HDS Connectivity**: Check `hdsUrl` configuration and network access
- **Database Issues**: Ensure H2/PostgreSQL is accessible and configured correctly

### Debug Configuration
Enable debug logging in `config-dev.yml`:
```yaml
logging:
  level: DEBUG
  loggers:
    "com.sonatype.insight.brain": DEBUG
```

### Performance Monitoring
- **Metrics**: Dropwizard metrics available at `/metrics`
- **Health Checks**: Available at `/health`
- **Thread Dumps**: JVM diagnostics available

## Development Best Practices

### Code Organization
- **Resources**: Thin controllers, delegate to services
- **Services**: Business logic, transaction boundaries
- **DTOs**: Data transfer objects for API contracts

### Error Handling
```java
@GET
public ExampleDTO getExample(@PathParam("id") String id) {
    Example entity = dao.findById(id);
    if (entity == null) {
        throw new NotFoundException("Example not found: " + id);
    }
    return adapter.toDTO(entity);
}
```

### Dependency Injection
- Use `@Named` and `@Singleton` for services
- Use `@Inject` for dependencies
- Avoid field injection in favor of constructor injection

## Related Modules

- **insight-brain-data**: Data access layer and entities
- **insight-brain-frontend**: React UI components
- **insight-brain-policy**: Policy evaluation engine
- **insight-brain-db**: Database schema and migrations
- **nexus-iq-server**: Single-tenant server bundle
- **nexus-mtiq-server**: Multi-tenant server bundle

## Important Files

- **`InsightBrainService.java`**: Main application class
- **`config-dev.yml`**: Development configuration
- **`pom.xml`**: Maven dependencies and build configuration
- **`src/main/resources/`**: Configuration files and resources
- **`src/test/resources/`**: Test configurations and data

---

**Note**: This module contains the core business logic for Nexus IQ Server. Changes here affect all four products (Lifecycle, Firewall, SBOM Manager, Developer) and require careful testing across all deployment scenarios.