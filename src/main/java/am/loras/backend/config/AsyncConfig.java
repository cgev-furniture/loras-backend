package am.loras.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Dedicated thread pool for fire-and-forget async tasks (primarily mail sending).
     * Keeps mail I/O off the request thread while limiting resource consumption.
     *
     * <ul>
     *   <li>Core threads: 2 — always alive, handles typical low-traffic load.</li>
     *   <li>Max threads: 5 — burst capacity for concurrent inquiries.</li>
     *   <li>Queue capacity: 50 — buffers tasks during short spikes before rejection.</li>
     * </ul>
     */
    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}
