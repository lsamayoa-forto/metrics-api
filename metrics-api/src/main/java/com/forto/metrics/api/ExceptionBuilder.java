package com.forto.metrics.api;

import java.util.Arrays;

public final class ExceptionBuilder {

    public static String formatException(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(ex.getClass().getName());
            sb.append(": ");
            sb.append(ex.getMessage());
            sb.append("\n");
            sb.append(Arrays.stream(ex.getStackTrace()).map(stackTraceElement -> stackTraceElement.getClassName() + stackTraceElement.getMethodName() + "(" + stackTraceElement.getLineNumber() + ") " + stackTraceElement.getFileName()));
            sb.append("\n");
        }
        while ((ex = ex.getCause()) != null);

        return sb.toString();
    }
}
