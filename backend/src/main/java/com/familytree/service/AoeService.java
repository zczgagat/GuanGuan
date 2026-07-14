package com.familytree.service;

import com.familytree.model.Person;
import com.familytree.model.Relationship;
import com.familytree.model.enums.RelationshipType;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AoeService {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    public Map<String, Object> calculate(Long treeId) {
        List<Person> persons = personRepository.findByFamilyTreeId(treeId);
        List<Relationship> allRels = relationshipRepository.findAll();
        Set<Long> pids = persons.stream().map(Person::getId).collect(Collectors.toSet());

        // 过滤当前树的关系
        List<Relationship> edges = allRels.stream()
            .filter(r -> pids.contains(r.getPerson1().getId()) && pids.contains(r.getPerson2().getId()))
            .filter(r -> r.getType() == RelationshipType.CUSTOM)
            .collect(Collectors.toList());

        Map<Long, String> nodeNames = persons.stream().collect(Collectors.toMap(Person::getId, Person::getName));
        Map<Long, List<Edge>> outEdges = new HashMap<>();
        Map<Long, List<Edge>> inEdges = new HashMap<>();
        Set<Long> allNodes = new HashSet<>();

        for (Person p : persons) allNodes.add(p.getId());
        List<Edge> edgeList = new ArrayList<>();

        for (Relationship r : edges) {
            Long u = r.getPerson1().getId();
            Long v = r.getPerson2().getId();
            int d = r.getDuration() != null ? r.getDuration() : 0;
            String lbl = r.getCustomLabel();
            if (lbl == null || lbl.isEmpty() || "CUSTOM".equals(lbl)) lbl = "a" + (edgeList.size() + 1);
            Edge e = new Edge(u, v, d, lbl, r.getId());
            edgeList.add(e);
            outEdges.computeIfAbsent(u, k -> new ArrayList<>()).add(e);
            inEdges.computeIfAbsent(v, k -> new ArrayList<>()).add(e);
        }

        // 拓扑排序 (Kahn)
        Map<Long, Integer> inDeg = new HashMap<>();
        for (Long n : allNodes) inDeg.put(n, 0);
        for (Edge e : edgeList) inDeg.put(e.v, inDeg.getOrDefault(e.v, 0) + 1);

        List<Long> topo = new ArrayList<>();
        Queue<Long> q = new LinkedList<>();
        for (Long n : allNodes) if (inDeg.getOrDefault(n, 0) == 0) q.add(n);
        while (!q.isEmpty()) {
            Long u = q.poll();
            topo.add(u);
            for (Edge e : outEdges.getOrDefault(u, Collections.emptyList())) {
                inDeg.put(e.v, inDeg.get(e.v) - 1);
                if (inDeg.get(e.v) == 0) q.add(e.v);
            }
        }

        // 前推：最早开始/完成
        Map<Long, Integer> es = new HashMap<>();
        Map<Long, Integer> ef = new HashMap<>();
        for (Long n : allNodes) { es.put(n, 0); ef.put(n, 0); }
        for (Long u : topo) {
            int curEf = ef.getOrDefault(u, 0);
            for (Edge e : outEdges.getOrDefault(u, Collections.emptyList())) {
                int newEs = curEf;
                if (newEs > es.getOrDefault(e.v, 0)) {
                    es.put(e.v, newEs);
                }
                int newEf = newEs + e.d;
                if (newEf > ef.getOrDefault(e.v, 0)) {
                    ef.put(e.v, newEf);
                }
            }
        }

        // 后推：最晚开始/完成
        Map<Long, Integer> ls = new HashMap<>();
        Map<Long, Integer> lf = new HashMap<>();
        int projectEnd = ef.values().stream().max(Integer::compareTo).orElse(0);
        for (Long n : allNodes) { ls.put(n, projectEnd); lf.put(n, projectEnd); }
        List<Long> revTopo = new ArrayList<>(topo);
        Collections.reverse(revTopo);
        for (Long u : revTopo) {
            int curLs = ls.getOrDefault(u, projectEnd);
            for (Edge e : inEdges.getOrDefault(u, Collections.emptyList())) {
                int newLf = curLs;
                if (newLf < lf.getOrDefault(e.u, projectEnd)) {
                    lf.put(e.u, newLf);
                }
                int newLs = newLf - e.d;
                if (newLs < ls.getOrDefault(e.u, projectEnd)) {
                    ls.put(e.u, newLs);
                }
            }
        }

        // 关键路径
        List<String> criticalPath = new ArrayList<>();
        Set<String> criticalEdges = new HashSet<>();
        for (Edge e : edgeList) {
            int slack = ls.get(e.v) - e.d - ef.get(e.u);
            if (slack == 0) {
                criticalEdges.add(e.id.toString());
                if (criticalPath.isEmpty()) criticalPath.add(nodeNames.getOrDefault(e.u, e.u.toString()));
                criticalPath.add(e.label);
                criticalPath.add(nodeNames.getOrDefault(e.v, e.v.toString()));
            }
        }

        // 节点信息
        List<Map<String, Object>> nodeInfos = new ArrayList<>();
        for (Long n : topo) {
            Map<String, Object> ni = new LinkedHashMap<>();
            ni.put("id", n);
            ni.put("name", nodeNames.getOrDefault(n, "节点" + n));
            ni.put("es", es.getOrDefault(n, 0));
            ni.put("ef", ef.getOrDefault(n, 0));
            ni.put("ls", ls.getOrDefault(n, 0));
            ni.put("lf", lf.getOrDefault(n, 0));
            ni.put("slack", ls.getOrDefault(n, 0) - es.getOrDefault(n, 0));
            ni.put("isCritical", Objects.equals(es.get(n), ls.get(n)));
            nodeInfos.add(ni);
        }

        // 边信息
        List<Map<String, Object>> edgeInfos = new ArrayList<>();
        for (Edge e : edgeList) {
            Map<String, Object> ei = new LinkedHashMap<>();
            ei.put("id", e.id);
            ei.put("label", e.label);
            ei.put("from", nodeNames.getOrDefault(e.u, e.u.toString()));
            ei.put("to", nodeNames.getOrDefault(e.v, e.v.toString()));
            ei.put("duration", e.d);
            ei.put("isCritical", criticalEdges.contains(e.id.toString()));
            edgeInfos.add(ei);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topoSort", topo.stream().map(n -> nodeNames.getOrDefault(n, n.toString())).collect(Collectors.toList()));
        result.put("projectDuration", projectEnd);
        result.put("criticalPath", criticalPath);
        result.put("criticalEdgeIds", new ArrayList<>(criticalEdges));
        result.put("nodes", nodeInfos);
        result.put("edges", edgeInfos);
        return result;
    }

    static class Edge {
        Long u, v, id;
        int d;
        String label;
        Edge(Long u, Long v, int d, String label, Long id) {
            this.u = u; this.v = v; this.d = d; this.label = label; this.id = id;
        }
    }
}
