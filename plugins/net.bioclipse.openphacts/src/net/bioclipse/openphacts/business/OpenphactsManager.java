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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bioclipse.cdk.business.ICDKManager;
import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.core.domain.StringMatrix;
import net.bioclipse.managers.business.IBioclipseManager;
import net.bioclipse.openphacts.model.CWResult;
import net.bioclipse.rdf.business.IRDFStore;
import net.bioclipse.rdf.business.RDFManager;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.github.egonw.ops4j.Compounds;
import com.github.egonw.ops4j.ConceptType;
import com.github.egonw.ops4j.Concepts;
import com.github.egonw.ops4j.Mapping;
import com.github.egonw.ops4j.Targets;

/**
 * A manager to query the Open PHACTS infrastructure.
 * @author ola
 *
 */
public class OpenphactsManager implements IBioclipseManager {

	private static final Logger logger = Logger.getLogger(OpenphactsManager.class);
	public static final String OPENPHACTS_PREFERENCE_NODE ="openphacts.prefs.node";
	public static final String OPENPHACTS_ENDPOINT_PREFERENCE ="openphacts.prefs.endpoint";
	public static final String CONCEPTWIKI_ENDPOINT_PREFERENCE ="conceptwiki.prefs.endpoint";

	private static final String EXACT_MATCHES =
		"PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
		"SELECT ?match WHERE {" +
	    " ?concept skos:exactMatch ?match ." +
		"}";
	private static final String CONCEPT_SEARCH_RESULTS =
		"SELECT ?uuid ?match WHERE {" +
		" ?uuid <http://www.openphacts.org/api#match> ?match ." +
		"}";
	private static final String PROTEIN_INFO =
		"SELECT ?uuid ?name ?residues ?pi WHERE {" +
		" ?uuid <http://www.w3.org/2004/02/skos/core#prefLabel> ?name ;" +
		"       <http://www.w3.org/2004/02/skos/core#exactMatch> ?dbUri ." +
		" OPTIONAL {" + 
		"  ?dbUri <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/theoreticalPi> ?pi ;" +
		"         <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/numberOfResidues> ?residues ." +
		" }" +
		"}";
	private static final String COMPOUND_INFO =
			"SELECT ?compound ?smiles ?inchi ?logp ?hba ?hbd ?ro5Violations ?psa ?rtb ?molweight ?molformula WHERE {" +
			" ?compound_uri <http://www.openphacts.org/api#smiles> ?smiles ; " +
			"               <http://www.openphacts.org/api#inchi> ?inchi . " +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#logp> ?logp . }" +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#hba> ?hba . }" +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#hbd> ?hbd . }" +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#ro5_violations> ?ro5Violations . }" +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#psa> ?psa . }" +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#rtb> ?rtb . }" +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#molweight> ?molweight . }" +
			" OPTIONAL { ?compound_uri <http://www.openphacts.org/api#molformula> ?molformula . }" +
			"}";
	
    private static final String APPID = "5dea5f60";
	private static final String APPKEY = "064e38c33ad32e925cd7a6e78b7c4996";

	RDFManager rdf = new RDFManager();
	
	public String getManagerName() {
		return "openphacts";
	}

	//Get and set OPS and CW endpoint preferences via manager methods
	private String getConceptWikiEndpoint(){
		 IEclipsePreferences preferences = ConfigurationScope.INSTANCE
				  .getNode(OPENPHACTS_PREFERENCE_NODE);
		 return preferences.get(CONCEPTWIKI_ENDPOINT_PREFERENCE,"default");
	}
	private String getOPSLDAendpoint(){
		 IEclipsePreferences preferences = ConfigurationScope.INSTANCE
				  .getNode(OPENPHACTS_PREFERENCE_NODE);
		 return preferences.get(OPENPHACTS_ENDPOINT_PREFERENCE,"https://beta.openphacts.org/1.3/");
	}
	public void setConceptWikiEndpoint(String endpoint){
		 IEclipsePreferences preferences = ConfigurationScope.INSTANCE
				  .getNode(OPENPHACTS_PREFERENCE_NODE);
		 preferences.put(CONCEPTWIKI_ENDPOINT_PREFERENCE,endpoint);
	}
	public void setOPSLDAendpoint(String endpoint){
		 IEclipsePreferences preferences = ConfigurationScope.INSTANCE
				  .getNode(OPENPHACTS_PREFERENCE_NODE);
		 preferences.put(OPENPHACTS_ENDPOINT_PREFERENCE,endpoint);
	}

	/**
	 * Look up proteins in conceptwiki
	 * 
	 * @param query Query string to search for.
	 * @return list of CWresults (name + cwid)
	 * @throws BioclipseException 
	 */
	public List<CWResult> lookUpProteins(String query, IProgressMonitor monitor) throws BioclipseException {
		return lookUpCW(query, "protein", monitor);
	}		

	/**
	 * Look up compounds in conceptwiki
	 * 
	 * @param query Query string to search for.
	 * @return list of CWresults (name + cwid)
	 * @throws BioclipseException 
	 */
	public List<CWResult> lookUpCompounds(String query, IProgressMonitor monitor)
			throws BioclipseException {
		return lookUpCW(query, "compound", monitor);
	}

	public List<String> mapURI(String URI, IProgressMonitor monitor)
			throws BioclipseException {
		Mapping mapper;
		try {
			mapper = Mapping.getInstance(
				getOPSLDAendpoint(), APPID, APPKEY
			);
			String rdfContent = mapper.mapUri(URI);
			System.out.println("OPS LDA results: " + rdfContent);
			IRDFStore store = rdf.createInMemoryStore();
			rdf.importFromString(store, rdfContent, "Turtle", monitor);
			StringMatrix matches = rdf.sparql(store, EXACT_MATCHES);
			return matches.getColumn("match");
		} catch (Exception e) {
			throw new BioclipseException(e.getMessage(), e);
		}
	}
	
	/**
	 * Private method for CW access
	 */
	private List<CWResult> lookUpCW(String name, String type, IProgressMonitor monitor)
			throws BioclipseException {
		
		if (name==null || name.length()<3)
			throw new BioclipseException(
							"Searches need to be at least 3 characters long");

		monitor.beginTask("Searching ConceptWiki for:" + name, IProgressMonitor.UNKNOWN);
		monitor.subTask("Searching ConceptWiki for:" + name);

		//Query CW based on type
		IRDFStore store = rdf.createInMemoryStore();
		try {
			Concepts concepts = Concepts.getInstance(getOPSLDAendpoint(), APPID, APPKEY);
			ConceptType cwtype = null;
			if ("compound".equals(type)) {
				cwtype = ConceptType.CHEMICAL_VIEWED_STRUCTURALLY;
			} else if ("protein".equals(type)) {
				cwtype = ConceptType.AMINO_ACID_PEPTIDE_OR_PROTEIN;
			} else {
				throw new BioclipseException("Type must be either protein or compound");
			}
			String rdfContent = concepts.freetextByTag(name, cwtype);
			System.out.println("OPS LDA results: " + rdfContent);
			rdf.importFromString(store, rdfContent, "Turtle", monitor);
		} catch (Exception e) {
			throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
		}

		//Parse the results
		List<CWResult> res = new ArrayList<CWResult>();
		try {
			StringMatrix matches = rdf.sparql(store, CONCEPT_SEARCH_RESULTS);
			for (int i=1; i<=matches.getRowCount(); i++) {
				String uuid = matches.get(i, "uuid");
				uuid = uuid.substring(uuid.lastIndexOf('/')+1);
				CWResult result = new CWResult(matches.get(i, "match"), uuid);
				res.add(result);
			}
		} catch (Exception e) {
			throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
		}

		return res;
	}

	/**
	 * Look up target information for a set of CWResults by CWID.
	 * 
	 * @param collection
	 * @param monitor
	 * @return
	 */
	public List<String> getProteinsInfo(List<CWResult> collection, IProgressMonitor monitor)
	throws BioclipseException {

		monitor.beginTask("Retrieving information about protein from Open PHACTS", collection.size());
		List<String> res = new ArrayList<String>();
		
		Targets targets = null;
		try {
			targets = Targets.getInstance(getOPSLDAendpoint(), APPID, APPKEY);
		} catch (Exception e) {
			throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
		}
		int i=0;
		for (CWResult protein : collection){
			i++;
			monitor.subTask("Protein (" + i + "/" + 
					collection.size() + "): " + protein.getName());
			monitor.worked(1);
			if (monitor.isCanceled())
				return null;

			String cwikiURI = "http://www.conceptwiki.org/concept/" +protein.getCwid();
			
			//Query CW based on type
			IRDFStore store = rdf.createInMemoryStore();
			try {
				String rdfContent = targets.info(cwikiURI);
				System.out.println("OPS LDA results: " + rdfContent);
				rdf.importFromString(store, rdfContent, "Turtle", monitor);

				// process the results
				StringMatrix matches = rdf.sparql(store, PROTEIN_INFO);
				String tinfo = "";
				if (matches.getRowCount() > 0) {
					String name = matches.get(1, "name");
					tinfo += name + "\n";
				}
				for (int prot=1; prot<=matches.getRowCount(); prot++) {
					String residues = matches.get(prot, "residues");
					tinfo += "\nresidues=" + residues;
					String pi = matches.get(prot, "pi");
					tinfo += "\npi=" + pi;
					res.add(tinfo);
				}
			} catch (Exception e) {
				if (e.getMessage().contains("404: Not Found")) {
					// just skip this entry: the URI provided did not return matching results
					res.add("");
				} else {
					throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
				}
			}			
		}
		
		return res;
	}


	/**
	 * Look up compound information, such as pharmacological props for a set 
	 * of CWResults by CWID.
	 * @param collection
	 * @param monitor
	 * @return List of molecules with properties set
	 * @throws BioclipseException
	 */
	public List<IMolecule> getCompoundsInfo(List<CWResult> collection, IProgressMonitor monitor) 
			throws BioclipseException{
		
		ICDKManager cdk = net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager();
		java.util.List<IMolecule> mols = new ArrayList<IMolecule>();

		Compounds compounds = null;
		try {
			compounds = Compounds.getInstance(getOPSLDAendpoint(), APPID, APPKEY);
		} catch (Exception e) {
			throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
		}

		int i=0;
		monitor.beginTask("Retrieving information about compounds from Open PHACTS", collection.size()*2);
		for (CWResult compound : collection){
			i++;
			//Concatenate into URL and look up in OPS
			String cwikiCompound="http://www.conceptwiki.org/concept/" + compound.getCwid();

			//Look up compound info
			//======================
			logger.debug("Looking up info for compound: " + compound);
			monitor.subTask("Looking up compound info for compound (" + i + "/" + 
					collection.size() + "): " + compound.getName());
			monitor.worked(1);
			if (monitor.isCanceled())
				return null;

			try {
				// List<LDAInfo> compoundInfo=OPSLDAJava.GetCompoundInfo();
				String turtle = compounds.info(cwikiCompound,getOPSLDAendpoint());
				System.out.println("Turtle: " + turtle);

				//First we
				Map<String, String> props = new HashMap<String, String>();
				ICDKMolecule cdkmol=null;

				IRDFStore store = rdf.createInMemoryStore();
				rdf.importFromString(store, turtle, "Turtle", monitor);
				// process the results
				StringMatrix matches = rdf.sparql(store, COMPOUND_INFO);
				System.out.println(matches);

				//for (LDAInfo info : compoundInfo){
				for (int compoundNo=1; compoundNo<=matches.getRowCount(); compoundNo++) {

					//				props.put(info.field, info.value);
					//				logger.debug("Added compound info:" + info.source +" - " + info.field + " - " + info.value);
					String[] propNames = { // see SPARQL in COMPOUND_INFO
						"logp", "hba", "hbd", "ro5Violations", "psa", "rtb", "molweight", "molformula"
					};
					for (String propName : propNames) {
						String propVal = matches.get(compoundNo, propName);
						props.put(propName, propVal);
						System.out.println("Added compound info: " + propName + " - " + propVal);
					}

					//Treat the chemspider field SMILES as special
					String smiles = matches.get(compoundNo, "smiles");
					cdkmol = cdk.fromSMILES(smiles);
				}

				//Look up pharmacology info
				//======================
				//			logger.debug("Looking up pharmacology for compound: " + compound);
				//			monitor.subTask("Looking up compound info for compound (" + i + "/" + 
				//					collection.size() + "): " + compound.getName());
				//			monitor.worked(1);
				//			if (monitor.isCanceled())
				//				return null;
				//			List<Map<String,String>> pharm2 = OPSLDAJava.GetPharmacologyByCompound(cwikiCompound,getOPSLDAendpoint());
				//			for(Map<String,String> pharmainfo : pharm2)
				//			{
				//				for(Entry<String,String> entry: pharmainfo.entrySet()){
				//					props.put(entry.getKey(), entry.getValue());
				//					logger.debug("Added Pharmacology Field: "+ entry.getKey() + " Value: "+entry.getValue());
				//				}
				//			}
				//			
				for (String key : props.keySet())
					cdkmol.setProperty(key, props.get(key));

				mols.add(cdkmol);
			} catch (Exception exception) {
				exception.printStackTrace();
			}

		}

		return mols;

	}

}
