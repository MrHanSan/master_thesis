package master.ntnu;

import java.util.Arrays;
import java.util.HashSet;

public class YagoNode {
    private YagoNode parent;
    private HashSet<YagoNode> hitChildren;
    private String nodeData;
    private int depth;
    private HashSet<String> nodeMatchWords;
    private HashSet<String> tokenList;

    public YagoNode(String nodeData) {
        this.nodeData = nodeData;
        this.tokenList = new HashSet<>();
        String[] a = nodeData.split("/");
        this.tokenList.addAll(Arrays.asList(a[a.length-1].replaceAll("[,()]", "").toLowerCase().split("_")));
        depth = 0;
        hitChildren = new HashSet<YagoNode>();
        nodeMatchWords = new HashSet<String>();
    }

    public YagoNode(YagoNode parent, String nodeData) {
        this.parent = parent;
        this.nodeData = nodeData;
        this.tokenList = new HashSet<>();
        String[] a = nodeData.split("/");
        this.tokenList.addAll(Arrays.asList(a[a.length-1].replaceAll("[,()]", "").toLowerCase().split("_")));
        depth = parent.getDepth()+1;
        hitChildren = parent.hitChildren;
        nodeMatchWords = new HashSet<String>();
    }

    public void addHitChild(YagoNode child) {
        this.hitChildren.add(child);
    }

    public HashSet<YagoNode> getHitChildren() {
        return hitChildren;
    }

    public YagoNode getParent() {
        return parent;
    }

    public String getNodeData() {
        return nodeData;
    }

    public int getDepth() {
        return depth;
    }

    public void addNodeMatchWord(String word) {
        nodeMatchWords.add(word);
    }

    public void removeNodeMatchWord(String word) {
        nodeMatchWords.remove(word);
    }

    public HashSet<String> getNodeMatchWords() {
        return nodeMatchWords;
    }

    public HashSet<String> getTokenList() {
        return tokenList;
    }
}
