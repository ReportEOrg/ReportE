package org.reportbay.common.interceptor;

import org.slf4j.Logger;

public interface LogInterceptable<T extends JPAEntity> {
	Logger getLogger();
}
