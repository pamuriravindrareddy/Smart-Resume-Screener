package com.resume.screener.repository;

import com.resume.screener.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    Optional<MatchResult> findFirstByCandidateIdAndJobDescriptionIdOrderByCreatedAtDesc(Long candidateId, Long jobDescriptionId);

    @Query("SELECT m FROM MatchResult m WHERE " +
           "(:candidateId IS NULL OR m.candidate.id = :candidateId) AND " +
           "(:jobDescriptionId IS NULL OR m.jobDescription.id = :jobDescriptionId) " +
           "ORDER BY m.createdAt DESC")
    List<MatchResult> findFiltered(@Param("candidateId") Long candidateId, @Param("jobDescriptionId") Long jobDescriptionId);
}
