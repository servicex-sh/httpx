package org.mvnsearch.http.logging;

import org.jetbrains.annotations.PropertyKey;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Locale;
import java.util.ResourceBundle;

public class HttpxErrorCodeLogger implements Logger {
    private final Logger delegate;
    private static final String BUNDLE_FQN = "httpx.ErrorMessages";
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_FQN, new Locale("en", "US"));

    public HttpxErrorCodeLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public static String errorMessage(@PropertyKey(resourceBundle = BUNDLE_FQN) String key, Object... params) {
        if (RESOURCE_BUNDLE.containsKey(key)) {
            String value = RESOURCE_BUNDLE.getString(key);
            final FormattingTuple tuple = MessageFormatter.arrayFormat(value, params);
            return key + " - " + tuple.getMessage();
        } else {
            return MessageFormatter.arrayFormat(key, params).getMessage();
        }
    }

    private static String errorCodeFormat(String format) {
        if (RESOURCE_BUNDLE.containsKey(format)) {
            return format + " - " + RESOURCE_BUNDLE.getString(format);
        } else {
            return format;
        }
    }

    @Override
    public void trace(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.trace(errorCodeFormat(msg));
    }

    @Override
    public void trace(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.trace(errorCodeFormat(format));
    }


    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }


    @Override
    public void trace(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.trace(errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void trace(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.trace(errorCodeFormat(format), arguments);
    }

    @Override
    public void trace(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.trace(errorCodeFormat(msg), t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.trace(marker, errorCodeFormat(msg));
    }

    @Override
    public void trace(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.trace(marker, errorCodeFormat(format), arg);
    }

    @Override
    public void trace(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.trace(marker, errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void trace(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... argArray) {
        delegate.trace(marker, errorCodeFormat(format), argArray);
    }

    @Override
    public void trace(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.trace(marker, errorCodeFormat(msg), t);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.debug(errorCodeFormat(msg));
    }

    @Override
    public void debug(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.debug(errorCodeFormat(format), arg);
    }

    @Override
    public void debug(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.debug(errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void debug(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.debug(errorCodeFormat(format), arguments);
    }

    @Override
    public void debug(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.debug(errorCodeFormat(msg), t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.debug(marker, errorCodeFormat(msg));
    }

    @Override
    public void debug(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.debug(marker, errorCodeFormat(format), arg);
    }

    @Override
    public void debug(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.debug(marker, errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void debug(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.debug(marker, errorCodeFormat(format), arguments);
    }

    @Override
    public void debug(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.debug(marker, errorCodeFormat(msg), t);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.info(errorCodeFormat(msg));
    }

    @Override
    public void info(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.info(errorCodeFormat(format), arg);
    }

    @Override
    public void info(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.info(errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void info(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.info(errorCodeFormat(format), arguments);
    }

    @Override
    public void info(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.info(errorCodeFormat(msg), t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.info(marker, errorCodeFormat(msg));
    }

    @Override
    public void info(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.info(marker, errorCodeFormat(format), arg);
    }

    @Override
    public void info(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.info(marker, errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void info(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.info(marker, errorCodeFormat(format), arguments);
    }

    @Override
    public void info(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.info(marker, errorCodeFormat(msg), t);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.warn(errorCodeFormat(msg));
    }

    @Override
    public void warn(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.warn(errorCodeFormat(format), arg);
    }

    @Override
    public void warn(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.warn(errorCodeFormat(format), arguments);
    }

    @Override
    public void warn(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.warn(errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void warn(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.warn(errorCodeFormat(msg), t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.warn(marker, errorCodeFormat(msg));
    }

    @Override
    public void warn(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.warn(marker, errorCodeFormat(format), arg);
    }

    @Override
    public void warn(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.warn(marker, errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void warn(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.warn(marker, errorCodeFormat(format), arguments);
    }

    @Override
    public void warn(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.warn(marker, errorCodeFormat(msg), t);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.error(errorCodeFormat(msg));
    }

    @Override
    public void error(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.error(errorCodeFormat(format), arg);
    }

    @Override
    public void error(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.error(errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void error(@PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.error(errorCodeFormat(format), arguments);
    }

    @Override
    public void error(@PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.error(errorCodeFormat(msg), t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg) {
        delegate.error(marker, errorCodeFormat(msg));
    }

    @Override
    public void error(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg) {
        delegate.error(marker, errorCodeFormat(format), arg);
    }

    @Override
    public void error(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object arg1, Object arg2) {
        delegate.error(marker, errorCodeFormat(format), arg1, arg2);
    }

    @Override
    public void error(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String format, Object... arguments) {
        delegate.error(marker, errorCodeFormat(format), arguments);
    }

    @Override
    public void error(Marker marker, @PropertyKey(resourceBundle = BUNDLE_FQN) String msg, Throwable t) {
        delegate.error(marker, errorCodeFormat(msg), t);
    }
}
