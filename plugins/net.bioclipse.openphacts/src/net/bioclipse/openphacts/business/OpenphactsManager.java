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
import net.bioclipse.core.domain.IStringMatrix;
import net.bioclipse.inchi.InChI;
import net.bioclipse.inchi.business.IInChIManager;
import net.bioclipse.jobs.IReturner;
import net.bioclipse.managers.business.IBioclipseManager;
import net.bioclipse.openphacts.model.CWResult;
import net.bioclipse.rdf.business.IRDFManager;
import net.bioclipse.rdf.business.IRDFStore;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.github.egonw.ops4j.Compounds;
import com.github.egonw.ops4j.ConceptType;
import com.github.egonw.ops4j.Concepts;
import com.github.egonw.ops4j.Mapping;
import com.github.egonw.ops4j.Structures;
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
	private static final String COMPOUND_PHARMA_COUNT =
			"SELECT ?uuid ?count WHERE {" +
			" ?uuid <http://www.openphacts.org/api#compoundPharmacologyTotalResults> ?count ." +
			"}";
	private static final String COMPOUND_URI =
			"SELECT ?compound WHERE {" +
			" ?compound <http://semanticscience.org/resource/CHEMINF_000396> ?inchi ." +
			"}";
	private static final String COMPOUND_SIMILARITY =
			"SELECT ?compound ?relevance WHERE {" +
			" ?compound <http://www.openphacts.org/api/#relevance> ?relevance ." +
			"}";

	private static final String COMPOUND_PHARMA =
			"SELECT * WHERE {" +
			" ?item <http://rdf.ebi.ac.uk/terms/chembl#hasMolecule> ?chembl_compound_uri ; " +
			"   <http://rdf.ebi.ac.uk/terms/chembl#hasAssay> ?assay_uri . " +
			" OPTIONAL { ?item <http://rdf.ebi.ac.uk/terms/chembl#publishedType> ?published_type . } " +
			" OPTIONAL { ?item <http://rdf.ebi.ac.uk/terms/chembl#publishedRelation> ?published_relation . } " +
			" OPTIONAL { ?item <http://rdf.ebi.ac.uk/terms/chembl#publishedValue> ?published_value . } " +
			" OPTIONAL { ?item <http://rdf.ebi.ac.uk/terms/chembl#publishedUnits> ?published_unit . } " +
			" OPTIONAL { ?item <http://rdf.ebi.ac.uk/terms/chembl#pChembl> ?pChembl . } " +
			" OPTIONAL { ?item <http://rdf.ebi.ac.uk/terms/chembl#activityComment> ?act_comment . } " +
			" OPTIONAL { ?assay_uri <http://purl.org/dc/terms/description> ?assay_description. } " +
			"}";
	
    private static final String APPID = "5dea5f60";
	private static final String APPKEY = "064e38c33ad32e925cd7a6e78b7c4996";
	
	public String getManagerName() {
		return "openphacts";
	}

	private String getOPSLDAendpoint(){
		 IEclipsePreferences preferences = ConfigurationScope.INSTANCE
				  .getNode(OPENPHACTS_PREFERENCE_NODE);
		 return preferences.get(OPENPHACTS_ENDPOINT_PREFERENCE,"https://beta.openphacts.org/1.3/");
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
		IRDFManager rdf = net.bioclipse.rdf.Activator.getDefault().getJavaManager();
		Mapping mapper;
		try {
			mapper = Mapping.getInstance(
				getOPSLDAendpoint(), APPID, APPKEY
			);
			String rdfContent = mapper.mapUri(URI);
			System.out.println("OPS LDA results: " + rdfContent);
			IRDFStore store = rdf.createInMemoryStore();
			rdf.importFromString(store, rdfContent, "Turtle");
			IStringMatrix matches = rdf.sparql(store, EXACT_MATCHES);
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

		IRDFManager rdf = net.bioclipse.rdf.Activator.getDefault().getJavaManager();

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
			rdf.importFromString(store, rdfContent, "Turtle");
		} catch (Exception e) {
			throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
		}

		//Parse the results
		List<CWResult> res = new ArrayList<CWResult>();
		try {
			IStringMatrix matches = rdf.sparql(store, CONCEPT_SEARCH_RESULTS);
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

		IRDFManager rdf = net.bioclipse.rdf.Activator.getDefault().getJavaManager();
		
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
				rdf.importFromString(store, rdfContent, "Turtle");

				// process the results
				IStringMatrix matches = rdf.sparql(store, PROTEIN_INFO);
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

	public String getURI(IMolecule molecule, IProgressMonitor monitor) throws BioclipseException {
		if (monitor == null) monitor = new NullProgressMonitor();

		IInChIManager inchi = net.bioclipse.inchi.business.Activator.getDefault().getJavaInChIManager();
		IRDFManager rdf = net.bioclipse.rdf.Activator.getDefault().getJavaManager();

		InChI inchiVal = null;
		try {
			inchiVal = inchi.generate(molecule);
		} catch (Exception exception) {
			throw new BioclipseException(
				"Error while generating an InChI: " + exception.getMessage(), exception
			);
		}
		if (inchiVal != null) {
			// do something
			Structures structures = null;
			try {
				structures = Structures.getInstance(getOPSLDAendpoint(), APPID, APPKEY);
				String turtle = structures.inchi2uri(inchiVal.getValue());
				
				IRDFStore countStore = rdf.createInMemoryStore();
				rdf.importFromString(countStore, turtle, "Turtle");
				IStringMatrix countMatches = rdf.sparql(countStore, COMPOUND_URI);
				if (countMatches.getRowCount() > 0) {
					String uri = countMatches.get(1, "compound");
					return uri;
				}
			} catch (Exception e) {
				throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
			}
		}

		return null;
	}

	public void findSimilar(IMolecule molecule, IReturner<String> returner,
            IProgressMonitor monitor)
	throws BioclipseException {
		findSimilar(molecule, null, returner, monitor);
	}

	public void findSimilar(IMolecule molecule, Double treshold, IReturner<String> returner,
            IProgressMonitor monitor)
	throws BioclipseException {
		if (monitor == null) monitor = new NullProgressMonitor();

		Double tresholdUsed = treshold == null ? Double.valueOf(0.8) : treshold;

		IRDFManager rdf = net.bioclipse.rdf.Activator.getDefault().getJavaManager();
		ICDKManager cdk = net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager();
		String smiles = cdk.calculateSMILES(molecule);

		Structures structures = null;
		try {
			structures = Structures.getInstance(getOPSLDAendpoint(), APPID, APPKEY);
			String turtle = structures.tanimotoSimilarity(smiles, tresholdUsed.floatValue());
			
			IRDFStore countStore = rdf.createInMemoryStore();
			rdf.importFromString(countStore, turtle, "Turtle");
			IStringMatrix countMatches = rdf.sparql(countStore, COMPOUND_SIMILARITY);
			if (countMatches.getRowCount() > 0) {
				for (int hit=1; hit<=countMatches.getRowCount(); hit++) {
					String uri = countMatches.get(hit, "compound");
					returner.partialReturn(uri);
				}
			}
		} catch (Exception e) {
			throw new BioclipseException("Something went wrong: " + e.getMessage(), e);
		}
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

		IRDFManager rdf = net.bioclipse.rdf.Activator.getDefault().getJavaManager();
		ICDKManager cdk = net.bioclipse.cdk.business.Activator.getDefault().getJavaCDKManager();

		List<IMolecule> mols = new ArrayList<IMolecule>();

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
				rdf.importFromString(store, turtle, "Turtle");
				// process the results
				IStringMatrix matches = rdf.sparql(store, COMPOUND_INFO);
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

//				Look up pharmacology info
//				======================
				logger.debug("Looking up pharmacology for compound: " + compound);
				monitor.subTask("Looking up compound info for compound (" + i + "/" + 
						collection.size() + "): " + compound.getName());
				monitor.worked(1);
				if (monitor.isCanceled())
					return null;
				String countTurtle = compounds.pharmacologyCount(cwikiCompound);
				System.out.println("Count turtle: " + countTurtle);
				IRDFStore countStore = rdf.createInMemoryStore();
				rdf.importFromString(countStore, countTurtle, "Turtle");
				IStringMatrix countMatches = rdf.sparql(countStore, COMPOUND_PHARMA_COUNT);
				System.out.println("Count matches: " + countMatches);
				if (countMatches.getRowCount() > 0) {
					int count = Integer.valueOf(countMatches.get(1, "count"));

					if (count > 0) {
						if (count > 100) {
							logger.warn("Retrieving only the first 100 activities. Total found is: " + count);
							count = 100;
						}
						String pharmaTurtle = compounds.pharmacologyList(cwikiCompound, 1, count);
						System.out.println("Pharma turtle: " + pharmaTurtle);
						IRDFStore pharmaStore = rdf.createInMemoryStore();
						rdf.importFromString(pharmaStore, pharmaTurtle, "Turtle");
						IStringMatrix pharmaInfo = rdf.sparql(pharmaStore, COMPOUND_PHARMA);
						System.out.println("pharmaInfo: " + pharmaInfo);

						//			List<Map<String,String>> pharm2 = OPSLDAJava.GetPharmacologyByCompound(cwikiCompound,getOPSLDAendpoint());
						//			for(Map<String,String> pharmainfo : pharm2)
						//			{
						//				for(Entry<String,String> entry: pharmainfo.entrySet()){
						//					props.put(entry.getKey(), entry.getValue());
						//					logger.debug("Added Pharmacology Field: "+ entry.getKey() + " Value: "+entry.getValue());
						for (int actCounter=1;actCounter<=pharmaInfo.getRowCount();actCounter++) {
							String report = onlyIfNotNull("", pharmaInfo.get(actCounter, "published_type"), " ") +
									onlyIfNotNull("", pharmaInfo.get(actCounter, "published_relation"), " ") +
									onlyIfNotNull("", pharmaInfo.get(actCounter, "published_value"), " ") +
									onlyIfNotNull("", pharmaInfo.get(actCounter, "published_unit"), " ") +
									onlyIfNotNull("(pChembl=", pharmaInfo.get(actCounter, "pChembl"), ") ") +
									onlyIfNotNull("Comment: ", pharmaInfo.get(actCounter, "act_comment"), "");
							String propName = (
								pharmaInfo.get(actCounter, "assay_description") != null ?
									pharmaInfo.get(actCounter, "assay_description") :
									"pharmacology"+actCounter
							);
							props.put(propName, report);
							System.out.println("Added compound pharma: " + report);
						}
					} else {
						logger.debug("No pharma found for this compound");
					}
				} else {
					logger.debug("No proper pharma count found in Turtle: " + countTurtle);
				}

				for (String key : props.keySet())
					cdkmol.setProperty(key, props.get(key));

				mols.add(cdkmol);
			} catch (Exception exception) {
				exception.printStackTrace();
			}

		}

		return mols;

	}

	private String onlyIfNotNull(String prefix, String string, String suffix) {
		if (string != null) return prefix + string + suffix;
		return "";
	}

}
