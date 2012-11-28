/*******************************************************************************
 * Copyright (c) 2012  Ola Spjuth <ola.spjuth@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.openphacts.business;

import java.util.List;

import net.bioclipse.core.PublishedClass;
import net.bioclipse.core.PublishedMethod;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.managers.business.IBioclipseManager;
import net.bioclipse.openphacts.model.CWResult;

/**
 * 
 * @author ola
 *
 */
@PublishedClass(
    value="Manager to access pharmacological data from the OpenPHACTS" +
    		" infrastructure.",
    doi="10.1016/j.drudis.2012.05.016"
)
public interface IOpenphactsManager extends IBioclipseManager {

	@PublishedMethod(methodSummary = "Set conceptwiki endpoint")
	public void setConceptWikiEndpoint(String endpoint);
	@PublishedMethod(methodSummary = "Set Open PHACTS LDA endpoint")
	public void setOPSLDAendpoint(String endpoint);
	
	@PublishedMethod(
			methodSummary = "Look up proteins in ConceptWiki by name",
			params = "String name")
	public List<CWResult> lookUpProteins(String name)
			throws BioclipseException;
	
	@PublishedMethod(
			methodSummary = "Look up compounds in ConceptWiki by name",
			params = "String name")
	public List<CWResult> lookUpCompounds(String name)
			throws BioclipseException;

	@PublishedMethod(
			methodSummary = "Get info for chemical structures from Open PHACTS" +
					", including pharmacological data")
	public List<IMolecule> getCompoundsInfo(List<CWResult> collection) 
			throws BioclipseException;
	
	@PublishedMethod(
			methodSummary = "Get info for proteins from Open PHACTS")
	public List<String> getProteinsInfo(List<CWResult> collection)
			throws BioclipseException;

}
