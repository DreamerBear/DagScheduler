package com.wts.dag.scheduler;

import java.util.*;

/**
 * @Package com.wts.dag.scheduler
 * @author: xuchao（xuchao.xxc@ncarzone.com）
 * @date: 2023/2/22 下午2:36
 */
public class MyDAG {

    public static void requireNotNull(Object object) {
        if (object == null) {
            throw new IllegalArgumentException();
        }
    }

    public static void requireNoneNull(Object... objects) {
        if (objects == null) {
            throw new IllegalArgumentException();
        }
        for (Object object : objects) {
            requireNotNull(object);
        }
    }

    public static void main(String[] args) {
        Graph graph = new Graph();
        graph.addEdge(new Vertex("1"), new Vertex("2"));
        graph.addEdge(new Vertex("2"), new Vertex("3"));
        graph.addEdge(new Vertex("1"), new Vertex("4"));
        graph.addEdge(new Vertex("0"), new Vertex("3"));
        graph.addEdge(new Vertex("3"), new Vertex("5"));
        graph.addEdge(new Vertex("3"), new Vertex("8"));
        graph.addEdge(new Vertex("4"), new Vertex("5"));
        graph.addEdge(new Vertex("5"), new Vertex("9"));
        graph.addEdge(new Vertex("8"), new Vertex("9"));

        System.out.println(graph.edges.size());
        System.out.println(graph);
        System.out.println(graph.bfsTopologicalSort());
        System.out.println(graph.dfsTopologicalSort());
    }

    public static class Vertex {
        private final String name;

        public Vertex(String name) {
            requireNotNull(name);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return Objects.equals(name, vertex.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "Vertex{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    public static class Edge {
        private final Vertex from;
        private final Vertex to;

        public Edge(Vertex from, Vertex to) {
            requireNoneNull(from, to);
            this.from = from;
            this.to = to;
        }

        public Vertex getFrom() {
            return from;
        }

        public Vertex getTo() {
            return to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return Objects.equals(from, edge.from) &&
                    Objects.equals(to, edge.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "from=" + from +
                    ", to=" + to +
                    '}';
        }
    }

    public static class Graph {
        private final Set<Vertex> vertices = new HashSet<>();
        private final Set<Edge> edges = new HashSet<>();
        private final Map<Vertex, Set<Edge>> incommingEdgeMap = new HashMap<>();
        private final Map<Vertex, Set<Edge>> outcommingEdgeMap = new HashMap<>();

        public boolean addVertex(Vertex vertex) {
            requireNotNull(vertex);
            return vertices.add(vertex);
        }

        public boolean removeVertex(Vertex vertex) {
            requireNotNull(vertex);
            boolean removed = vertices.remove(vertex);
            if (removed) {
                Set<Edge> incommingEdges = incommingEdgeMap.get(vertex);
                if (incommingEdges != null) {
                    incommingEdges.forEach(edges::remove);
                }
                Set<Edge> outcommingEdges = outcommingEdgeMap.get(vertex);
                if (outcommingEdges != null) {
                    outcommingEdges.forEach(edges::remove);
                }
            }
            return removed;
        }

        public void addEdge(Vertex from, Vertex to) {
            requireNoneNull(from, to);
            addVertex(from);
            addVertex(to);
            Edge edge = new Edge(from, to);
            edges.add(edge);
            incommingEdgeMap.computeIfAbsent(to, k -> new HashSet<>()).add(edge);
            outcommingEdgeMap.computeIfAbsent(from, k -> new HashSet<>()).add(edge);
        }

        public void removeEdge(Vertex from, Vertex to) {
            requireNoneNull(from, to);
            Edge edge = new Edge(from, to);
            edges.remove(edge);
            incommingEdgeMap.computeIfAbsent(to, k -> new HashSet<>()).remove(edge);
            outcommingEdgeMap.computeIfAbsent(from, k -> new HashSet<>()).remove(edge);
        }


        private List<Vertex> dfsTopologicalSort() {
            LinkedHashSet<Vertex> result = new LinkedHashSet<>();
            for (Vertex vertex : vertices) {
                doDfsTopologicalSort(vertex, result);
            }
            return new ArrayList<>(result);
        }

        private void doDfsTopologicalSort(Vertex vertex, LinkedHashSet<Vertex> result) {
            if (result.contains(vertex)) {
                return;
            }
            Set<Edge> imcommingEdges = incommingEdgeMap.get(vertex);
            if (imcommingEdges != null) {
                for (Edge imcommingEdge : imcommingEdges) {
                    doDfsTopologicalSort(imcommingEdge.getFrom(), result);
                }
            }
            result.add(vertex);
        }

        private List<Vertex> bfsTopologicalSort() {
            List<Vertex> result = new ArrayList<>();
            Map<Vertex, Integer> degreeMap = new HashMap<>();
            for (Vertex vertex : vertices) {
                degreeMap.put(vertex, Optional.ofNullable(incommingEdgeMap.get(vertex)).map(Set::size).orElse(0));
            }
            Queue<Vertex> queue = new LinkedList<>();
            degreeMap.forEach(((vertex, degree) -> {
                if (degree == 0) {
                    queue.offer(vertex);
                }
            }));
            while (!queue.isEmpty()) {
                int queueSize = queue.size();
                for (int i = 0; i < queueSize; i++) {
                    Vertex vertex = queue.poll();
                    degreeMap.remove(vertex);
                    result.add(vertex);
                    Set<Edge> outcommingEges = outcommingEdgeMap.get(vertex);
                    if (outcommingEges != null) {
                        for (Edge outcommingEge : outcommingEges) {
                            Integer degree = degreeMap.get(outcommingEge.getTo());
                            if (degree != null) {
                                degreeMap.put(outcommingEge.getTo(), degree - 1);
                            }
                        }
                    }
                }
                degreeMap.forEach(((vertex, degree) -> {
                    if (degree == 0) {
                        queue.offer(vertex);
                    }
                }));
            }
            return result;
        }
    }
}
