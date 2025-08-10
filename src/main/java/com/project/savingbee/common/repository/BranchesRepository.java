package com.project.savingbee.common.repository;

import com.project.savingbee.common.entity.Branches;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchesRepository extends JpaRepository<Branches, Integer> {

}