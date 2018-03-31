/*******************************************************************************
 * Copyright (c) 2011, 2012 Tom Seidel, Remus Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *
 * Contributors:
 *     Tom Seidel - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.htmltext.configuration;


/**
 * Whether to use HTML entities in the output. 
 * @author Tom Seidel <tom.seidel@remus-software.org>
 * @since 0.8
 * @noextend This class is not intended to be subclassed by clients.
 */
public class UseEntitiesConfiguration extends ConfigurationElement {

	public UseEntitiesConfiguration(boolean value) {
		super("entities", Boolean.valueOf(value));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylyn.internal.htmltext.configuration.ConfigurationElement#doGetDefaultValue()
	 */
	@Override
	protected Object doGetDefaultValue() {
		return Boolean.TRUE;
	}

}
