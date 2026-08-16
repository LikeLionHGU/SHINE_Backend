package com.shine.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 검사지 분석은 10~30초가 걸린다.
 * 요청 스레드에서 처리하면 그동안 톰캣 스레드가 묶이므로 별도 풀에서 돌린다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        // 큐가 꽉 차면 호출한 스레드가 대신 실행한다. 작업을 버리지 않는다.
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
