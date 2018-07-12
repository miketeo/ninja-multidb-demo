/*
 * Some portions of this class are copied from Google Guice
 * https://github.com/google/guice/blob/master/extensions/persist/src/com/google/inject/persist/jpa/JpaLocalTxnInterceptor.java
 * under the following license.
 *
 * Copyright (C) 2010 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package multidbsources;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class LocalTxnInterceptor implements MethodInterceptor {

	@Inject
	private MultiDBSources multiDBSources;

	@Transactional
	private static class Internal {}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (multiDBSources.hasTransaction()) {
			return methodInvocation.proceed();
		}
		multiDBSources.beginTransaction();

		Transactional transactional = readTransactionMetadata(methodInvocation);
		Object result;
		try {
			result = methodInvocation.proceed();
		} catch (Exception e) {
			multiDBSources.endTransaction(transactional, e);

			//propagate whatever exception is thrown anyway
			throw e;
		}

		multiDBSources.endTransaction(transactional);

		//or return result
		return result;
	}

	// Copied from Google Guice
	private Transactional readTransactionMetadata(MethodInvocation methodInvocation) {
		Transactional transactional;
		Method method = methodInvocation.getMethod();
		Class<?> targetClass = methodInvocation.getThis().getClass();

		transactional = method.getAnnotation(Transactional.class);
		if (null == transactional) {
			// If none on method, try the class.
			transactional = targetClass.getAnnotation(Transactional.class);
		}
		if (null == transactional) {
			// If there is no transactional annotation present, use the default
			transactional = Internal.class.getAnnotation(Transactional.class);
		}

		return transactional;
	}

}
