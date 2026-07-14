package com.familytree.controller;

import com.familytree.model.FamilyTree;
import com.familytree.repository.FamilyTreeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trees")
public class TreeController {

    @Autowired
    private FamilyTreeRepository familyTreeRepository;

    @GetMapping
    public List<FamilyTree> getAll(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) return List.of();
        return familyTreeRepository.findByUserId(userId);
    }

    @PostMapping
    public FamilyTree create(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        String name = body.get("name");
        if (name == null || name.isBlank()) name = "未命名图谱";
        FamilyTree tree = new FamilyTree(name);
        tree.setDescription(body.get("description"));
        tree.setTemplate(body.get("template"));
        tree.setUserId(userId);
        return familyTreeRepository.save(tree);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        familyTreeRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
