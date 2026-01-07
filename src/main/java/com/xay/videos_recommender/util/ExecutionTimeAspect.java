package com.xay.videos_recommender.util;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect that measures and logs execution time for methods
 * annotated with {@link LogExecutionTime}.
 */
@Slf4j
@Aspect
@Component
class ExecutionTimeAspect {

    @Around("@annotation(LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long duration = System.currentTimeMillis() - startTime;
        String methodName = joinPoint.getSignature().toShortString();

        log.info("{} executed in {} ms", methodName, duration);

        return result;
    }
}

