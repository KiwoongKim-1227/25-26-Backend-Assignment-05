package com.example.oauthgoogleloginexample.repository;

import com.example.oauthgoogleloginexample.domain.Issue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    @EntityGraph(attributePaths = "project")
    List<Issue> findAllWithProject();
}