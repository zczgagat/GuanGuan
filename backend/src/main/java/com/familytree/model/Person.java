package com.familytree.model;

import com.familytree.model.enums.Gender;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "persons")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    /** 兄弟姐妹间的出生排序（1=老大，2=老二...），用于判定哥哥/弟弟/姐姐/妹妹 */
    private Integer siblingRank;

    /** 头像文件名，为空则显示名字首字 */
    private String avatar;

    /** 所属家族树 ID */
    private Long familyTreeId;

    /** ER 图类型：entity=实体, relation=关系, attribute=属性 */
    private String entityType;

    private LocalDate birthDate;
    private String hobby;
    private String education;
    private String profession;
    private String address;

    public Person() {}

    public Person(String name, Gender gender) {
        this.name = name;
        this.gender = gender;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public Integer getSiblingRank() { return siblingRank; }
    public void setSiblingRank(Integer siblingRank) { this.siblingRank = siblingRank; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Long getFamilyTreeId() { return familyTreeId; }
    public void setFamilyTreeId(Long familyTreeId) { this.familyTreeId = familyTreeId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getHobby() { return hobby; }
    public void setHobby(String hobby) { this.hobby = hobby; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
