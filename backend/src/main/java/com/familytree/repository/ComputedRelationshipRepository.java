package com.familytree.repository;

import com.familytree.model.ComputedRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComputedRelationshipRepository extends JpaRepository<ComputedRelationship, Long> {

    List<ComputedRelationship> findByPersonAIdOrPersonBId(Long personAId, Long personBId);

    void deleteByPersonAIdOrPersonBId(Long personAId, Long personBId);
}
