package com.familytree.relationship;

import com.familytree.graph.FamilyGraph;
import com.familytree.model.Person;
import com.familytree.model.Relationship;
import org.springframework.stereotype.Component;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 关系计算器主入口。
 * 负责加载数据、构建图、并协调分类器。
 */
@Component
public class RelationshipCalculator {

    private static final int MAX_GENERATIONS = 7;

    private FamilyGraph graph;
    private RelationshipClassifier classifier;

    /**
     * 从数据库数据构建内存图
     */
    public void buildGraph(List<Person> persons, List<Relationship> relationships) {
        this.graph = FamilyGraph.buildFrom(persons, relationships);
        this.classifier = new RelationshipClassifier(graph);
    }

    public FamilyGraph getGraph() {
        return graph;
    }

    /**
     * 计算 personA 与 personB 的关系（从 A 的视角看 B）
     */
    public RelationshipResult calculate(Long personA, Long personB) {
        if (graph == null || classifier == null) {
            throw new IllegalStateException("Graph not built yet. Call buildGraph() first.");
        }

        if (!graph.hasPerson(personA) || !graph.hasPerson(personB)) {
            return new RelationshipResult("人物不存在", 0, "UNKNOWN");
        }

        return classifier.calculate(personA, personB, MAX_GENERATIONS);
    }

    /**
     * 获取图谱数据（Cytoscape.js 格式）
     */
    public Map<String, Object> getGraphData() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // 节点
        for (Person p : graph.getAllPersons()) {
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", p.getId().toString());
            nodeData.put("label", p.getName());
            nodeData.put("gender", p.getGender().name());
            nodeData.put("entityType", p.getEntityType() != null ? p.getEntityType() : "");

            // 头像：优先用户上传，无则生成 SVG data URI 显示首字
            String avatar = p.getAvatar();
            String name = p.getName();
            if (avatar != null && !avatar.isEmpty()) {
                nodeData.put("avatar", "/uploads/avatars/" + avatar);
            } else {
                // 生成首字的 SVG data URI（Base64 编码，避免中文编码问题）
                char first = '?';
                if (name != null && !name.isEmpty()) {
                    char c = name.charAt(0);
                    first = (c >= 0x4E00 && c <= 0x9FFF) ? c : Character.toUpperCase(c);
                }
                String bgColor = p.getGender() == com.familytree.model.enums.Gender.MALE ? "409EFF" : "F56C6C";
                String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='52' height='52'>"
                    + "<circle cx='26' cy='26' r='26' fill='#" + bgColor + "'/>"
                    + "<text x='26' y='26' text-anchor='middle' dominant-baseline='central' "
                    + "fill='white' font-size='22' font-weight='bold'>" + first + "</text></svg>";
                String b64 = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
                nodeData.put("avatar", "data:image/svg+xml;base64," + b64);
            }

            Map<String, Object> node = new HashMap<>();
            node.put("data", nodeData);
            nodes.add(node);
        }

        // 边（只显示基础关系）
        Set<String> addedEdges = new HashSet<>();
        for (Person p : graph.getAllPersons()) {
            Long pId = p.getId();

            // 亲子关系
            Set<Long> children = graph.getChildren(pId);
            for (Long childId : children) {
                String edgeKey = pId + "-" + childId;
                if (!addedEdges.contains(edgeKey)) {
                    Person child = graph.getPerson(childId);
                    String label = p.getGender() == com.familytree.model.enums.Gender.MALE ? "父亲" : "母亲";
                    // 反向：从孩子角度看父母
                    String reverseLabel = "";
                    if (child != null) {
                        reverseLabel = child.getGender() == com.familytree.model.enums.Gender.MALE ? "儿子" : "女儿";
                    }
                    Map<String, Object> edgeData = new HashMap<>();
                    edgeData.put("id", "e-" + pId + "-" + childId);
                    edgeData.put("source", pId.toString());
                    edgeData.put("target", childId.toString());
                    edgeData.put("label", label);
                    edgeData.put("reverseLabel", reverseLabel);
                    edgeData.put("type", "PARENT_CHILD");
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("data", edgeData);
                    edges.add(edge);
                    addedEdges.add(edgeKey);
                }
            }

            // 配偶关系
            Long spouseId = graph.getSpouse(pId);
            if (spouseId != null) {
                String edgeKey = Math.min(pId, spouseId) + "-" + Math.max(pId, spouseId) + "-spouse";
                if (!addedEdges.contains(edgeKey)) {
                    String label = p.getGender() == com.familytree.model.enums.Gender.MALE ? "丈夫" : "妻子";
                    // 反向：从配偶角度看对方
                    Person spouse = graph.getPerson(spouseId);
                    String reverseLabel = "";
                    if (spouse != null) {
                        reverseLabel = spouse.getGender() == com.familytree.model.enums.Gender.MALE ? "丈夫" : "妻子";
                    }
                    Map<String, Object> edgeData = new HashMap<>();
                    edgeData.put("id", "es-" + pId + "-" + spouseId);
                    edgeData.put("source", pId.toString());
                    edgeData.put("target", spouseId.toString());
                    edgeData.put("label", label);
                    edgeData.put("reverseLabel", reverseLabel);
                    edgeData.put("type", "SPOUSE");
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("data", edgeData);
                    edges.add(edge);
                    addedEdges.add(edgeKey);
                }
            }
        }

        result.put("elements", Map.of("nodes", nodes, "edges", edges));
        return result;
    }

    /**
     * 新人物加入后，自动计算与所有人的关系
     */
    public Map<String, String> autoCalculateRelationships(Long newPersonId) {
        Map<String, String> relationships = new LinkedHashMap<>();

        for (Long existingId : graph.getAllPersonIds()) {
            if (existingId.equals(newPersonId)) continue;

            RelationshipResult result = calculate(newPersonId, existingId);
            Person existing = graph.getPerson(existingId);
            if (existing != null && !"UNKNOWN".equals(result.getCategory())) {
                relationships.put(existing.getName(), result.getLabel());
            }
        }

        return relationships;
    }
}
