/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.core.databinding.observable;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;

@SuppressWarnings("unchecked")
public class ObservableArrayHelper {

	public static <T> IObservableValue<T>[] newIObservableValueArray(int size) {
		return new IObservableValue[size];
	}

	public static <E> IObservableList<E>[] newIObservableListArray(int size) {
		return new IObservableList[size];
	}

	public static <E> IObservableSet<E>[] newIObservableSetArray(int size) {
		return new IObservableSet[size];
	}

	public static <K, V> IObservableMap<K, V>[] newIObservableMapArray(int size) {
		return new IObservableMap[size];
	}
}
