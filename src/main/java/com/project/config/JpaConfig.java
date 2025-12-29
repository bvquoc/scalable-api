package com.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration to enable automatic auditing.
 * This allows @CreatedDate and @LastModifiedDate annotations to work.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
