package com.zhiyan.redbookbackend.util;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在执行指定代码块期间将若干 Mapper 的日志级别临时降为 INFO，避免 MyBatis DEBUG SQL 刷屏；
 * 使用引用计数，支持并发与同 Mapper 嵌套调用。
 */
@Component
public class MapperSqlLogMute {

    public static final String NOTE_LIKE_MAPPER = "com.zhiyan.redbookbackend.mapper.NoteLikeMapper";
    public static final String LIKE_OUTBOX_MAPPER = "com.zhiyan.redbookbackend.mapper.LikeOutboxMapper";

    private final LoggingSystem loggingSystem = LoggingSystem.get(ClassUtils.getDefaultClassLoader());
    private final ConcurrentHashMap<String, AtomicInteger> refCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LogLevel> savedLevel = new ConcurrentHashMap<>();

    public void enter(String loggerName) {
        AtomicInteger ref = refCount.computeIfAbsent(loggerName, k -> new AtomicInteger());
        int n = ref.incrementAndGet();
        if (n == 1) {
            LogLevel eff = loggingSystem.getLoggerConfiguration(loggerName).getEffectiveLevel();
            savedLevel.put(loggerName, eff);
            loggingSystem.setLogLevel(loggerName, LogLevel.INFO);
        }
    }

    public void leave(String loggerName) {
        AtomicInteger ref = refCount.get(loggerName);
        if (ref == null) {
            return;
        }
        int n = ref.decrementAndGet();
        if (n == 0) {
            LogLevel prev = savedLevel.remove(loggerName);
            refCount.remove(loggerName);
            loggingSystem.setLogLevel(loggerName, prev);
        }
    }

    public void runMuted(Runnable action, String... loggerNames) {
        for (String name : loggerNames) {
            enter(name);
        }
        try {
            action.run();
        } finally {
            for (int i = loggerNames.length - 1; i >= 0; i--) {
                leave(loggerNames[i]);
            }
        }
    }
}
