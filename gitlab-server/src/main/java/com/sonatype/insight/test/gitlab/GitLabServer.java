/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test.gitlab;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.docker.utils.DockerUtils;
import com.sonatype.insight.test.networking.PortAllocator;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Project;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A test utility class that creates and manages a GitLab server instance in a Docker container.
 *
 * The class automatically:
 * - Creates a GitLab Docker container
 * - Configures the GitLab instance with custom settings
 * - Creates a personal access token for the root (admin) user
 * - Creates a project in the GitLab instance
 * - Provides helper methods to access its GitLab API
 *
 * Usage example:
 *
 * <pre>
 * try (GitLabServer gitLabServer = new GitLabServer("test-project")) {
 *   GitLabApi gitLabApi = gitLabServer.getGitLabApi();
 *   // Use GitLab API to interact with the server
 * }
 * </pre>
 *
 * The class implements {@link AutoCloseable} to properly stop the container when it's no longer needed.
 */
public class GitLabServer
    implements AutoCloseable
{
  private static final Logger log = LoggerFactory.getLogger(GitLabServer.class);

  // Available docker images at https://hub.docker.com/r/gitlab/gitlab-ce/tags
  public static final String DEFAULT_IMAGE_NAME = "gitlab/gitlab-ce";

  public static final String DEFAULT_IMAGE_VERSION = "17.11.2-ce.0";

  public static final String DEFAULT_ADMIN_USERNAME = "root";

  public static final String DEFAULT_ADMIN_PASSWORD = UUID.randomUUID().toString();

  public static final String DEFAULT_ADMIN_TOKEN = UUID.randomUUID().toString();

  public static final int DEFAULT_HTTP_PORT = 80;

  public static final int DEFAULT_SSH_PORT = 22;

  private final GenericContainer<?> container;

  private final String image;

  private final String networkAlias;

  private final String containerId;

  private final String containerName;

  private final Integer httpPort;

  private final Integer sshPort;

  private final String hostname;

  private final String adminUsername;

  private final String adminPassword;

  private final String adminToken;

  private final Project project;

  private final GitLabApi gitLabApi;

  public GitLabServer() {
    this(null);
  }

  public GitLabServer(final String projectName) {
    this(null, DEFAULT_IMAGE_NAME, DEFAULT_IMAGE_VERSION, projectName);
  }

  public GitLabServer(
      final Network network,
      final String imageName,
      final String imageVersion,
      final String projectName)
  {
    DockerUtils.assumeSupported();
    try {
      adminUsername = DEFAULT_ADMIN_USERNAME;
      adminPassword = DEFAULT_ADMIN_PASSWORD;
      adminToken = DEFAULT_ADMIN_TOKEN;
      image = imageName + ":" + imageVersion;

      httpPort = PortAllocator.nextFreePort();
      sshPort = PortAllocator.nextFreePort();
      PortBinding httpPortBinding = PortBinding.parse(httpPort + ":" + httpPort);
      PortBinding sshPortBinding = PortBinding.parse(sshPort + ":" + DEFAULT_SSH_PORT);
      container = new GenericContainer<>(DockerImageName.parse(DockerUtils.applyRegistry(image)));
      container.withCreateContainerCmdModifier(cmd -> cmd
          .withHostConfig(HostConfig.newHostConfig()
              .withPortBindings(httpPortBinding, sshPortBinding))
          .withHostName("127.0.0.1")
          .withExposedPorts(ExposedPort.tcp(httpPort), ExposedPort.tcp(DEFAULT_SSH_PORT)));
      container.addExposedPorts(httpPort, DEFAULT_SSH_PORT);
      // https://docs.gitlab.com/administration/monitoring/health_check/#readiness
      container.waitingFor(
          Wait.forHealthcheck()
              .withRateLimiter(
                  RateLimiterBuilder.newBuilder()
                      .withRate(1, TimeUnit.SECONDS)
                      .withConstantThroughput()
                      .build()));
      container.withStartupTimeout(Duration.ofMinutes(5));
      container.addEnv("GITLAB_ROOT_PASSWORD", adminPassword);
      // https://forum.gitlab.com/t/gitlab-docker-not-working-if-external-url-is-set/4110/6
      // https://docs.gitlab.com/omnibus/settings/memory_constrained_envs/
      // https://www.reddit.com/r/gitlab/comments/10m0hxa/gitlab_container_image_without_extra_applications/
      List<String> options = List.of(
          String.format("external_url 'http://127.0.0.1:%s'", httpPort),
          "gitlab_rails['monitoring_whitelist'] = ['0.0.0.0/0']",
          // Configure puma and sidekiq
          "puma['worker_processes'] = 0",
          "sidekiq['concurrency'] = 10",
          // Disable unneeded services
          "alertmanager['enable'] = false",
          "gitlab_kas['enable'] = false",
          "gitlab_pages['enable'] = false",
          "gitlab_rails['smtp_enable'] = false",
          "letsencrypt['enable'] = false",
          "mattermost['enable'] = false",
          "mattermost_nginx['enable'] = false",
          "monitoring_role['enable'] = false",
          "prometheus['enable'] = false",
          "prometheus_monitoring['enable'] = false",
          "registry['enable'] = false",
          // nginx
          "nginx['listen_https'] = false",
          "nginx['redirect_http_to_https'] = false",
          "nginx['status'] = { 'enable' => false }",
          // Disable exporters
          "gitlab_exporter['enable'] = false",
          "node_exporter['enable'] = false",
          "pgbouncer_exporter['enable'] = false",
          "postgres_exporter['enable'] = false",
          "puma['exporter_enabled'] = false",
          "redis_exporter['enable'] = false");
      container.addEnv("GITLAB_OMNIBUS_CONFIG", String.join("; ", options));
      container.addEnv("TZ", ZoneId.systemDefault().getId());

      if (network != null) {
        container.setNetwork(network);
      }

      networkAlias = "gitlab-" + UUID.randomUUID().toString().substring(0, 8);
      container.setNetworkAliases(Collections.singletonList(networkAlias));

      container.start();
      container.followOutput(new Slf4jLogConsumer(log).withSeparateOutputStreams());

      containerId = container.getContainerId();
      containerName = container.getContainerName();
      hostname = DockerUtils.getHostname(container.getHost(), httpPort);

      createRootToken();
      String finalProjectName =
          projectName != null ? projectName : "repo-" + UUID.randomUUID().toString().substring(0, 8);
      gitLabApi = new GitLabApi(getBaseUrl(), getAdminToken());
      project = gitLabApi.getProjectApi().createProject(finalProjectName);

      log.info("Started GitLab Server {}.", this);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new IllegalStateException("Could not start GitLab docker container.", e);
    }
  }

  private void createRootToken() {
    try {
      // https://gitlab.com/gitlab-org/gitlab/-/blob/master/lib/gitlab/auth.rb
      List<String> scopes =
          List.of("api", "read_api", "read_user", "read_repository", "write_repository", "sudo", "admin_mode");
      String scopesString = String.join("', '", scopes);
      // https://docs.gitlab.com/user/profile/personal_access_tokens/#create-a-personal-access-token-programmatically
      String createTokenCommand = "user=User.find_by_username('root');" +
          String.format(
              "token=user.personal_access_tokens.create(scopes: ['%s'], name: 'token', expires_at: 365.days.from_now);",
              scopesString)
          +
          String.format("token.set_token('%s');", adminToken) +
          "token.save!";
      ExecResult result = container.execInContainer("gitlab-rails", "runner", createTokenCommand);
      if (result.getExitCode() > 0) {
        String error = "Failed to create root personal access token, exit code: " + result.getExitCode();
        log.error(error);
        if (!result.getStdout().isEmpty()) {
          log.error("Stdout: " + result.getStdout());
        }
        if (!result.getStderr().isEmpty()) {
          log.error("Stderr: " + result.getStderr());
        }
        throw new RuntimeException(error);
      }
    }
    catch (Exception e) {
      log.error("Failed to create root token", e);
      throw new IllegalStateException("Could not create root token", e);
    }
  }

  public GenericContainer<?> getContainer() {
    return container;
  }

  public String getImage() {
    return image;
  }

  public String getNetworkAlias() {
    return networkAlias;
  }

  public String getContainerId() {
    return containerId;
  }

  public String getContainerName() {
    return containerName;
  }

  public Integer getHttpPort() {
    return httpPort;
  }

  public Integer getSshPort() {
    return sshPort;
  }

  public String getHostname() {
    return hostname;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public String getAdminToken() {
    return adminToken;
  }

  public Project getProject() {
    return project;
  }

  public String getBaseUrl() {
    return "http://" + getHostname() + ":" + getHttpPort();
  }

  public GitLabApi getGitLabApi() {
    return gitLabApi;
  }

  @Override
  public void close() {
    if (container != null) {
      container.stop();
    }
  }

  @Override
  public String toString() {
    return "GitLabServer{" +
        "container=" + container +
        ", image='" + image + '\'' +
        ", networkAlias='" + networkAlias + '\'' +
        ", containerId='" + containerId + '\'' +
        ", containerName='" + containerName + '\'' +
        ", httpPort=" + httpPort +
        ", sshPort=" + sshPort +
        ", hostname='" + hostname + '\'' +
        ", adminUsername='" + adminUsername + '\'' +
        ", adminPassword='" + adminPassword + '\'' +
        ", adminToken='" + adminToken + '\'' +
        ", project=" + project +
        ", gitLabApi=" + gitLabApi +
        '}';
  }
}
