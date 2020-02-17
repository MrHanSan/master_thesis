package master.ntnu;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb.TDBFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting...");

        String directory = "../../yago/db" ;
        Dataset dataset = TDBFactory.createDataset(directory);
        dataset.begin(ReadWrite.READ);
        Model model = dataset.getDefaultModel();

        HashSet<String> queryWords = new HashSet<>();
//        queryWords.add("Trondheim");
//        queryWords.add("Science");
//        queryWords.add("School");

//        queryWords.add("Bomb");
//        queryWords.add("Lock"); //ancient, roman, catholic, history
        queryWords.add("Ancient");
        queryWords.add("Roman");
        queryWords.add("Catholic");
        queryWords.add("History");

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
                        "SELECT DISTINCT ?s ?p WHERE { ?s ?p <" + RESOURCE + "Marseille> . " +
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

        try (QueryExecution qexec = QueryExecutionFactory.create(query, model) ) {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                if (results.nextSolution() == null) continue;
                try {
                    QuerySolution soln = results.nextSolution();
                    System.out.println(soln);
                    RDFNode sub = soln.get("s");
                    if( !sub.isURIResource() ) continue;
                    roots.add(new YagoNode(sub.toString()));
                } catch (NoSuchElementException e) {
                    System.out.println(e);
                }
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
        HashSet<YagoNode> hitNodes = new HashSet<>();
        HashSet<String> nodeWords = new HashSet<>();

        while (!nodes.isEmpty()) {
            YagoNode node = nodes.get(0);
            if (node.getDepth() > maxDepth) break;
            nodeWords.clear();
            for (String word : queryWords) {
                if (node.getNodeData().toLowerCase().contains(word.toLowerCase())) {
                    nodeWords.add(word);
                    node.addWord(word);
                    node.addHitChild(node);
                    YagoNode n = node;
                    for (int i = node.getDepth(); i > 0; i--) {
                        n = n.getParent();
                        if (n.getDepth() == 0) hitNodes.add(n);
                    }
                }
            }
            node.setNodeMatchWords(nodeWords);
            if (node.getWords().equals(queryWords)) maxDepth = node.getDepth();
            List<YagoNode> newNodes = traverse(model, node);
            if (newNodes != null) {
                nodes.addAll(newNodes);
            }
            nodes.remove(0);
        }
        System.out.println("");
        System.out.println("Found Match...");
        findMinSubgraph(hitNodes, queryWords);
    }


    // Useing kruskals -ish algo(?) Write something about that...
    public static void findMinSubgraph (HashSet<YagoNode> rootNodes, HashSet<String> queryWords) {
        HashSet<YagoNode> minTree = new HashSet<>();
        HashSet<YagoNode> parentList = new HashSet<>();
        final AtomicBoolean newMin = new AtomicBoolean(false);

        for (YagoNode node : rootNodes) {
            parentList.clear();
            minTree.clear();
            minTree.add(node);

            // Check if root contains all words
            if (node.getNodeMatchWords().equals(queryWords)) {
                rankSubGraphs(minTree, queryWords);
            }

            // Else find the node with most fitting words.
            else {
                for (YagoNode n : node.getHitChildren()) {
                    newMin.set(false);
                    for (YagoNode min : minTree) {
                        if (!min.getNodeMatchWords().containsAll(n.getNodeMatchWords())) {
                            newMin.set(true);
                            for (String s : n.getNodeMatchWords()) {
                                if (min.getNodeMatchWords().contains(s)) min.removeNodeMatchWord(s);
                            }
                        }
                        else {
                            newMin.set(false);
                            break;
                        }
                    }
                    if (newMin.get()) {
                        minTree.add(n);
                    }
                    minTree.removeIf(e -> (e.getNodeMatchWords().isEmpty()));
                }
            }
            for (YagoNode min : minTree) {
                YagoNode n = min;
                for (int i=min.getDepth(); i>0;i--) {
                    if (n.getParent() != null) {
                        parentList.add(n.getParent());
                        n = n.getParent();
                    }
                }
            }
            minTree.addAll(parentList);
            rankSubGraphs(minTree, queryWords);
        }
    }


    public static void rankSubGraphs(HashSet<YagoNode> rootNodes, HashSet<String> queryWords) {
        double score = 0.0;
        double hitRate = 0.0;
//        float score = (float) node.getWords().size() / queryWords.size() / (node.getHitChildren().size() + 1);
        for (YagoNode node : rootNodes) {
            for (int j = 0; j < node.getDepth(); j++) { System.out.print("\t"); }
            System.out.println(node.getNodeData() + "   " + node.getNodeMatchWords());
            for (String s : node.getNodeMatchWords()) {
                if (queryWords.contains(s)) hitRate += 1;
            }
            score += node.getDepth() + 1;
        }
        hitRate = hitRate/queryWords.size();
        System.out.println("Score: " + hitRate/score);
        System.out.println("----");
        System.out.println("");
    }


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
