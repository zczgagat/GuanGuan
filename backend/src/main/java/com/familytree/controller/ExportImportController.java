package com.familytree.controller;

import com.familytree.model.*;
import com.familytree.model.enums.Gender;
import com.familytree.model.enums.RelationshipType;
import com.familytree.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ExportImportController {

    @Autowired private FamilyTreeRepository familyTreeRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private RelationshipRepository relationshipRepository;
    @Autowired private com.familytree.service.FamilyService familyService;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (Long) principal;
    }

    @GetMapping("/export-tree/{treeId}")
    public ResponseEntity<?> exportTree(@PathVariable Long treeId) {
        Long userId = getCurrentUserId();
        FamilyTree tree = familyTreeRepository.findById(treeId).orElse(null);
        if (tree == null || !tree.getUserId().equals(userId))
            return ResponseEntity.badRequest().body(Map.of("error", "图谱不存在"));

        List<Person> persons = personRepository.findByFamilyTreeId(treeId);
        Set<Long> personIds = new HashSet<>();
        persons.forEach(p -> personIds.add(p.getId()));

        List<Map<String, Object>> personData = new ArrayList<>();
        for (Person p : persons) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p.getName());
            m.put("gender", p.getGender().name());
            if (p.getBirthDate() != null) m.put("birthDate", p.getBirthDate().toString());
            if (p.getHobby() != null) m.put("hobby", p.getHobby());
            if (p.getEducation() != null) m.put("education", p.getEducation());
            if (p.getProfession() != null) m.put("profession", p.getProfession());
            if (p.getAddress() != null) m.put("address", p.getAddress());
            if (p.getSiblingRank() != null) m.put("siblingRank", p.getSiblingRank());
            if (p.getEntityType() != null) m.put("entityType", p.getEntityType());
            personData.add(m);
        }

        List<Map<String, Object>> relData = new ArrayList<>();
        for (Relationship r : relationshipRepository.findAll()) {
            if (!personIds.contains(r.getPerson1().getId()) || !personIds.contains(r.getPerson2().getId())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("person1Name", r.getPerson1().getName());
            m.put("person2Name", r.getPerson2().getName());
            m.put("type", r.getType().name());
            if (r.getCustomLabel() != null) m.put("customLabel", r.getCustomLabel());
            if (r.getCardinality() != null) m.put("cardinality", r.getCardinality());
            if (r.getDuration() != null) m.put("duration", r.getDuration());
            relData.add(m);
        }

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("treeName", tree.getName());
        export.put("treeDescription", tree.getDescription() != null ? tree.getDescription() : "");
        export.put("template", tree.getTemplate() != null ? tree.getTemplate() : "family");
        export.put("version", "1.0");
        export.put("exportTime", new Date().toString());
        export.put("persons", personData);
        export.put("relationships", relData);
        return ResponseEntity.ok(export);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/import-tree")
    public ResponseEntity<?> importTree(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        String treeName = (String) body.get("treeName");
        String treeDesc = (String) body.get("treeDescription");
        List<Map<String, String>> personsData = (List<Map<String, String>>) body.get("persons");
        List<Map<String, String>> relsData = (List<Map<String, String>>) body.get("relationships");

        if (treeName == null || personsData == null)
            return ResponseEntity.badRequest().body(Map.of("error", "数据格式错误"));

        FamilyTree tree = new FamilyTree(treeName);
        tree.setDescription(treeDesc);
        tree.setTemplate((String) body.get("template"));
        tree.setUserId(userId);
        tree = familyTreeRepository.save(tree);
        Long treeId = tree.getId();

        Map<String, Long> nameToId = new HashMap<>();
        for (Map<String, String> pd : personsData) {
            Person p = new Person(pd.get("name"),
                "MALE".equals(pd.get("gender")) ? Gender.MALE : Gender.FEMALE);
            p.setFamilyTreeId(treeId);
            String bd = pd.get("birthDate");
            if (bd != null && !bd.isEmpty()) p.setBirthDate(LocalDate.parse(bd));
            p.setHobby(pd.get("hobby"));
            p.setEducation(pd.get("education"));
            p.setProfession(pd.get("profession"));
            p.setAddress(pd.get("address"));
            Object srObj = pd.get("siblingRank");
            if (srObj != null) {
                if (srObj instanceof Number) p.setSiblingRank(((Number) srObj).intValue());
                else if (srObj instanceof String && !((String) srObj).isEmpty()) p.setSiblingRank(Integer.valueOf((String) srObj));
            }
            String et = pd.get("entityType");
            if (et != null && !et.isEmpty()) p.setEntityType(et);
            p = personRepository.save(p);
            nameToId.put(pd.get("name"), p.getId());
        }

        for (Map<String, String> rd : relsData) {
            Long p1Id = nameToId.get(rd.get("person1Name"));
            Long p2Id = nameToId.get(rd.get("person2Name"));
            if (p1Id == null || p2Id == null) continue;
            Person p1 = personRepository.findById(p1Id).orElse(null);
            Person p2 = personRepository.findById(p2Id).orElse(null);
            if (p1 == null || p2 == null) continue;
            try {
                Relationship r = new Relationship(p1, p2, RelationshipType.valueOf(rd.get("type")));
                if (rd.containsKey("customLabel")) r.setCustomLabel(rd.get("customLabel"));
                if (rd.containsKey("cardinality")) r.setCardinality(rd.get("cardinality"));
                if (rd.containsKey("duration")) {
                    Object durVal = rd.get("duration");
                    if (durVal instanceof Number) r.setDuration(((Number) durVal).intValue());
                    else if (durVal instanceof String) r.setDuration(Integer.valueOf((String) durVal));
                }
                relationshipRepository.save(r);
            } catch (Exception ex) {
                System.err.println("[Import] 创建关系失败: " + ex.getMessage());
            }
        }

        // 重建内存图并触发计算
        familyService.setCurrentTree(treeId);
        familyService.rebuildGraph();
        familyService.triggerAutoCompute();

        System.out.println("[Import] 导入完成: " + personsData.size() + " 人, " + relsData.size() + " 关系");
        return ResponseEntity.ok(Map.of("message", "导入成功", "treeId", treeId, "persons", personsData.size(), "rels", relsData.size()));
    }
}
