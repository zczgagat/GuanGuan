package com.familytree.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_trees")
public class FamilyTree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 所属用户 ID */
    private Long userId;

    private String description;

    /** 模板类型：family=族谱, er=ER图 */
    private String template;

    private LocalDateTime createdAt;

    public FamilyTree() {}

    public FamilyTree(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
}
