package master.ntnu;

import java.util.ArrayList;
import java.util.HashSet;

public class YagoNode {
    private YagoNode parent;
    private HashSet<YagoNode> hitChildren;
    private String nodeData;
    private int depth;
    private HashSet<String> matchWords;

    public YagoNode(String nodeData) {
        this.nodeData = nodeData;
        depth = 0;
        matchWords = new HashSet<String>();
        hitChildren = new HashSet<YagoNode>();
    }

    public YagoNode(YagoNode parent, String nodeData) {
        this.parent = parent;
        this.nodeData = nodeData;
        depth = parent.getDepth()+1;
        matchWords = parent.matchWords;
        hitChildren = parent.hitChildren;
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

    public HashSet<String> getWords() {
        return matchWords;
    }

    public void addWord(String word) {
        matchWords.add(word);
    }
}
