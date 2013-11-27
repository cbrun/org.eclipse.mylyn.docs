/*******************************************************************************
 * Copyright (c) 2013 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.core.osgi;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class OsgiServiceLocatorTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void create() {
		assertNotNull(new OsgiServiceLocator());
	}

	@Test
	public void getAllMarkupLanguages() {
		OsgiServiceLocator serviceLocator = createOsgiServiceLocator(createBundleWithLanguage(MockMarkupLanguage.class));
		Set<MarkupLanguage> languages = serviceLocator.getAllMarkupLanguages();
		assertNotNull(languages);
		assertTrue(!languages.isEmpty());
		assertMarkupLanguagePresent("MockMarkupLanguage", languages);
	}

	@Test
	public void getApplicableInstance() {
		assertNotNull(OsgiServiceLocator.getApplicableInstance());
	}

	private Bundle createBundleWithLanguage(Class<? extends MarkupLanguage> markupLanguage) {
		Bundle bundle = mock(Bundle.class);
		try {
			URL url = new URL("file:" + markupLanguage.getName());
			List<URL> resources = Lists.newArrayList(url);
			doReturn(Collections.enumeration(resources)).when(bundle).getResources(
					"META-INF/services/" + MarkupLanguage.class.getName());
			doReturn(1234L).when(bundle).getBundleId();
			doReturn(markupLanguage).when(bundle).loadClass(eq(markupLanguage.getName()));
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
		return bundle;
	}

	private OsgiServiceLocator createOsgiServiceLocator(Bundle... bundles) {
		final BundleContext context = mock(BundleContext.class);
		doReturn(bundles).when(context).getBundles();
		return new OsgiServiceLocator() {
			@Override
			BundleContext getContext() {
				return context;
			}

			@Override
			protected List<String> readServiceClassNames(URL url) {
				List<String> names = Lists.newArrayList();
				names.add(url.getPath());
				return names;
			}
		};
	}

	private void assertMarkupLanguagePresent(String name, Set<MarkupLanguage> languages) {
		for (MarkupLanguage language : languages) {
			if (language.getName().equals(name)) {
				return;
			}
		}
		fail(format("Language {0} expected but not found in {1}", name, languages));
	}

}