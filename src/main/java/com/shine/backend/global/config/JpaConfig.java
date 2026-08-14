package com.shine.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** BaseTimeEntity의 @CreatedDate/@LastModifiedDate를 활성화한다. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
