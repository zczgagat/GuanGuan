package com.familytree.controller;

import com.familytree.model.Person;
import com.familytree.model.Relationship;
import com.familytree.relationship.RelationshipResult;
import com.familytree.service.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FamilyController {

    @Autowired
    private FamilyService familyService;

    @Autowired
    private com.familytree.service.AoeService aoeService;

    // ============ 人物管理 ============

    @GetMapping("/persons")
    public List<Person> getAllPersons(@RequestParam(required = false) Long treeId) {
        familyService.setCurrentTree(treeId);
        return familyService.getAllPersons(treeId);
    }

    @GetMapping("/persons/{id}")
    public ResponseEntity<Person> getPerson(@PathVariable Long id) {
        Person p = familyService.getPerson(id);
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    @PostMapping("/persons")
    public Person createPerson(@RequestBody Map<String, String> body) {
        Long treeId = body.get("treeId") != null ? Long.valueOf(body.get("treeId")) : null;
        familyService.setCurrentTree(treeId);
        Person p = familyService.createPerson(body.get("name"), body.get("gender"), treeId, body.get("entityType"));
        familyService.triggerAutoCompute();
        return p;
    }

    @PutMapping("/persons/{id}")
    public ResponseEntity<Person> updatePerson(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Person p = familyService.updatePerson(
                id, body.get("name"), body.get("gender"), body.get("birthDate"),
                body.get("hobby"), body.get("education"), body.get("profession"), body.get("address"));
        return p != null ? ResponseEntity.ok(p) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        familyService.deletePerson(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/persons/{id}/sibling-rank")
    public ResponseEntity<Void> updateSiblingRank(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer rank = body.get("rank") != null ? Integer.valueOf(body.get("rank").toString()) : null;
        familyService.updateSiblingRank(id, rank);
        familyService.triggerAutoCompute();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/family/clear")
    public ResponseEntity<Void> clearAll() {
        familyService.clearAll();
        return ResponseEntity.ok().build();
    }

    // ============ 关系管理 ============

    @GetMapping("/relationships")
    public List<Map<String, Object>> getAllRelationships() {
        return familyService.getAllRelationships();
    }

    @PostMapping("/relationships")
    public ResponseEntity<?> createRelationship(@RequestBody Map<String, Object> body) {
        Long person1Id = Long.valueOf(body.get("person1Id").toString());
        Long person2Id = Long.valueOf(body.get("person2Id").toString());
        String type = (String) body.get("type");
        String customLabel = (String) body.get("customLabel");
        String cardinality = (String) body.get("cardinality");
        Integer duration = body.get("duration") != null ? Integer.valueOf(body.get("duration").toString()) : null;
        Long treeId = body.get("treeId") != null ? Long.valueOf(body.get("treeId").toString()) : null;

        familyService.setCurrentTree(treeId);
        Relationship r = familyService.createRelationship(person1Id, person2Id, type, customLabel, cardinality, duration);
        if (r == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "人物不存在"));
        }
        familyService.triggerAutoCompute();
        return ResponseEntity.ok(r);
    }

    @DeleteMapping("/relationships/{id}")
    public ResponseEntity<Void> deleteRelationship(@PathVariable Long id) {
        familyService.deleteRelationship(id);
        return ResponseEntity.ok().build();
    }

    // ============ 关系计算 ============

    @GetMapping("/family/calculate-relationship")
    public ResponseEntity<RelationshipResult> calculateRelationship(
            @RequestParam("p1") Long person1Id,
            @RequestParam("p2") Long person2Id) {
        RelationshipResult result = familyService.calculateRelationship(person1Id, person2Id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/family/graph")
    public Map<String, Object> getGraphData(@RequestParam(required = false) Long treeId) {
        familyService.setCurrentTree(treeId);
        familyService.rebuildGraph(); // 切换到当前树后重建内存图
        return familyService.getGraphData();
    }

    @GetMapping("/family/computed-relationships")
    public List<Map<String, Object>> getComputedRelationships() {
        return familyService.getComputedRelationships();
    }

    @PutMapping("/family/computed-relationships/{id}")
    public ResponseEntity<Void> updateComputedRelationship(@PathVariable Long id, @RequestBody Map<String, String> body) {
        familyService.updateComputedRelationship(id, body.get("label"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/family/auto-connect")
    public ResponseEntity<Map<String, Object>> addPersonWithConnection(
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String gender = (String) body.get("gender");
        Long connectedPersonId = body.get("connectedPersonId") != null
                ? Long.valueOf(body.get("connectedPersonId").toString()) : null;
        String relationType = (String) body.get("relationType");
        Long treeId = body.get("treeId") != null ? Long.valueOf(body.get("treeId").toString()) : null;

        familyService.setCurrentTree(treeId);
        Map<String, Object> result = familyService.addPersonWithConnection(
                name, gender, connectedPersonId, relationType, treeId);
        familyService.triggerAutoCompute();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/family/create-er-relation")
    public ResponseEntity<?> createErRelation(@RequestBody Map<String, Object> body) {
        Long entityA = Long.valueOf(body.get("entityA").toString());
        Long entityB = Long.valueOf(body.get("entityB").toString());
        String relName = (String) body.get("relName");
        String cardA = (String) body.get("cardA");
        String cardB = (String) body.get("cardB");
        Long treeId = body.get("treeId") != null ? Long.valueOf(body.get("treeId").toString()) : null;
        familyService.setCurrentTree(treeId);
        return ResponseEntity.ok(familyService.createErRelation(entityA, entityB, relName, cardA, cardB, treeId));
    }

    @GetMapping("/family/aoe/{treeId}")
    public ResponseEntity<Map<String, Object>> calculateAoe(@PathVariable Long treeId) {
        return ResponseEntity.ok(aoeService.calculate(treeId));
    }

    @PostMapping("/family/recompute")
    public ResponseEntity<String> recompute(@RequestParam(required = false) Long treeId) {
        familyService.setCurrentTree(treeId);
        familyService.triggerAutoCompute();
        return ResponseEntity.ok("OK");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
        if (msg.length() > 200) msg = msg.substring(0, 200);
        return ResponseEntity.internalServerError().body(Map.of("error", msg));
    }
}
