package com.sandwich.koan.runner;

import static com.sandwich.koan.KoanConstants.EXPECTATION_LEFT_ARG;
import static com.sandwich.koan.KoanConstants.EXPECTED_LEFT;
import static com.sandwich.koan.KoanConstants.EXPECTED_RIGHT;
import static com.sandwich.koan.KoanConstants.__;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.sandwich.koan.KoanMethod;
import com.sandwich.koan.KoanResult;
import com.sandwich.koan.KoanResult.KoanResultBuilder;
import com.sandwich.koan.path.PathToEnlightenment;
import com.sandwich.koan.path.PathToEnlightenment.Path;
import com.sandwich.koan.ui.ConsolePresenter;
import com.sandwich.koan.ui.SuitePresenter;
import com.sandwich.util.OsSpecificHelper;

public class KoanSuiteRunner {

	SuitePresenter presenter = null;
	
	public static void main(final String... args) throws Throwable {
		SuiteRunnerFactory.getSuiteRunner(args).run();
	}

	void run() throws ClassNotFoundException, IOException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		KoanResult result = runKoans();
		getPresenter().displayResult(result);
	}
	
	SuitePresenter getPresenter(){
		if(presenter == null){
			presenter = new ConsolePresenter();
		}
		return presenter;
	}

	int getAllValuesSize(Map<Object, List<Method>> koans) {
		int size = 0;
		for (Entry<Object, List<Method>> entry : koans.entrySet()) {
			size += entry.getValue().size();
		}
		return size;
	}

	KoanResult runKoans()
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		Path path = getPathToEnlightenment();
		int successfull = 0;
		List<Class<?>> passingSuites = new ArrayList<Class<?>>();
		List<Class<?>> failingSuites = new ArrayList<Class<?>>();
		Throwable failure = null;
		Class<?> firstFailingSuite = null;
		KoanMethod firstFailingMethod = null;
		String level = null;
		for(Entry<String, Map<Object, List<KoanMethod>>> packages : path){
			for (Entry<Object, List<KoanMethod>> e : packages.getValue().entrySet()) {
				final Object suite = e.getKey();
				final List<KoanMethod> methods = e.getValue();
				boolean testsPassed = true;
				for (final KoanMethod koan : methods) {
					try {
						koan.getMethod().invoke(suite, (Object[]) null);
						successfull++;
					} catch (Throwable t) {
						testsPassed = false;
						while(t.getCause() != null){
							t = t.getCause();
						}
						if (failure == null) {
							failure = t;
							firstFailingSuite = suite.getClass();
							firstFailingMethod = koan;
							level = packages.getKey();
						}
					}
				}
				if(testsPassed){
					passingSuites.add(suite.getClass());
				}else{
					failingSuites.add(suite.getClass());
				}
			}
		}
		String message = failure instanceof AssertionError ? failure.getMessage() : null;
		if(message != null && message.contains(EXPECTED_LEFT+__+EXPECTED_RIGHT)){
			logExpectationOnWrongSideWarning(firstFailingSuite, firstFailingMethod.getMethod());
		}
		return new KoanResultBuilder().level(level)
			.numberPassing(successfull).totalNumberOfKoanMethods(path.getTotalNumberOfKoans())
			.failingCase(firstFailingSuite).failingMethod(firstFailingMethod)
			.message(message).lineNumber(
					OsSpecificHelper.getOriginalLineNumber(failure, firstFailingSuite))
			.passingCases(passingSuites).build();
	}

	/**
	 * permit forwarding by overriding classes
	 * @return
	 */
	public Path getPathToEnlightenment() {
		return PathToEnlightenment.getPathToEnlightment();
	}

	private void logExpectationOnWrongSideWarning(Class<?> firstFailingSuite,
			Method firstFailingMethod) {
		Logger.getLogger(KoanSuiteRunner.class.getSimpleName()).severe(
				new StringBuilder(
						firstFailingSuite.getSimpleName()).append(
						".").append(
						firstFailingMethod.getName()).append(
						" ").append(
						EXPECTATION_LEFT_ARG).toString());
	}
}
