package com.familytree.repository;

import com.familytree.model.FamilyTree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyTreeRepository extends JpaRepository<FamilyTree, Long> {
    List<FamilyTree> findByUserId(Long userId);
}
