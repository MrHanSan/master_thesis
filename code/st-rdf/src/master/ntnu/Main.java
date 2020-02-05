package master.ntnu;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb.TDBFactory;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting...");

        String directory = "../../yago/db" ;
        Dataset dataset = TDBFactory.createDataset(directory);
        dataset.begin(ReadWrite.READ);
        Model model = dataset.getDefaultModel();

        HashSet<String> queryWords = new HashSet<>();
//        queryWords.add("Trondheim");
        queryWords.add("Science");
        queryWords.add("School");

        traverseStart(model, queryWords);

        dataset.end();
    }


    private static List<YagoNode> getRoots (Model model) {
//        List<String> roots = new ArrayList<String>();
        List<YagoNode> roots = new ArrayList<YagoNode>();
        final String RESOURCE = "http://yago-knowledge.org/resource/";
        String queryString =
                "PREFIX yago:<" + RESOURCE + "> " +
                        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema> " +
//                "SELECT ?s ?p ?o WHERE { ?s <"+RESOURCE+"isLocatedIn> ?o .FILTER regex(str(?s), \"Oslo\") " +
//                        "FILTER ( ?s = <"+RESOURCE+"isLocatedIn> ) " +
//                "SELECT ?s ?p ?o WHERE { ?s ?p ?o .FILTER regex(str(?s), \"Oslo\") " +
                "SELECT DISTINCT ?s ?p WHERE { ?s ?p <" + RESOURCE + "Trondheim> . " +
//                        "SELECT DISTINCT ?o WHERE { <" + RESOURCE + "Oslo> ?p ?o . " +
//                "SELECT DISTINCT ?s { { ?s yago:isLocatedIn <" + RESOURCE + "Oslo> . } " +
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
            while(results.hasNext()) {
                if (results.nextSolution() == null) continue;
                QuerySolution soln = results.nextSolution();
                System.out.println(soln);
                RDFNode sub = soln.get("s");

                if( !sub.isURIResource() ) continue;


//                roots.add(sub.toString());
                roots.add(new YagoNode(sub.toString()));
            }
        }
        return roots;
    }


    public static void traverseStart(Model model, HashSet<String> queryWords) {

        // Use a list in parent node for all children that gets a hit.
        // group all hit Children
        // while hitListChildren is not empty: select child, check for hit children in child, ... something, its dinnertime...
        // use node level for tabs when printing, displaying inheratance
        List<YagoNode> nodes = getRoots(model);
        int maxDepth = 3;
//        ArrayList<YagoNode> hitNodes = new ArrayList<>();
        HashSet<YagoNode> hitNodes = new HashSet<>();

//        HashSet<String> found = new HashSet<>();
        while (!nodes.isEmpty()) {
            YagoNode node = nodes.get(0);
            if (node.getDepth() > maxDepth) break;
            for (String word : queryWords) {
                if (node.getNodeData().toLowerCase().contains(word.toLowerCase())) {
                    node.addWord(word);
                    node.addHitChild(node);
                    if (node.getDepth() > 0) {
                        System.out.println(node.getWords());
                        System.out.println(node.getParent().getWords());
                    }

//                    found.add(word);
                    YagoNode n = node;
                    for (int i=node.getDepth(); i > 0; i--) {
                        n = n.getParent();
                        System.out.println(n.getDepth());
                        if (n.getDepth() == 0) hitNodes.add(n);
                    }
                }
            }
            if (node.getWords().equals(queryWords)) maxDepth = node.getDepth();
//            System.out.println(node.getNodeData());
            List<YagoNode> newNodes = traverse(model, node);
            if (newNodes != null) {
                nodes.addAll(newNodes);
            }
            nodes.remove(0);
        }
        System.out.println("");
        System.out.println("Found Match...");
        for (YagoNode nodeParent : hitNodes) {
            System.out.println(nodeParent.getNodeData());
//        for (YagoNode node : hitNodes) {
            for (YagoNode node : nodeParent.getHitChildren()) {
//            for (int i=node.getDepth(); i >= 0; i--) {
//                System.out.println("Node depth");
//                for (int j = 0; j < node.getDepth(); j++) { System.out.print("\t"); }
//                System.out.println(node.getDepth());
                for (int j = 0; j < node.getDepth(); j++) { System.out.print("\t"); }
                System.out.println(node.getNodeData());
                if (node.getParent() != null) node = node.getParent();
            }
            System.out.println("----");
            System.out.println("");
        }
    }
//    }


    public static ArrayList<YagoNode> traverse (Model model, YagoNode yagoNode) {
        String entity = yagoNode.getNodeData();
        ArrayList<YagoNode> children = new ArrayList<YagoNode>();
        if (entity == null) return null;
        if (entity.contains("<") || entity.contains(">") || entity.contains("\"") || entity.contains(" ")) return null;

        String queryString 	= "SELECT ?s WHERE { "
                + "?s ?p <" + entity + "> . }";
//        + "?s <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + entity + "> . }";

        try {
            Query query = QueryFactory.create(queryString);

            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    RDFNode sub = soln.get("s");

                    if (sub == null || !sub.isURIResource()) continue;

                    String str = sub.toString();
                    children.add(new YagoNode(yagoNode, str));
                }
            } catch (QueryParseException e) {
                return null;
            }
        } catch (QueryParseException e) {
            System.out.println(queryString);
            return null;
        }
        return children;
    }
}
