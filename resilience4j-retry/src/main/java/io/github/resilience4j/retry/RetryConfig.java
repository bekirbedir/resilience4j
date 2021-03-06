/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.retry;


import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import io.github.resilience4j.core.lang.Nullable;

public class RetryConfig {

	public static final int DEFAULT_MAX_ATTEMPTS = 3;
	public static final long DEFAULT_WAIT_DURATION = 500;
	public static final IntervalFunction DEFAULT_INTERVAL_FUNCTION = (numOfAttempts) -> DEFAULT_WAIT_DURATION;
	public static final Predicate<Throwable> DEFAULT_RECORD_FAILURE_PREDICATE = (throwable) -> true;

	private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
	@Nullable
	private Predicate resultPredicate;
	private IntervalFunction intervalFunction = DEFAULT_INTERVAL_FUNCTION;
	// The default exception predicate retries all exceptions.
	private Predicate<Throwable> exceptionPredicate = DEFAULT_RECORD_FAILURE_PREDICATE;

	private RetryConfig() {
	}

	/**
	 * @return the maximum allowed retries to make.
	 */
	public int getMaxAttempts() {
		return maxAttempts;
	}

	public Function<Integer, Long> getIntervalFunction() {
		return intervalFunction;
	}

	public Predicate<Throwable> getExceptionPredicate() {
		return exceptionPredicate;
	}

	/**
	 * Return the Predicate which evaluates if an result should be retried.
	 * The Predicate must return true if the result should  be retried, otherwise it must return false.
	 *
	 * @param <T> The type of result.
	 * @return the resultPredicate
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> Predicate<T> getResultPredicate() {
		return resultPredicate;
	}

	/**
	 * Returns a builder to create a custom RetryConfig.
	 *
	 * @param <T> The type being built.
	 * @return a {@link Builder}
	 */
	public static <T> Builder<T> custom() {
		return new Builder<>();
	}


	public static <T> Builder<T> from(RetryConfig baseConfig) {
		return new Builder<>(baseConfig);
	}

	/**
	 * Creates a default Retry configuration.
	 *
	 * @return a default Retry configuration.
	 */
	public static RetryConfig ofDefaults() {
		return new Builder().build();
	}

	public static class Builder<T> {
		@Nullable
		private Predicate<Throwable> retryExceptionPredicate;
		private Predicate<Throwable> exceptionPredicate = DEFAULT_RECORD_FAILURE_PREDICATE;
		@Nullable
		private Predicate<T> resultPredicate;
		private IntervalFunction intervalFunction = IntervalFunction.ofDefaults();
		@SuppressWarnings("unchecked")
		private Class<? extends Throwable>[] retryExceptions = new Class[0];
		@SuppressWarnings("unchecked")
		private Class<? extends Throwable>[] ignoreExceptions = new Class[0];
		private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
		// by default it is enabled
		private boolean enableRetryExceptionPredicate = true;

		public Builder() {
		}

		public Builder(RetryConfig retryConfig) {
			this.exceptionPredicate = retryConfig.exceptionPredicate;
			this.maxAttempts = retryConfig.maxAttempts;
			this.intervalFunction = retryConfig.intervalFunction;
			this.resultPredicate = retryConfig.resultPredicate;
		}

		public Builder<T> maxAttempts(int maxAttempts) {
			if (maxAttempts < 1) {
				throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
			}
			this.maxAttempts = maxAttempts;
			return this;
		}

		public Builder<T> waitDuration(Duration waitDuration) {
			if (waitDuration.toMillis() < 10) {
				throw new IllegalArgumentException("waitDurationInOpenState must be at least 10ms");
			}
			this.intervalFunction = (x) -> waitDuration.toMillis();
			return this;
		}

		/**
		 * Configures a Predicate which evaluates if an result should be retried.
		 * The Predicate must return true if the result should be retried, otherwise it must return false.
		 *
		 * @param predicate the Predicate which evaluates if an result should be retried or not.
		 * @return the RetryConfig.Builder
		 */
		public Builder<T> retryOnResult(Predicate<T> predicate) {
			this.resultPredicate = predicate;
			return this;
		}

		/**
		 * Set a function to modify the waiting interval
		 * after a failure. By default the interval stays
		 * the same.
		 *
		 * @param f Function to modify the interval after a failure
		 * @return the CircuitBreakerConfig.Builder
		 */
		public Builder<T> intervalFunction(IntervalFunction f) {
			this.intervalFunction = f;
			return this;
		}

		/**
		 * Configures a Predicate which evaluates if an exception should be retried.
		 * The Predicate must return true if the exception should count be retried, otherwise it must return false.
		 *
		 * @param predicate the Predicate which evaluates if an exception should be retried or not.
		 * @return the RetryConfig.Builder
		 */
		public Builder<T> retryOnException(Predicate<Throwable> predicate) {
			this.retryExceptionPredicate = predicate;
			return this;
		}


		/**
		 * it is ued when you merge shared configuration with Retry specific backend configuration to cover the following :
		 * 1- if retry specific configuration has no ignore or retry exception neither retry exception predicate , do not recalculate the predicate to avoid losing the shared one if any
		 * 2- otherwise do the calculation
		 *
		 * @param enableRetryExceptionPredicate enable or disable result predicate exception
		 * @return the RetryConfig.Builder
		 */
		public Builder<T> enableRetryExceptionPredicate(boolean enableRetryExceptionPredicate) {
			this.enableRetryExceptionPredicate = enableRetryExceptionPredicate;
			return this;
		}


		/**
		 * Configures a list of error classes that are recorded as a failure and thus increase the failure rate.
		 * Any exception matching or inheriting from one of the list will be retried, unless ignored via
		 *
		 * @param errorClasses the error classes that are retried
		 * @return the RetryConfig.Builder
		 * @see #ignoreExceptions(Class[]) ). Ignoring an exception has priority over retrying an exception.
		 * <p>
		 * Example:
		 * retryOnExceptions(Throwable.class) and ignoreExceptions(RuntimeException.class)
		 * would retry all Errors and checked Exceptions, and ignore unchecked
		 * <p>
		 * For a more sophisticated exception management use the
		 * @see #retryOnException(Predicate) method
		 */
		@SuppressWarnings("unchecked")
		@SafeVarargs
		public final Builder<T> retryExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
			this.retryExceptions = errorClasses != null ? errorClasses : new Class[0];
			return this;
		}

		/**
		 * Configures a list of error classes that are ignored and thus are not retried.
		 * Any exception matching or inheriting from one of the list will not be retried, even if marked via
		 *
		 * @param errorClasses the error classes that are retried
		 * @return the RetryConfig.Builder
		 * @see #retryExceptions(Class[]) . Ignoring an exception has priority over retrying an exception.
		 * <p>
		 * Example:
		 * ignoreExceptions(Throwable.class) and retryOnExceptions(Exception.class)
		 * would capture nothing
		 * <p>
		 * Example:
		 * ignoreExceptions(Exception.class) and retryOnExceptions(Throwable.class)
		 * would capture Errors
		 * <p>
		 * For a more sophisticated exception management use the
		 * @see #retryOnException(Predicate) method
		 */
		@SuppressWarnings("unchecked")
		@SafeVarargs
		public final Builder<T> ignoreExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
			this.ignoreExceptions = errorClasses != null ? errorClasses : new Class[0];
			return this;
		}

		public RetryConfig build() {
			if (enableRetryExceptionPredicate){
				buildExceptionPredicate();
			}
			RetryConfig config = new RetryConfig();
			config.intervalFunction = intervalFunction;
			config.maxAttempts = maxAttempts;
			config.exceptionPredicate = exceptionPredicate;
			config.resultPredicate = resultPredicate;
			return config;
		}

		private void buildExceptionPredicate() {
			this.exceptionPredicate =
					getRetryPredicate()
							.and(buildIgnoreExceptionsPredicate()
									.orElse(DEFAULT_RECORD_FAILURE_PREDICATE));
		}

		private Predicate<Throwable> getRetryPredicate() {
			return buildRetryExceptionsPredicate()
					.map(predicate -> retryExceptionPredicate != null ? predicate.or(retryExceptionPredicate) : predicate)
					.orElseGet(() -> retryExceptionPredicate != null ? retryExceptionPredicate : DEFAULT_RECORD_FAILURE_PREDICATE);
		}

		private Optional<Predicate<Throwable>> buildRetryExceptionsPredicate() {
			return Arrays.stream(retryExceptions)
					.distinct()
					.map(Builder::makePredicate)
					.reduce(Predicate::or);
		}

		private Optional<Predicate<Throwable>> buildIgnoreExceptionsPredicate() {
			return Arrays.stream(ignoreExceptions)
					.distinct()
					.map(Builder::makePredicate)
					.reduce(Predicate::or)
					.map(Predicate::negate);
		}

		static Predicate<Throwable> makePredicate(Class<? extends Throwable> exClass) {
			return (Throwable e) -> exClass.isAssignableFrom(e.getClass());
		}
	}
}
