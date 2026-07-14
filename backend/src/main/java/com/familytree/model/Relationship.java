package com.familytree.model;

import com.familytree.model.enums.RelationshipType;
import jakarta.persistence.*;

@Entity
@Table(name = "relationships")
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person1_id", nullable = false)
    private Person person1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person2_id", nullable = false)
    private Person person2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType type;

    /** 自定义关系名称（type=CUSTOM 时使用，如"朋友"、"同事"） */
    private String customLabel;

    /** ER 图基数标注：1:1 / 1:N / N:M */
    private String cardinality;

    /** AOE 网活动持续时间 */
    private Integer duration;

    public Relationship() {}

    public Relationship(Person person1, Person person2, RelationshipType type) {
        this.person1 = person1;
        this.person2 = person2;
        this.type = type;
    }

    public Relationship(Person person1, Person person2, RelationshipType type, String customLabel) {
        this.person1 = person1;
        this.person2 = person2;
        this.type = type;
        this.customLabel = customLabel;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getPerson1() { return person1; }
    public void setPerson1(Person person1) { this.person1 = person1; }

    public Person getPerson2() { return person2; }
    public void setPerson2(Person person2) { this.person2 = person2; }

    public RelationshipType getType() { return type; }
    public void setType(RelationshipType type) { this.type = type; }

    public String getCustomLabel() { return customLabel; }
    public void setCustomLabel(String customLabel) { this.customLabel = customLabel; }

    public String getCardinality() { return cardinality; }
    public void setCardinality(String cardinality) { this.cardinality = cardinality; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
