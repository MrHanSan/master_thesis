package master.ntnu;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb.TDBFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting...");

        String directory = "../../yago/db" ;
        Dataset dataset = TDBFactory.createDataset(directory);
        dataset.begin(ReadWrite.READ);
        Model model = dataset.getDefaultModel();

        traverseStart(model, null);

        dataset.end();
    }


    private static List<String> getRoots(Model model) {
        List<String> roots = new ArrayList<String>();
        final String RESOURCE = "http://yago-knowledge.org/resource/";
        String queryString =
                "PREFIX yago:<" + RESOURCE + "> " +
                "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema> " +
//                "SELECT ?s ?p ?o WHERE { ?s <"+RESOURCE+"isLocatedIn> ?o .FILTER regex(str(?s), \"Oslo\") " +
//                        "FILTER ( ?s = <"+RESOURCE+"isLocatedIn> ) " +
//                "SELECT ?s ?p ?o WHERE { ?s ?p ?o .FILTER regex(str(?s), \"Oslo\") " +
                "SELECT DISTINCT ?s WHERE { ?s ?p <" + RESOURCE + "Oslo> . " +
//                "SELECT ?s { { ?s yago:isLocatedIn <" + RESOURCE + "Oslo> . } " +
//                        "UNION { ?s yago:isConnectedTo <" + RESOURCE + "Oslo> . } " +
//                        "UNION { ?s yago:isLeaderOf <" + RESOURCE + "Oslo> . } " +
//                        "UNION { ?s yago:happenedIn <" + RESOURCE + "Oslo> . } " +
//                        "UNION { ?s yago:wasBornIn <" + RESOURCE + "Oslo> . } " +
//                        "UNION { ?s yago:diedIn <" + RESOURCE + "Oslo> . } " +
//                        "UNION { ?s yago:hasCapital <" + RESOURCE + "Oslo> . } " +
//                        "UNION { ?s yago:livesIn <" + RESOURCE + "Oslo> . } " +
                "}";

        Query query = QueryFactory.create(queryString);

        try ( QueryExecution qexec = QueryExecutionFactory.create( query, model ) ) {
            ResultSet results = qexec.execSelect();
            while( results.hasNext() )
            {
                QuerySolution soln = results.nextSolution();
                RDFNode sub = soln.get("s");

                if( !sub.isURIResource() ) continue;

                roots.add( sub.toString() );
                System.out.println(soln);
            }
        }
        return roots;
    }


    public static void traverseStart( Model model, String entity) {
        // if starting class available
        if( entity != null ) {
            traverse( model, entity,  new ArrayList<String>(), 0);
        }
        // get roots and traverse each root
        else {
            List<String> roots = getRoots( model );

            for( int i = 0; i < roots.size(); i++ ) {
                traverse( model, roots.get( i ), new ArrayList<String>(), 0);
            }
        }
    }


    public static int traverse( Model model, String entity, List<String> occurs, int depth) {
        if (entity == null  || depth > 2) return 99;
        if (entity.contains("<") || entity.contains(">") || entity.contains("\"") || entity.contains(" ")) return 99;
        int score = 0;

        String queryString 	= "SELECT ?s WHERE { "
                + "?s ?p <" + entity + "> . }";
//        + "?s <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + entity + "> . }";

//        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);

        if (!occurs.contains(entity))
        {
            // print depth times "\t" to retrieve an explorer tree like output
            for (int i = 0; i < depth; i++) { System.out.print("\t"); }
            // print out the URI
            System.out.println(entity);

            try (QueryExecution qexec = QueryExecutionFactory.create( query, model ) ) {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    RDFNode sub = soln.get("s");

                    if (sub == null || !sub.isURIResource()) continue;

                    String str = sub.toString();
//                    if str.contains("")

                    // push this expression on the occurs list before we recurse to avoid loops
                    occurs.add( entity );
                    // traverse down and increase depth (used for logging tabs)
                    traverse( model, str, occurs, depth + 1);
                    // after traversing the path, remove from occurs list
                    occurs.remove( entity );
                }
            }
            catch (QueryParseException e) {
                return 99;
            }
        }
        return score;
    }
}
