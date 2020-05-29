package master.ntnu;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDBFactory;

import master.ntnu.YagoNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.NumberFormatException;

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

        List<String> queryWordsTwo = readQueryWords("rndZipf.txt");
        List<String> queryWordsFour = readQueryWords("rndZipf4.txt");
        List<String> queryPlaces = readQueryWords("place.txt");
        List<String> queryDates = readQueryWords("dates.txt");

        for (String place : queryPlaces) {
            for (String dates : queryDates) {
                List<String> queryWords = queryWordsTwo;
                for (int i = 0; i < queryWordsTwo.size(); i += 2) {
                    queryWordsString = "";
                    HashSet<String> queryWordsSet = new HashSet<>();
                    queryWordsSet.add(queryWords.get(i));
                    queryWordsSet.add(queryWords.get(i + 1));

                    System.out.println(queryWords.get(i));
                    System.out.println(queryWords.get(i + 1));
                    queryWordsString += "Query Words: " + queryWords.get(i) + ", " + queryWords.get(i + 1);
                    WriteResults(queryWordsString, place);

                    startTime = System.currentTimeMillis();
                    nodeCount = traverseStart(model, queryWordsSet, place, dates);
                    endTime = System.currentTimeMillis();
                    WriteResults("\nNodes visited: " + nodeCount +
                            "\nTotal execution time: " + (endTime - startTime) +
                            "\n\n", place);

                    System.out.println("\nTotal execution time: " + (endTime - startTime));
                    if (nodeCount == -1) break;
                }

                queryWords = queryWordsFour;
                for (int i = 0; i < queryWords.size(); i += 4) {
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
                    nodeCount = traverseStart(model, queryWordsSet, place, dates);
                    endTime = System.currentTimeMillis();
                    WriteResults("\nNodes visited: " + nodeCount +
                            "\nTotal execution time: " + (endTime - startTime) +
                            "\n\n", place);

                    System.out.println("\nTotal execution time: " + (endTime - startTime));
                    if (nodeCount == -1) break;
                }
            }
        }
        dataset.end();
    }

    private static List<String> readQueryWords(String fileName) {
        List<String> queryWords = new ArrayList<String>();
        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for(String line; (line = br.readLine()) != null; ) {
                if(!line.isEmpty()) {
                    if (fileName.contains("place")) {
                        queryWords.add(line.split(" ")[0]);
                    }
                    else if (fileName.contains("dates")) {
                        queryWords.add(line);
                    }
                    else{
                        queryWords.add(line.split(" ")[0].toLowerCase());
                    }
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
                "PREFIX yago:<http://yago-knowledge.org/resource/> "+
                        "SELECT DISTINCT ?s WHERE { ?s ?p yago:" + place + " . " +
                        "FILTER ( ?p =  <http://yago-knowledge.org/resource/isLocatedIn> ) . " +
                        "}";

        try {
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    if (soln == null) continue;
                    RDFNode sub = soln.get("s");
                    if (sub == null || !sub.isURIResource()) continue;
                    roots.add(new YagoNode(sub.toString()));
                }
            } catch (NoSuchElementException | NullPointerException e) {
                System.out.println("result error");
                System.out.println(e);
            }
        } catch (NullPointerException e) {
            System.out.println("Root outer error");
            System.out.println(queryString);
            System.out.println(e);
        }
        return roots;
    }

    public static List<YagoNode> getTemporalNodes (Model model, String date) {
        List<YagoNode> roots = new ArrayList<YagoNode>();
        String dateString = "?date >= \"" + date.split(" ")[0] +"\"^^xsd:date && ?date <= \"" + date.split(" ")[1] + "\"^^xsd:date";

        String queryString = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " +
            "PREFIX yago:<http://yago-knowledge.org/resource/> " +
            "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema> " +
            "SELECT DISTINCT ?s " +
            "WHERE {" +
                "VALUES ?p {yago:wasCreatedOnDate yago:wasDestroyedOnDate} " +
                "?s ?p ?date . " +
                "FILTER (" + dateString + " && ?p != rdfs:label)" +
            "}";

        try {
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet resultSet = qexec.execSelect();
                while (resultSet.hasNext()) {
                    try {
                    QuerySolution soln = resultSet.nextSolution();
                    if (soln == null) continue;
                    RDFNode sub = soln.get("s");
                    if (sub == null || !sub.isURIResource()) continue;
                    roots.add(new YagoNode(sub.toString()));
                    } catch (NullPointerException e) {
                        System.out.println(e);
                    }
                }
            } catch (QueryParseException | NullPointerException e) {
                System.out.println("time loop");
                System.out.println(e);
            }
        } catch (QueryParseException e) {
            System.out.println("Time query parse error");
            System.out.println(queryString);
            System.out.println(e);
        }

        return roots;
    }

    public static int traverseStart(Model model, HashSet<String> queryWords, String place, String dates) {
        List<YagoNode> spatialRoots = getRoots(model, place);

        List<YagoNode> roots = new ArrayList<YagoNode>();
        for (YagoNode r : spatialRoots) {
            List<YagoNode> spatiotemporal = temporalTraverse(model, r, dates);
            if (spatiotemporal != null) {
                roots.addAll(spatiotemporal);
            }
        }

        List<YagoNode> nodes = new ArrayList<YagoNode>();
        int maxDepth = 2;
        int nodeCount = 0;
        HashSet<YagoNode> hitNodes = new HashSet<>();

        WriteResults("\nRoot nodes Found: " + roots.size(), place);
        for (YagoNode root : roots) {
            nodes.clear();
            nodes.add(root);
            for (String word : queryWords) {
                if (root.getTokenList().contains(word)) {
                    root.addNodeMatchWord(word);
                }
            }
            while (!nodes.isEmpty()) {
                nodeCount += 1;
                YagoNode node = nodes.get(0);

                if (node.getDepth() > maxDepth) {
                    nodes.remove(0);
                    continue;
                }
                for (String word : queryWords) {
                    if (node.getTokenList().contains(word)) {
                        node.addNodeMatchWord(word);
                        node.addHitChild(node);
                    }
                }
                if (node.getTokenList().equals(queryWords)) maxDepth = node.getDepth();
                List<YagoNode> newNodes = traverse(model, node, queryWords);
                if (newNodes != null) {
                    nodes.addAll(newNodes);
                }
                nodes.remove(0);
            }
            findMinSubgraph(root, queryWords, place);
        }
        return nodeCount;
    }

    public static ArrayList<YagoNode> traverse (Model model, YagoNode yagoNode, HashSet<String> queryWords) {
        boolean followHits = true;
        String entity = yagoNode.getNodeData();
        ArrayList<YagoNode> children = new ArrayList<YagoNode>();
        if (entity == null) return null;
        if (entity.contains("<") || entity.contains(">") || entity.contains("\"") || entity.contains(" ")) return null;

        String queryString 	= "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema> " +
                "SELECT DISTINCT ?p ?o WHERE { " +
                "<" + entity + "> ?p ?o . " +
                "FILTER ( ?p != rdf:type && ?p != rdfs:label ) " +
                "}";

        try {
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    RDFNode sub = soln.get("o");
                    if (sub == null || !sub.isURIResource()) continue;
                    String str = sub.toString();
                    String[] uriSplit = str.split("/");
                    String[] tokens = uriSplit[uriSplit.length-1].replaceAll("[,()]", "").toLowerCase().split("_");
                    if (!followHits) {
                        children.add(new YagoNode(yagoNode, str));
                    } else {
                        for (String word : queryWords) {
                            if (Arrays.asList(tokens).contains(word)) {
                                children.add(new YagoNode(yagoNode, str));
                                break;
                            }
                        }
                    }
                }
            } catch (QueryParseException | NullPointerException e) {
            //    System.out.println(e);
            }
        } catch (QueryParseException e) {
            System.out.println(queryString);
            return null;
        }
        return children;
    }
    
    public static ArrayList<YagoNode> temporalTraverse(Model model, YagoNode yagoNode, String dates) {
        String entity = yagoNode.getNodeData();
        ArrayList<YagoNode> children = new ArrayList<YagoNode>();
        Integer startDate = Integer.parseInt(dates.split(" ")[0].replace("-", ""));
        Integer endDate = Integer.parseInt(dates.split(" ")[1].replace("-", ""));
        String dateStringHigh = "";
        if (entity == null) return null;
        if (entity.contains("<") || entity.contains(">") || entity.contains("\"") || entity.contains(" ")) return null;

        String queryString 	= "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema> " +
                "SELECT DISTINCT * WHERE { " +
                "<" + entity + "> ?p ?o . " +
                "} ";

        try {
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    RDFNode sub = soln.get("o");
                    if (sub == null) continue;
                    String str = sub.toString();
                    if (str.contains("^^http://www.w3.org/2001/XMLSchema#date")){
                        String dateStringLow = str.replace("-", "").replace("##", "01").replace("^", "_").split("_")[0];
                        if (str.contains("##-##-##")) {
                            continue;
                        }
                        else if (str.contains("##-##")) {
                            dateStringHigh = str.replace("-", "").replace("####", "1231").replace("^", "_").split("_")[0];
                        } else {
                            dateStringHigh = str.replace("-", "").replace("##", "30").replace("^", "_").split("_")[0];
                        }
                        try{
                            Integer dateLow = Integer.parseInt(dateStringLow);
                            Integer dateHigh = Integer.parseInt(dateStringHigh);
                            if (dateLow <= endDate && dateHigh >= startDate){
                                children.add(yagoNode);
                            }
                        } catch(NumberFormatException e) {
                            System.out.println(e);
                            System.out.println(str);
                            System.out.println(yagoNode);
                        }
                    }
                }
            } catch (QueryParseException | NullPointerException e) {
               System.out.println(e);
            }
        } catch (QueryParseException e) {
            System.out.println(queryString);
            return null;
        }
        return children;
    }

    public static void findMinSubgraph (YagoNode rootNode, HashSet<String> queryWords, String place) {
        HashSet<YagoNode> minTree = new HashSet<>();
        HashSet<YagoNode> parentList = new HashSet<>();
        HashSet<YagoNode> removeNodes = new HashSet<>();
        YagoNode removeMin = null;
        final AtomicBoolean newMin = new AtomicBoolean(false);
        minTree.add(rootNode);

        if (rootNode.getTokenList().containsAll(queryWords)) {
            rankSubGraphs(minTree, queryWords, place);
        }
        else if (rootNode.getHitChildren().size() == 0) {
            return;
        }
        else {
            for (YagoNode newNode : rootNode.getHitChildren()) {
                if (newNode.getNodeMatchWords().isEmpty()) {
                    continue;
                }
                newMin.set(false);
                if (minTree.isEmpty()) minTree.add(newNode);
                for (YagoNode min : minTree) {
                    if (min.getNodeMatchWords().isEmpty()) {
                        removeNodes.add(min);
                    }
                    if (min.getNodeMatchWords().containsAll(newNode.getNodeMatchWords()) &&
                            min.getDepth() < newNode.getDepth()) {
                        newMin.set(false);
                        break;
                    }
                    else if (min.getNodeMatchWords().containsAll(newNode.getNodeMatchWords()) &&
                            min.getDepth() >= newNode.getDepth() && min != newNode) {
                        removeNodes.add(min);
                        newMin.set(true);
                        continue;
                    }
                    if (!min.getNodeMatchWords().containsAll(newNode.getNodeMatchWords())) {
                        removeMin = min;
                        newMin.set(true);
                    }
                }
                if (newMin.get()) {
                    if (removeMin != null) {
                        removeMin.getNodeMatchWords().removeAll(newNode.getNodeMatchWords());
                    }
                    minTree.add(newNode);
                }
                minTree.removeAll(removeNodes);
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
            hitRate += node.getNodeMatchWords().size();
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

    public static void WriteResults(String s, String place) {
        try {
            Files.write(Paths.get("results/" + place + ".txt"), s.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
