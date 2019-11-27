package master.ntnu;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting...");
//        readFactsFromFile();
        querySelect();
    }

    private static void readFactsFromFile() {
        System.out.println("Read from file...");
        String file = "testData.ttl";
        FileManager.get().addLocatorClassLoader(Main.class.getClassLoader());
        Model model = FileManager.get().loadModel(file, null, "TURTLE");

        StmtIterator iter = model.listStatements();
        try {
            while ( iter.hasNext() ) {
                Statement stmt = iter.next();

                Resource s = stmt.getSubject();
                Resource p = stmt.getPredicate();
                RDFNode o = stmt.getObject();

                if ( s.isURIResource() ) {
                    System.out.print("URI");
                } else if ( s.isAnon() ) {
                    System.out.print("blank");
                }

                if ( p.isURIResource() )
                    System.out.print(" URI ");

                if ( o.isURIResource() ) {
                    System.out.print("URI");
                } else if ( o.isAnon() ) {
                    System.out.print("blank");
                } else if ( o.isLiteral() ) {
                    System.out.print("literal");
                }
                System.out.println();
                System.out.println(stmt);

                System.out.println();
                System.out.println();
            }
        } finally {
            if ( iter != null ) iter.close();
        }
    }

    private static void querySelect() {
        String file = "testData.ttl";
        Model model = FileManager.get().loadModel(file, null, "TURTLE");
        String queryString = "PREFIX yago:<http://yago-knowledge.org/resource/> SELECT * WHERE { ?s yago:isLocatedIn ?x}";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect() ;
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                RDFNode x = soln.get("x") ;       // Get a result variable by name.
//                Resource r = soln.getResource("VarR") ; // Get a result variable - must be a resource
//                Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
                System.out.println(x);
                System.out.println(soln);
            }
        }
    }
}
