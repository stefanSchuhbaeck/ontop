package it.unibz.inf.ontop.obda;

/*
 * #%L
 * ontop-test
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.inf.ontop.io.ModelIOManager;
import it.unibz.inf.ontop.model.OBDADataFactory;
import it.unibz.inf.ontop.model.OBDAModel;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.owlrefplatform.core.QuestPreferences;
import it.unibz.inf.ontop.owlrefplatform.owlapi.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/***
 * Test same as using h2 simple database on wellbores
 */
public class H2NoDuplicatesSameAsTest {

	private OBDADataFactory fac;
	private QuestOWLConnection conn;

	Logger log = LoggerFactory.getLogger(this.getClass());
	private OBDAModel obdaModel;
	private OWLOntology ontology;

	final String owlfile = "src/test/resources/sameAs/wellbores-no-duplicates.owl";
	final String obdafile = "src/test/resources/sameAs/wellbores-Tcan-linkingT.obda";
	private QuestOWL reasoner;
	private Connection sqlConnection;

	@Before
	public void setUp() throws Exception {

			 sqlConnection= DriverManager.getConnection("jdbc:h2:mem:wellboresNoDuplicates","sa", "");
			    java.sql.Statement s = sqlConnection.createStatement();
			  
//			    try {
			    	String text = new Scanner( new File("src/test/resources/sameAs/wellbores-Tcan-linkingT.sql") ).useDelimiter("\\A").next();
			    	s.execute(text);
//			    	Server.startWebServer(sqlConnection);
			    	 
//			    } catch(SQLException sqle) {
//			        System.out.println("Exception in creating db from script");
//			    }
			   
			    s.close();
		
		
		// Loading the OWL file
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument((new File(owlfile)));

		// Loading the OBDA data
		fac = OBDADataFactoryImpl.getInstance();
		obdaModel = fac.getOBDAModel();
		
		ModelIOManager ioManager = new ModelIOManager(obdaModel);
		ioManager.load(obdafile);
	
		QuestPreferences p = new QuestPreferences();
		p.setCurrentValueOf(QuestPreferences.SQL_GENERATE_REPLACE, QuestConstants.FALSE);

		// Creating a new instance of the reasoner
		QuestOWLFactory factory = new QuestOWLFactory();
		QuestOWLConfiguration config;

		config = QuestOWLConfiguration.builder().obdaModel(obdaModel).preferences(p).build();
//				.sameAsMappings(true).build();


		reasoner = (QuestOWL) factory.createReasoner(ontology, config);

		// Now we are ready for querying
		conn = reasoner.getConnection();

		
	}


	@After
	public void tearDown() throws Exception{
		conn.close();
		reasoner.dispose();
		if (!sqlConnection.isClosed()) {
			java.sql.Statement s = sqlConnection.createStatement();
			try {
				s.execute("DROP ALL OBJECTS DELETE FILES");
			} catch (SQLException sqle) {
				System.out.println("Table not found, not dropping");
			} finally {
				s.close();
				sqlConnection.close();
			}
		}
	}



	private ArrayList runTests(String query) throws Exception {

		QuestOWLStatement st = conn.createStatement();
		ArrayList<String> retVal = new ArrayList<>();
		try {
			QuestOWLResultSet rs = st.executeTuple(query);
			while(rs.nextRow()) {
				for (String s : rs.getSignature()) {
					OWLObject binding = rs.getOWLObject(s);

					String rendering = ToStringRenderer.getInstance().getRendering(binding);
					retVal.add(rendering);
					log.debug((s + ":  " + rendering));
				}
			}

		} catch (Exception e) {
			throw e;
		} finally {
			try {

			} catch (Exception e) {
				st.close();
				assertTrue(false);
			}
			conn.close();
			reasoner.dispose();
		}
		return retVal;

	}



	@Test
    public void testSameAs1() throws Exception {
        String query =  "PREFIX : <http://ontop.inf.unibz.it/test/wellbore#> Select DISTINCT ?x ?y  WHERE{\n" +
				"?x a :Wellbore .\n" +
				"?x :inWell ?y .\n" +
				"}\n";

		ArrayList<String> results = runTests(query);
		ArrayList<String> expectedResults = new ArrayList<>();

		assertEquals(expectedResults.size(), results.size() );
		assertEquals(expectedResults, results);
    }

	@Test
	public void testSameAs2() throws Exception {
		String query =  "PREFIX : <http://ontop.inf.unibz.it/test/wellbore#> Select DISTINCT ?x WHERE{\n" +
				"?x a :Wellbore .\n" +
				"}\n";

		ArrayList<String> results = runTests(query);
		ArrayList<String> expectedResults = new ArrayList<>();

		assertEquals(expectedResults.size(), results.size() );
		assertEquals(expectedResults, results);
	}

	@Test
	public void testSameAs3() throws Exception {
		String query =  "PREFIX : <http://ontop.inf.unibz.it/test/wellbore#> Select DISTINCT ?x ?y ?z WHERE{\n" +
				"?x a :Wellbore .\n" +
				"?x :inWell ?y .\n" +
				"?x :name ?z .\n" +
				"}\n";

		ArrayList<String> results = runTests(query);
		ArrayList<String> expectedResults = new ArrayList<>();

		assertEquals(expectedResults.size(), results.size() );
		assertEquals(expectedResults, results);
	}

	/**
	 * TODO: transform the input mappings in wellbores-same-as to the mappings in wellbores-Tcan-linkingT
	 *
	 */



}

