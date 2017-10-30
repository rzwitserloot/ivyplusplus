/**********************************************************************
 * Copyright (c) 2005-2009 ant4eclipse project team.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nils Hartmann, Daniel Kasmeroglu, Gerd Wuetherich
 **********************************************************************/

package com.zwitserloot.ivyplusplus.ecj;

import java.io.File;

/**
 * Implements utility methods to support design-by-contract. If a condition is
 * evaluated to false, a RuntimeException will be thrown.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 * @author Daniel Kasmeroglu (daniel.kasmeroglu@kasisoft.net)
 */
public class Assure {
	/**
	 * Assert that the specified object is not null.
	 * 
	 * @param parameterName The name of the parameter that is checked
	 * @param object The object that must be set.
	 */
	public static final void notNull(String parameterName, Object object) {
		if (object == null) throw new NullPointerException(parameterName);
	}
	
	/**
	 * Asserts that the specified array is neither {@code null} nor empty.
	 * 
	 * @param parametername The name of the parameter that has to be tested.
	 * @param object The object that has to be tested.
	 */
	public static final void nonEmpty(String parameterName, byte[] object) {
		notNull(parameterName, object);
		if (object.length == 0) throw new IllegalArgumentException("empty " + parameterName);
	}
	
	/**
	 * Asserts that the specified array is neither {@code null} nor empty.
	 * 
	 * @param parametername The name of the parameter that has to be tested.
	 * @param object The object that has to be tested.
	 */
	public static final void nonEmpty(String parameterName, boolean[] object) {
		notNull(parameterName, object);
		if (object.length == 0) throw new IllegalArgumentException("empty " + parameterName);
	}
	
	/**
	 * Asserts that the specified array is neither {@code null} nor empty.
	 * 
	 * @param parametername The name of the parameter that has to be tested.
	 * @param object The object that has to be tested.
	 */
	public static final void nonEmpty(String parameterName, char[] object) {
		notNull(parameterName, object);
		if (object.length == 0) throw new IllegalArgumentException("empty " + parameterName);
	}
	
	/**
	 * Asserts that the specified array is neither {@code null} nor empty.
	 * 
	 * @param parametername The name of the parameter that has to be tested.
	 * @param object The object that has to be tested.
	 */
	public static final void nonEmpty(String parameterName, short[] object) {
		notNull(parameterName, object);
		if (object.length == 0) throw new IllegalArgumentException("empty " + parameterName);
	}
	
	/**
	 * Asserts that the specified array is neither <code>null</code> nor empty.
	 * Asserts that the specified array is neither {@code null} nor empty.
	 * 
	 * @param parametername The name of the parameter that has to be tested.
	 * @param object The object that has to be tested.
	 */
	public static final void nonEmpty(String parameterName, int[] object) {
		notNull(parameterName, object);
		if (object.length == 0) throw new IllegalArgumentException("empty " + parameterName);
	}
	
	/**
	 * Asserts that the specified array is neither {@code null} nor empty.
	 * 
	 * @param parametername The name of the parameter that has to be tested.
	 * @param object The object that has to be tested.
	 */
	public static final void nonEmpty(String parameterName, long[] object) {
		notNull(parameterName, object);
		if (object.length == 0) throw new IllegalArgumentException("empty " + parameterName);
	}
	
	/**
	 * Asserts that the given parameter is an instance of the given type.
	 * 
	 * @param parameterName The name of the parameter that is checked.
	 * @param parameter The actual parameter value.
	 * @param expectedType The type the parameter should be an instance of.
	 */
	public static final void instanceOf(String parameterName, Object parameter, Class<?> expectedType) {
		notNull(parameterName, parameter);
		if (!expectedType.isInstance(parameter)) throw new IllegalArgumentException("Expected type " + expectedType.getSimpleName() + " for " + parameterName);
	}
	
	/**
	 * Assert that the supplied string isn't empty.
	 * 
	 * @param string The string that must not be empty.
	 */
	public static final void nonEmpty(String parameterName, String string) {
		notNull(parameterName, string);
		if (string.length() == 0) throw new IllegalArgumentException("empty " + parameterName);
	}
	
	/**
	 * Assert that the specified file is not null and exists.
	 * 
	 * @param file The file that must exist.
	 */
	public static final void exists(String parameterName, File file) {
		notNull(parameterName, file);
		if (!file.exists()) throw new IllegalArgumentException(parameterName + " does not exist");
	}
	
	/**
	 * Assert that the specified file is not null, exists and is a file.
	 * 
	 * @param file The file that must be a file.
	 */
	public static final void isFile(String parameterName, File file) {
		Assure.exists(parameterName, file);
		if (!file.isFile()) throw new IllegalArgumentException(parameterName + " not a file");
	}
	
	/**
	 * Assert that the specified file is not null, exists and is a directory.
	 * 
	 * @param file The file that must be a directory.
	 */
	public static final void isDirectory(String parameterName, File file) {
		Assure.exists(parameterName, file);
		if (!file.isDirectory()) throw new IllegalArgumentException(parameterName + " not a directory");
	}
	
	public static final void assertTrue(boolean condition, String msg) {
		if (!condition) throw new IllegalStateException("precondition not met: " + msg);
	}
	
	/**
	 * Checks whether a value is in a specific range or not.
	 * 
	 * @param value The value that shall be tested.
	 * @param from The lower bound inclusive.
	 * @param to The upper bound inclusive.
	 */
	public static final void inRange(int value, int from, int to) {
		if ((value < from) || (value > to)) throw new IllegalStateException("Value expected to be in range [" + from + ", " + to + "], not " + value);
	}
}