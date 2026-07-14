package com.familytree.model;

import jakarta.persistence.*;

/**
 * 系统自动计算得出的关系。
 * 当添加人物或关系时，系统自动推理并存储五代内的所有衍生关系。
 */
@Entity
@Table(name = "computed_relationships")
public class ComputedRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_a_id", nullable = false)
    private Person personA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_b_id", nullable = false)
    private Person personB;

    /** 关系称呼，如"叔叔"、"堂哥"、"姥爷" */
    @Column(nullable = false)
    private String label;

    /** 类别：BLOOD / IN_LAW / SPOUSE / SELF */
    @Column(nullable = false)
    private String category;

    /** 代差：正数=B是长辈，负数=B是晚辈，0=同辈 */
    private int generationDiff;

    public ComputedRelationship() {}

    public ComputedRelationship(Person personA, Person personB, String label, String category, int generationDiff) {
        this.personA = personA;
        this.personB = personB;
        this.label = label;
        this.category = category;
        this.generationDiff = generationDiff;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getPersonA() { return personA; }
    public void setPersonA(Person personA) { this.personA = personA; }

    public Person getPersonB() { return personB; }
    public void setPersonB(Person personB) { this.personB = personB; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getGenerationDiff() { return generationDiff; }
    public void setGenerationDiff(int generationDiff) { this.generationDiff = generationDiff; }
}
