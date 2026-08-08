package com.srinivas.vaultpay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration — enables Spring Data JPA Auditing for the entire application.
 *
 * <p><b>@EnableJpaAuditing:</b>
 * This single annotation activates the entire auditing infrastructure:
 * <ul>
 *   <li>Registers {@code AuditingEntityListener} to listen for JPA lifecycle events</li>
 *   <li>Activates processing of @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy</li>
 *   <li>Connects our {@link AuditorAwareImpl} bean via {@code auditorAwareRef}</li>
 * </ul>
 *
 * <p><b>Why a separate config class instead of putting this on the main class?</b>
 * Single Responsibility Principle. The main application class starts the app.
 * This class configures JPA auditing. Each class has one job.
 *
 * <p><b>auditorAwareRef = "auditorAwareImpl":</b>
 * Tells Spring Data which bean to call when it needs the current auditor.
 * The string must match the @Component("auditorAwareImpl") name exactly.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig {
    // No beans needed here — @EnableJpaAuditing does all the heavy lifting
}
