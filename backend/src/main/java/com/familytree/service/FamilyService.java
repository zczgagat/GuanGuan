package com.familytree.service;

import com.familytree.model.ComputedRelationship;
import com.familytree.model.FamilyTree;
import com.familytree.model.Person;
import com.familytree.model.Relationship;
import com.familytree.model.enums.RelationshipType;
import com.familytree.relationship.RelationshipCalculator;
import com.familytree.relationship.RelationshipResult;
import com.familytree.repository.ComputedRelationshipRepository;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 家族业务服务。
 * 负责管理人物、关系，并在数据变更后自动计算五代内的所有衍生关系。
 */
@Service
public class FamilyService {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private ComputedRelationshipRepository computedRelationshipRepository;

    @Autowired
    private FamilyTreeRepository familyTreeRepository;

    @Autowired
    private RelationshipCalculator relationshipCalculator;

    private Long currentFamilyTreeId = null;

    @PostConstruct
    public void init() {
        rebuildGraph();
    }

    /** 设置当前操作的家族树 ID */
    public void setCurrentTree(Long treeId) {
        this.currentFamilyTreeId = treeId;
    }

    public void rebuildGraph() {
        rebuildGraph(currentFamilyTreeId);
    }

    public void rebuildGraph(Long familyTreeId) {
        List<Person> allPersons = familyTreeId != null
                ? personRepository.findByFamilyTreeId(familyTreeId)
                : personRepository.findAll();
        // 加载关系：只加载当前树人物的关系
        List<Relationship> allRelationships = relationshipRepository.findAll();
        if (familyTreeId != null) {
            Set<Long> treePersonIds = allPersons.stream().map(Person::getId).collect(Collectors.toSet());
            allRelationships = allRelationships.stream()
                .filter(r -> treePersonIds.contains(r.getPerson1().getId())
                          && treePersonIds.contains(r.getPerson2().getId()))
                .collect(Collectors.toList());
        }
        relationshipCalculator.buildGraph(allPersons, allRelationships);
    }

    // ============ 人物管理 ============

    public List<Person> getAllPersons(Long familyTreeId) {
        if (familyTreeId != null) {
            return personRepository.findByFamilyTreeId(familyTreeId);
        }
        return personRepository.findAll();
    }

    public Person getPerson(Long id) {
        return personRepository.findById(id).orElse(null);
    }

    @Transactional
    public Person createPerson(String name, String gender, Long familyTreeId, String entityType) {
        com.familytree.model.enums.Gender g = "MALE".equalsIgnoreCase(gender)
                ? com.familytree.model.enums.Gender.MALE
                : com.familytree.model.enums.Gender.FEMALE;
        Person person = new Person(name, g);
        person.setFamilyTreeId(familyTreeId != null ? familyTreeId : 1L);
        if (entityType != null) person.setEntityType(entityType);
        person = personRepository.save(person);
        rebuildGraph();
        return person;
    }

    @Transactional
    public Person updatePerson(Long id, String name, String gender, String birthDate,
                                String hobby, String education, String profession, String address) {
        Person person = personRepository.findById(id).orElse(null);
        if (person == null) return null;

        if (name != null) person.setName(name);
        if (gender != null) {
            person.setGender("MALE".equalsIgnoreCase(gender)
                    ? com.familytree.model.enums.Gender.MALE
                    : com.familytree.model.enums.Gender.FEMALE);
        }
        if (birthDate != null && !birthDate.isEmpty()) {
            person.setBirthDate(java.time.LocalDate.parse(birthDate));
        }
        if (hobby != null) person.setHobby(hobby);
        if (education != null) person.setEducation(education);
        if (profession != null) person.setProfession(profession);
        if (address != null) person.setAddress(address);
        person = personRepository.save(person);
        rebuildGraph();
        return person;
    }

    @Transactional
    public void updateSiblingRank(Long id, Integer rank) {
        Person p = personRepository.findById(id).orElse(null);
        if (p != null) {
            p.setSiblingRank(rank);
            personRepository.save(p);
        }
        rebuildGraph();
    }

    @Transactional
    public void deletePerson(Long id) {
        List<Relationship> rels = relationshipRepository.findByPerson1IdOrPerson2Id(id, id);
        relationshipRepository.deleteAll(rels);
        computedRelationshipRepository.deleteByPersonAIdOrPersonBId(id, id);
        personRepository.deleteById(id);
        rebuildGraph();
    }

    @Transactional
    public void clearAll() {
        computedRelationshipRepository.deleteAll();
        relationshipRepository.deleteAll();
        personRepository.deleteAll();
        rebuildGraph();
    }

    // ============ 关系管理 ============

    public List<Map<String, Object>> getAllRelationships() {
        return relationshipRepository.findAll().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("person1", Map.of("id", r.getPerson1().getId(), "name", r.getPerson1().getName()));
            map.put("person2", Map.of("id", r.getPerson2().getId(), "name", r.getPerson2().getName()));
            map.put("type", r.getType().name());
            map.put("label", r.getType() == RelationshipType.CUSTOM ? r.getCustomLabel() : getRelationshipLabel(r.getType()));
            if (r.getType() == RelationshipType.CUSTOM) map.put("customLabel", r.getCustomLabel());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Relationship createRelationship(Long person1Id, Long person2Id, String type,
                                            String customLabel, String cardinality, Integer duration) {
        Person p1 = personRepository.findById(person1Id).orElse(null);
        Person p2 = personRepository.findById(person2Id).orElse(null);
        if (p1 == null || p2 == null) return null;

        RelationshipType relType = RelationshipType.valueOf(type.toUpperCase());
        Relationship relationship;
        if (relType == RelationshipType.CUSTOM && customLabel != null) {
            relationship = new Relationship(p1, p2, relType, customLabel);
        } else {
            relationship = new Relationship(p1, p2, relType);
        }
        if (cardinality != null) relationship.setCardinality(cardinality);
        if (duration != null) relationship.setDuration(duration);
        relationship = relationshipRepository.save(relationship);

        // 自定义关系不创建反向，不参与计算
        if (relType == RelationshipType.CUSTOM) {
            rebuildGraph();
            return relationship;
        }

        // 自动创建反向关系
        RelationshipType reverseType = getReverseType(relType, p2.getGender());
        if (reverseType != null) {
            List<Relationship> existing = relationshipRepository.findByPerson1IdOrPerson2Id(p2.getId(), p1.getId());
            boolean exists = existing.stream().anyMatch(r ->
                r.getPerson1().getId().equals(p2.getId()) &&
                r.getPerson2().getId().equals(p1.getId()) &&
                r.getType() == reverseType);
            if (!exists) {
                Relationship reverse = new Relationship(p2, p1, reverseType);
                relationshipRepository.save(reverse);
            }
        }

        // 自动建立夫妻关系：当某人的父亲和母亲都已定义时
        if (relType == RelationshipType.FATHER || relType == RelationshipType.MOTHER) {
            Long childId = p2.getId();
            Long newParentId = p1.getId();
            // 只查 person2=孩子的记录（即谁是谁的父亲/母亲），避免查到该人作为父亲的关系行
            List<Relationship> childRels = relationshipRepository.findByPerson2Id(childId);
            for (Relationship cr : childRels) {
                if (cr.getId().equals(relationship.getId())) continue; // 跳过刚保存的
                Long otherParentId = null;
                if (cr.getType() == RelationshipType.FATHER && relType != RelationshipType.FATHER) {
                    otherParentId = cr.getPerson1().getId();
                } else if (cr.getType() == RelationshipType.MOTHER && relType != RelationshipType.MOTHER) {
                    otherParentId = cr.getPerson1().getId();
                }
                if (otherParentId == null || otherParentId.equals(newParentId)) continue;

                // 检查两个父母之间是否已有配偶关系
                boolean hasSpouse = false;
                List<Relationship> parentRels = relationshipRepository.findByPerson1IdOrPerson2Id(newParentId, otherParentId);
                for (Relationship pr : parentRels) {
                    if ((pr.getType() == RelationshipType.HUSBAND || pr.getType() == RelationshipType.WIFE) &&
                        ((pr.getPerson1().getId().equals(newParentId) && pr.getPerson2().getId().equals(otherParentId)) ||
                         (pr.getPerson1().getId().equals(otherParentId) && pr.getPerson2().getId().equals(newParentId)))) {
                        hasSpouse = true;
                        break;
                    }
                }
                if (!hasSpouse) {
                    Person otherParent = personRepository.findById(otherParentId).orElse(null);
                    if (otherParent != null) {
                        Person husband = p1.getGender() == com.familytree.model.enums.Gender.MALE ? p1 : otherParent;
                        Person wife = p1.getGender() == com.familytree.model.enums.Gender.MALE ? otherParent : p1;
                        relationshipRepository.save(new Relationship(husband, wife, RelationshipType.HUSBAND));
                        relationshipRepository.save(new Relationship(wife, husband, RelationshipType.WIFE));
                    }
                }
            }
        }

        rebuildGraph();
        return relationship;
    }

    @Transactional
    public void deleteRelationship(Long id) {
        relationshipRepository.deleteById(id);
        rebuildGraph();
    }

    // ============ 公开的事务安全方法 ============

    /**
     * 触发自动计算（在独立事务中运行）
     */
    public void triggerAutoCompute() {
        try {
            doAutoCompute();
        } catch (Exception e) {
            System.err.println("[FamilyService] 自动计算关系失败(已忽略): " + e.getMessage());
        }
    }

    @Transactional
    protected void doAutoCompute() {
        // ER 图模板跳过计算
        if (currentFamilyTreeId != null) {
            FamilyTree tree = familyTreeRepository.findById(currentFamilyTreeId).orElse(null);
            if (tree != null && ("er".equals(tree.getTemplate()) || "aoe".equals(tree.getTemplate()))) return;
        }
        // 先补全所有基础关系的反向关系
        ensureReverseRelationships();

        computedRelationshipRepository.deleteAll();
        List<Person> allPersons = currentFamilyTreeId != null
                ? personRepository.findByFamilyTreeId(currentFamilyTreeId)
                : personRepository.findAll();
        if (allPersons.size() < 2) return;

        Set<String> computedPairs = new HashSet<>();
        for (int i = 0; i < allPersons.size(); i++) {
            for (int j = i + 1; j < allPersons.size(); j++) {
                Long idA = allPersons.get(i).getId();
                Long idB = allPersons.get(j).getId();

                String pairKey = idA + "-" + idB;
                if (computedPairs.contains(pairKey)) continue;
                computedPairs.add(pairKey);

                try {
                    RelationshipResult result = relationshipCalculator.calculate(idA, idB);
                    if (result == null) continue;

                    String label = result.getLabel();
                    String category = result.getCategory();
                    if (label == null || "无关系".equals(label) || "UNKNOWN".equals(category)) continue;

                    ComputedRelationship cr = new ComputedRelationship(
                            allPersons.get(i), allPersons.get(j),
                            label, category, result.getGenerationDiff());
                    computedRelationshipRepository.save(cr);
                } catch (Exception e) {
                    // 单对计算失败不影响其他
                }
            }
        }
    }

    // ============ 关系计算 ============

    public RelationshipResult calculateRelationship(Long person1Id, Long person2Id) {
        return relationshipCalculator.calculate(person1Id, person2Id);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getGraphData() {
        Map<String, Object> baseGraph = relationshipCalculator.getGraphData();
        if (baseGraph == null || baseGraph.get("elements") == null) {
            return baseGraph != null ? baseGraph : Map.of("elements", Map.of("nodes", List.of(), "edges", List.of()));
        }
        Map<String, Object> elements = (Map<String, Object>) baseGraph.get("elements");
        List<Map<String, Object>> baseEdges = (List<Map<String, Object>>) elements.get("edges");
        if (baseEdges == null) {
            baseEdges = new ArrayList<>();
            elements.put("edges", baseEdges);
        }

        // 先添加自定义关系边（ER 图等非族谱场景，异常不影响后续）
        Set<String> existingEdgeKeys = new HashSet<>();
        for (Map<String, Object> edge : baseEdges) {
            Map<String, Object> data = (Map<String, Object>) edge.get("data");
            existingEdgeKeys.add(data.get("source") + "-" + data.get("target"));
            existingEdgeKeys.add(data.get("target") + "-" + data.get("source"));
        }

        // ===== 先处理自定义边（ER 图） =====
        try {
            Set<Long> treePids = relationshipCalculator.getGraph().getAllPersonIds();
            for (Relationship r : relationshipRepository.findAll()) {
                if (r.getType() != com.familytree.model.enums.RelationshipType.CUSTOM) continue;
                if (!treePids.contains(r.getPerson1().getId()) || !treePids.contains(r.getPerson2().getId())) continue;
                String aId = r.getPerson1().getId().toString();
                String bId = r.getPerson2().getId().toString();
                String ek = aId + "-" + bId;
                if (existingEdgeKeys.contains(ek) || aId.equals(bId)) continue;
                String lbl = r.getCustomLabel();
                if (lbl == null || lbl.isEmpty()) lbl = r.getCardinality();
                if (lbl == null || lbl.isEmpty()) lbl = "_";
                Map<String, Object> ed = new HashMap<>();
                ed.put("id", "cust-" + r.getId());
                ed.put("source", aId); ed.put("target", bId);
                ed.put("label", lbl); ed.put("type", "CUSTOM");
                if (r.getCardinality() != null) ed.put("cardinality", r.getCardinality());
                if (r.getDuration() != null) ed.put("duration", r.getDuration());
                baseEdges.add(Map.of("data", ed));
                existingEdgeKeys.add(ek);
            }
        } catch (Exception e) { /* 自定义边失败不影响 */ }

        // ===== 再处理计算关系边（族谱） =====
        List<Map<String, Object>> computedEdges = new ArrayList<>();
        List<ComputedRelationship> computedRels;
        try {
            if (currentFamilyTreeId != null) {
                Set<Long> treePersonIds = relationshipCalculator.getGraph().getAllPersonIds();
                computedRels = computedRelationshipRepository.findAll().stream()
                    .filter(cr -> treePersonIds.contains(cr.getPersonA().getId())
                               && treePersonIds.contains(cr.getPersonB().getId()))
                    .collect(Collectors.toList());
            } else {
                computedRels = computedRelationshipRepository.findAll();
            }
        } catch (Exception e) {
            // 计算关系表不存在等异常，跳过
            computedRels = List.of();
        }

        for (ComputedRelationship cr : computedRels) {
            String aId = cr.getPersonA().getId().toString();
            String bId = cr.getPersonB().getId().toString();
            String edgeKey = aId + "-" + bId;

            if (existingEdgeKeys.contains(edgeKey)) continue;
            if (aId.equals(bId)) continue;
            String label = cr.getLabel();
            if (label == null || label.contains("(") || "无关系".equals(label)) continue;

            // 计算反向关系标签（用于选中目标节点时显示）
            String reverseLabel = "";
            try {
                Map<Long, Person> personMap = new HashMap<>();
                for (Person p : personRepository.findAll()) personMap.put(p.getId(), p);
                RelationshipResult rev = relationshipCalculator.calculate(
                        Long.parseLong(bId), Long.parseLong(aId));
                if (rev != null && rev.getLabel() != null && !"无关系".equals(rev.getLabel())) {
                    reverseLabel = rev.getLabel();
                }
            } catch (Exception e) { /* 使用空值 */ }

            Map<String, Object> edgeData = new HashMap<>();
            edgeData.put("id", "ce-" + cr.getId());
            edgeData.put("source", aId);
            edgeData.put("target", bId);
            edgeData.put("label", label);
            edgeData.put("reverseLabel", reverseLabel);
            edgeData.put("type", "COMPUTED");
            edgeData.put("category", cr.getCategory());

            Map<String, Object> edge = new HashMap<>();
            edge.put("data", edgeData);
            computedEdges.add(edge);
        }

        baseEdges.addAll(computedEdges);

        return baseGraph;
    }

    public List<Map<String, Object>> getComputedRelationships() {
        return computedRelationshipRepository.findAll().stream().map(cr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cr.getId());
            map.put("personA", Map.of("id", cr.getPersonA().getId(), "name", cr.getPersonA().getName()));
            map.put("personB", Map.of("id", cr.getPersonB().getId(), "name", cr.getPersonB().getName()));
            map.put("label", cr.getLabel());
            map.put("category", cr.getCategory());
            map.put("generationDiff", cr.getGenerationDiff());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateComputedRelationship(Long id, String label) {
        ComputedRelationship cr = computedRelationshipRepository.findById(id).orElse(null);
        if (cr != null) {
            cr.setLabel(label);
            computedRelationshipRepository.save(cr);
        }
    }

    // ============ 添加人物并自动关联 ============

    @Transactional
    public Map<String, Object> addPersonWithConnection(String name, String gender,
                                                        Long connectedPersonId, String relationType,
                                                        Long familyTreeId) {
        com.familytree.model.enums.Gender g = "MALE".equalsIgnoreCase(gender)
                ? com.familytree.model.enums.Gender.MALE
                : com.familytree.model.enums.Gender.FEMALE;
        Person newPerson = new Person(name, g);
        newPerson.setFamilyTreeId(familyTreeId != null ? familyTreeId : 1L);
        newPerson = personRepository.save(newPerson);

        if (connectedPersonId != null && relationType != null) {
            createRelationship(newPerson.getId(), connectedPersonId, relationType, null, null, null);
        }

        rebuildGraph();
        return Map.of("person", newPerson);
    }

    @Transactional
    public Map<String, Object> createErRelation(Long entityA, Long entityB, String relName,
                                                  String cardA, String cardB, Long treeId) {
        // 创建关系节点（菱形）
        Person relNode = new Person(relName, com.familytree.model.enums.Gender.MALE);
        relNode.setFamilyTreeId(treeId != null ? treeId : 1L);
        relNode.setEntityType("relation");
        relNode = personRepository.save(relNode);

        // 创建 entityA → relation 边
        Relationship e1 = new Relationship(
            personRepository.findById(entityA).orElse(null),
            relNode, RelationshipType.CUSTOM, "");
        if (cardA != null) e1.setCardinality(cardA);
        relationshipRepository.save(e1);

        // 创建 relation → entityB 边
        Relationship e2 = new Relationship(
            relNode,
            personRepository.findById(entityB).orElse(null),
            RelationshipType.CUSTOM, "");
        if (cardB != null) e2.setCardinality(cardB);
        relationshipRepository.save(e2);

        rebuildGraph();
        return Map.of("relId", relNode.getId(), "message", "关系已创建");
    }

    // ============ 辅助方法 ============

    /**
     * 补全所有基础关系的反向关系（确保双向完整）
     */
    private void ensureReverseRelationships() {
        List<Relationship> allRels = relationshipRepository.findAll();
        for (Relationship r : allRels) {
            try {
                RelationshipType reverseType = getReverseType(r.getType(), r.getPerson2().getGender());
                if (reverseType == null) continue;

                // 检查反向是否已存在
                List<Relationship> existing = relationshipRepository.findByPerson1IdOrPerson2Id(
                        r.getPerson2().getId(), r.getPerson1().getId());
                boolean exists = existing.stream().anyMatch(er ->
                    er.getPerson1().getId().equals(r.getPerson2().getId()) &&
                    er.getPerson2().getId().equals(r.getPerson1().getId()) &&
                    er.getType() == reverseType);
                if (!exists) {
                    Relationship reverse = new Relationship(r.getPerson2(), r.getPerson1(), reverseType);
                    relationshipRepository.save(reverse);
                }
            } catch (Exception e) {
                // 单条失败不影响
            }
        }
    }

    private String getRelationshipLabel(RelationshipType type) {
        return switch (type) {
            case MOTHER -> "母亲";
            case FATHER -> "父亲";
            case SON -> "儿子";
            case DAUGHTER -> "女儿";
            case WIFE -> "妻子";
            case HUSBAND -> "丈夫";
            case CUSTOM -> "自定义";
        };
    }

    private RelationshipType getReverseType(RelationshipType type,
                                             com.familytree.model.enums.Gender p2Gender) {
        return switch (type) {
            case FATHER, MOTHER -> p2Gender == com.familytree.model.enums.Gender.MALE
                    ? RelationshipType.SON : RelationshipType.DAUGHTER;
            case SON, DAUGHTER -> p2Gender == com.familytree.model.enums.Gender.MALE
                    ? RelationshipType.FATHER : RelationshipType.MOTHER;
            case HUSBAND -> RelationshipType.WIFE;
            case WIFE -> RelationshipType.HUSBAND;
            case CUSTOM -> null;
        };
    }
}
