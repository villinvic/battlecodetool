package test1;
import battlecode.common.MapLocation;

import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

public class Node {
    private MapLocation loc;

    private List<Node> shortestPath = new LinkedList<>();

    double weight;

    Map<Node, Double> adjacentNodes = new HashMap<>();

    public void addDestination(Node destination) {
        adjacentNodes.put(destination, destination.weight);
        destination.adjacentNodes.put(this, weight);
    }

    public Node(MapLocation loc, double passability){
        this.weight = 1.0/passability;
        this.loc = loc;
    }

    // getters and setters
}