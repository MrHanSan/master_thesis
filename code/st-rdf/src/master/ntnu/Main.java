package master.ntnu;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDBFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) {
        System.out.println(Runtime.getRuntime().totalMemory());
        long startTime;
        long endTime;
        int nodeCount;
        String queryWordsString = "";

        System.out.println("Starting...");

        String directory = "../../yago/db" ;
        Dataset dataset = TDBFactory.createDataset(directory);
        dataset.begin(ReadWrite.READ);
        Model model = dataset.getDefaultModel();

        List<String> queryWords = readQueryWords("rndZipf.txt");
        List<String> queryPlaces = readQueryWords("place.txt");

        for (String place : queryPlaces) {
            for (int i = 0; i < queryWords.size(); i += 2) {
                queryWordsString = "";
                HashSet<String> queryWordsSet = new HashSet<>();
                queryWordsSet.add(queryWords.get(i));
                queryWordsSet.add(queryWords.get(i + 1));

                System.out.println(queryWords.get(i));
                System.out.println(queryWords.get(i + 1));
                queryWordsString += "Query Words: " + queryWords.get(i) + ", " + queryWords.get(i + 1);
                WriteResults(queryWordsString, place);

                startTime = System.currentTimeMillis();
                nodeCount = traverseStart(model, queryWordsSet, place);
                endTime = System.currentTimeMillis();
                WriteResults("\nNodes visited: " + nodeCount +
                        "\nTotal execution time: " + (endTime - startTime) +
                        "\n\n", place);

                System.out.println("\nTotal execution time: " + (endTime - startTime));
            }

            queryWords = readQueryWords("rndZipf4.txt");
            for (int i = 0; i < queryWords.size()-4; i += 4) {
                queryWordsString = "";
                HashSet<String> queryWordsSet = new HashSet<>();

                queryWordsSet.add(queryWords.get(i));
                queryWordsSet.add(queryWords.get(i + 1));
                queryWordsSet.add(queryWords.get(i + 2));
                queryWordsSet.add(queryWords.get(i + 3));

                System.out.println(queryWords.get(i));
                System.out.println(queryWords.get(i + 1));
                System.out.println(queryWords.get(i + 2));
                System.out.println(queryWords.get(i + 3));

                queryWordsString += "Query Words: " + queryWords.get(i) + ", " + queryWords.get(i + 1);
                queryWordsString += ", " + queryWords.get(i + 2) + ", " + queryWords.get(i + 3);
                WriteResults(queryWordsString, place);

                startTime = System.currentTimeMillis();
                nodeCount = traverseStart(model, queryWordsSet, place);
                endTime = System.currentTimeMillis();
                WriteResults("\nNodes visited: " + nodeCount +
                        "\nTotal execution time: " + (endTime - startTime) +
                        "\n\n", place);

                System.out.println("\nTotal execution time: " + (endTime - startTime));
            }
        }

        dataset.end();
    }

    private static List<String> readQueryWords(String fileName) {
        List<String> queryWords = new ArrayList<String>();
        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for(String line; (line = br.readLine()) != null; ) {
                if(!line.isEmpty()) {
                    queryWords.add(line.split(" ")[0]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queryWords;
    }

    private static List<YagoNode> getRoots (Model model, String place) {
        List<YagoNode> roots = new ArrayList<YagoNode>();
        final String RESOURCE = "http://yago-knowledge.org/resource/";
        String queryString =
                "PREFIX yago:<" + RESOURCE + "> " +
                        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema> " +
                        "SELECT DISTINCT ?s ?p WHERE { ?s ?p <" + RESOURCE + place + "> . " +
                        "FILTER ( ?p = <http://yago-knowledge.org/resource/isLocatedIn> ) . " +
                        "}";

        Query query = QueryFactory.create(queryString);

//        System.out.println("root");
//        System.out.println("<" + RESOURCE + place + ">");
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model) ) {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                try {
                    QuerySolution soln = results.nextSolution();
//                    System.out.println(soln);
                    RDFNode sub = soln.get("s");
                    if( !sub.isURIResource() ) continue;
                    roots.add(new YagoNode(sub.toString()));
                } catch (NoSuchElementException | NullPointerException e) {
                    System.out.println("Root error: ");
                    System.out.println(e);
                }
            }
        } catch (NullPointerException e) {
            System.out.println("Root outer error");
            System.out.println(e);
        }
        return roots;
    }


    public static int traverseStart(Model model, HashSet<String> queryWords, String place) {
        // Use a list in parent node for all children that gets a hit.
        // group all hit Children
        // while hitListChildren is not empty: select child, check for hit children in child, ... something, its dinnertime...
        // use node level for tabs when printing, displaying inheratance
        List<YagoNode> roots = getRoots(model, place);
        List<YagoNode> nodes = new ArrayList<YagoNode>();
        int maxDepth = 1;
        int nodeCount = 0;
        HashSet<YagoNode> hitNodes = new HashSet<>();

        WriteResults("\nRoot nodes Found: " + roots.size(), place);
        for (YagoNode root : roots) {
            nodes.clear();
            nodes.add(root);
            while (!nodes.isEmpty()) {
                nodeCount += 1;
                //            System.out.println(nodes.size());
                YagoNode node = nodes.get(0);

                if (node.getDepth() > maxDepth) break;
                for (String word : queryWords) {
                    if (node.getTokenList().contains(word)) {
                        node.addNodeMatchWord(word);
                        node.addHitChild(node);
                        YagoNode n = node;
                        for (int i = node.getDepth(); i > 0; i--) {
                            n = n.getParent();
                            if (n.getDepth() == 0) hitNodes.add(n);
                        }
                    }
                }
                if (node.getTokenList().equals(queryWords)) maxDepth = node.getDepth();
                List<YagoNode> newNodes = traverse(model, node, queryWords);
                if (newNodes != null) {
                    nodes.addAll(newNodes);
                }
                nodes.remove(0);
            }
        }
        System.out.println("");
        System.out.println("Found Match...");
        findMinSubgraph(hitNodes, queryWords, place);
        return nodeCount;
    }


    // Useing kruskals -ish algo(?) Write something about that...
    public static void findMinSubgraph (HashSet<YagoNode> rootNodes, HashSet<String> queryWords, String place) {
        HashSet<YagoNode> minTree = new HashSet<>();
        HashSet<YagoNode> parentList = new HashSet<>();
        HashSet<String> removeWords = new HashSet<>();
        final AtomicBoolean newMin = new AtomicBoolean(false);

        for (YagoNode node : rootNodes) {
            parentList.clear();
            minTree.clear();
            minTree.add(node);

            // Check if root contains all words
            if (node.getTokenList().containsAll(queryWords)) {
                rankSubGraphs(minTree, queryWords, place);
            }

            // Else find the node with most fitting words.
            else {
                for (YagoNode newNode : node.getHitChildren()) {
                    newMin.set(false);
                    for (YagoNode min : minTree) {
                        if (!min.getNodeMatchWords().containsAll(newNode.getNodeMatchWords()) ||
                                newNode.getNodeMatchWords().equals(min.getNodeMatchWords()) &&
                                        newNode.getDepth() <= min.getDepth()) {
                            newMin.set(true);
                            for (String s : newNode.getNodeMatchWords()) {
                                if (min.getNodeMatchWords().contains(s)) removeWords.add(s);
                            }
                            min.getNodeMatchWords().removeAll(removeWords);
                        }
                        else {
                            newMin.set(false);
                            break;
                        }
                    }
                    if (newMin.get()) {
                        minTree.add(newNode);
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
            rankSubGraphs(minTree, queryWords, place);
        }
    }


    public static void rankSubGraphs(HashSet<YagoNode> rootNodes, HashSet<String> queryWords, String place) {
        double score = 0.0;
        double hitRate = 0.0;
        String results = "\n";
        String scoreString = "";
        for (YagoNode node : rootNodes) {
            results += "\n";
            for (int j = 0; j < node.getDepth(); j++) {
                results += "\t";
            }
            results += node.getNodeData() + "   " + node.getNodeMatchWords();
           for (String s : node.getTokenList()) {
                if (queryWords.contains(s)) hitRate += 1;
            }
            score += node.getDepth() + 1;
        }
        System.out.println(results);
        WriteResults(results, place);
        hitRate = hitRate/queryWords.size();

        scoreString = "\nScore: " + hitRate/score;
        scoreString += "\n----";
        scoreString += "\n";
        System.out.println(scoreString);
        WriteResults(scoreString, place);
    }


    public static ArrayList<YagoNode> traverse (Model model, YagoNode yagoNode, HashSet<String> queryWords) {
        String entity = yagoNode.getNodeData();
        ArrayList<YagoNode> children = new ArrayList<YagoNode>();
        if (entity == null) return null;
        if (entity.contains("<") || entity.contains(">") || entity.contains("\"") || entity.contains(" ")) return null;

        String queryString 	= "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT DISTINCT ?p ?o WHERE { " +
                "<" + entity + "> ?p ?o . " +
                "FILTER ( ?p != rdf:type ) " +
                "}";

        try {
            Query query = QueryFactory.create(queryString);
//            System.out.println("trav");
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
//                    System.out.println("?s " + entity + " " + soln);
                    RDFNode sub = soln.get("o");

//                    System.out.println(soln);

                    if (sub == null || !sub.isURIResource()) continue;
                    String str = sub.toString();
                    String[] uriSplit = str.split("/");
                    String[] tokens = uriSplit[uriSplit.length-1].replaceAll(",", "").split("_");
                    System.out.println(tokens);
                    // children.add(new YagoNode(yagoNode, str));
                   for (String word : queryWords) {
                       if (Arrays.asList(tokens).contains(word)) {
                           children.add(new YagoNode(yagoNode, str));
                           break;
                       }
                   }
                }
            } catch (QueryParseException | NullPointerException e) {
//                System.out.println(entity);
//                System.out.println(e);
//                System.out.println();
            }
        } catch (QueryParseException e) {
            System.out.println(queryString);
            return null;
        }
        return children;
    }

    public static ArrayList<YagoNode> temporalNodes (Model modes, YagoNode yagoNode) {
        String entity = yagoNode.getNodeData();
        ArrayList<YagoNode> children = new ArrayList<YagoNode>();
        if (entity == null) return null;
        if (entity.contains("<") || entity.contains(">") || entity.contains("\"") || entity.contains(" ")) return null;
        String queryString 	= "@prefix xsd:<http://www.w3.org/2001/XMLSchema#> . " +
                "SELECT ?s ?p datatype(?o) { " +
                "?s ?p <" + entity + "> . " +
                "s? ?p ?o . " +
                "FILTER ( datatype(?o) =  xsd:date ) . " +
                "}";

        return children;
    }

    public static void WriteResults(String s, String place) {
        try {
            Files.write(Paths.get("results/" + place + ".txt"), s.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println(e);
        }

    }
}
