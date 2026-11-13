package com.example.myshopapp.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 定时任务多实例锁：多个后端实例同时部署时，同一任务在锁有效期内只会被一个实例执行。
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("shedlock")
                // 使用数据库时间，避免多台机器时钟不一致导致锁失效
                .usingDbTime()
                .build());
    }
}
