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
					", including pharmacological data",
			params = "List<CWResult> collection"
	)
	public List<IMolecule> getCompoundsInfo(List<CWResult> collection) 
			throws BioclipseException;
	
	@PublishedMethod(
			methodSummary = "Get info for proteins from Open PHACTS",
			params = "List<CWResult> collection"
	)
	public List<String> getProteinsInfo(List<CWResult> collection)
			throws BioclipseException;

	@PublishedMethod(
		methodSummary="Return other URIs the given URI maps to.",
		params="String URI"
	)
	public List<String> mapURI(String URI) throws BioclipseException;

	@PublishedMethod(
		methodSummary="Return a URI for the given molecule, or an empty String if no match was found",
		params="IMolecule molecule"
	)
	public String getURI(IMolecule molecule) throws BioclipseException;

	@PublishedMethod(
		methodSummary="Return URIs for similar molecules, or an empty List if no similar compounds were found. "
				+ "It uses the Tanimoto distance, with a minimal similarity treshold of 0.8.",
		params="IMolecule molecule"
			)
	public List<String> findSimilar(IMolecule molecule) throws BioclipseException;

	@PublishedMethod(
		methodSummary="Return URIs for similar molecules, or an empty List if no similar compounds were found. "
				+ "It uses the Tanimoto distance, with a minimal similarity treshold of 0.8 if no "
				+ " (null) or invalid value given.",
		params="IMolecule molecule, Double treshold"
	)
	public List<String> findSimilar(IMolecule molecule, Double treshold) throws BioclipseException;

}
