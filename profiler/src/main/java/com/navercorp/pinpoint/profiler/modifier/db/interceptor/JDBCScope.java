package com.nhn.pinpoint.profiler.modifier.db.interceptor;

import com.nhn.pinpoint.profiler.util.DepthScope;

/**
 * @author emeroad
 */
@Deprecated
public class JDBCScope {
    public static final DepthScope SCOPE = new DepthScope("JDBCScope");
}