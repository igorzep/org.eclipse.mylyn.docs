/*******************************************************************************
 * Copyright (c) 2007, 2008 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.ui.annotation;

import org.eclipse.jface.text.source.Annotation;

/**
 * An annotation that marks the location of an <code>a/@name</code>.
 * 
 * @author David Green
 * 
 */
public class AnchorNameAnnotation extends Annotation {

	public static final String TYPE = "org.eclipse.mylyn.wikitext.ui.annotation.anchorName";

	public AnchorNameAnnotation(String name) {
		super(TYPE, false, name);
	}

	public String getAnchorName() {
		return getText();
	}
}
