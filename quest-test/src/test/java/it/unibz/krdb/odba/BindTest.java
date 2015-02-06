package it.unibz.krdb.odba;


import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.io.QueryIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.*;
import it.unibz.krdb.obda.querymanager.QueryController;
import it.unibz.krdb.obda.querymanager.QueryControllerGroup;
import it.unibz.krdb.obda.querymanager.QueryControllerQuery;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BindTest {
    private OBDADataFactory fac;

    Logger log = LoggerFactory.getLogger(this.getClass());
    private OBDAModel obdaModel;
    private OWLOntology ontology;

    final String owlFile = "src/test/resources/bindTest/ontologyOdbs.owl";
    final String obdaFile = "src/test/resources/bindTest/mappingsOdbs.obda";

    @Before
    public void setUp() throws Exception {

        fac = OBDADataFactoryImpl.getInstance();

        // Loading the OWL file
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument((new File(owlFile)));

        // Loading the OBDA data
        obdaModel = fac.getOBDAModel();

        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        ioManager.load(obdaFile);

    }




    @Test
    public void testOneQuery() throws Exception {

        QuestPreferences p = new QuestPreferences();

//
//        String query = "PREFIX : <http://myproject.org/odbs#> \n" +
//                "\n" +
//                "SELECT DISTINCT ?f ?d " +
//                " ?price \n" +
//                "WHERE {?f a :Film; :hasDirector ?d . \n" +
//                "BIND (\"123\" AS ?price) \n" +
//                "}";

//        String query = "PREFIX : <http://myproject.org/odbs#> \n" +
//                "\n" +
//                "SELECT DISTINCT ?f ?d " +
//                " ?price \n" +
//                "WHERE {?f a :Film; :hasDirector ?d . \n" +
//                "BIND (CONCAT(\"123\", \"456\")  as ?price  )    " +
////                "BIND  \n" +
//                "}";

                String query = "PREFIX : <http://myproject.org/odbs#> \n" +

                "SELECT DISTINCT ?f ?d ?price  " +
                        "\n" +
                "  \n" +
                "WHERE {?f a :Film; :hasDirector ?d .  \n" +
                "BIND (\"123\" AS ?price) \n" +
//                    "?f :genre ?price . " +
                "}";

         runTestQuery(p, query);
    }

    private int runTestQuery(Properties p, String query) throws Exception {

        // Creating a new instance of the reasoner
        QuestOWLFactory factory = new QuestOWLFactory();
        factory.setOBDAController(obdaModel);

        factory.setPreferenceHolder(p);

        QuestOWL reasoner = (QuestOWL) factory.createReasoner(ontology, new SimpleConfiguration());

        // Now we are ready for querying
        QuestOWLConnection conn = reasoner.getConnection();
        QuestOWLStatement st = conn.createStatement();


                log.debug("Executing query: ");
                log.debug("Query: \n{}", query);

                long start = System.nanoTime();
                QuestOWLResultSet res = st.executeTuple(query);
                long end = System.nanoTime();

                double time = (end - start) / 1000;

                int count = 0;
                while (res.nextRow()) {
                    count += 1;
                    for (int i = 1; i <= res.getColumnCount(); i++) {
                         log.debug(res.getSignature().get(i-1) + "=" + res.getOWLObject(i));

                      }
                }
                log.debug("Total result: {}", count);

                assertFalse(count == 0);

                log.debug("Elapsed time: {} ms", time);

        return count;



    }


}

