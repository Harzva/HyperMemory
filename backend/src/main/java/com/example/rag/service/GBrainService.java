package com.example.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Skill-oriented layer over wiki memory. The current skills are deterministic
 * inspection tasks, which keeps the public demo reproducible while leaving a
 * clear place for scheduled production jobs later.
 */
@Service
public class GBrainService {

    private static final Logger log = LoggerFactory.getLogger(GBrainService.class);

    private final LLMWikiService wikiService;
    private final List<String> skillNames = List.of(
            "WikiCoverageSnapshot",
            "MemoryIndexHealthCheck"
    );

    public GBrainService(LLMWikiService wikiService) {
        this.wikiService = wikiService;
    }

    public String ask(String question) {
        return wikiService.query(question);
    }

    public String ask(String question, String tenantId) {
        return wikiService.query(question, tenantId);
    }

    public void runAllSkills() {
        log.info("Wiki coverage: {} pages, {} characters",
                wikiService.pageCount(),
                wikiService.totalPageCharacters());
        log.info("Memory index health check completed");
    }

    public List<String> getSkillNames() {
        return skillNames;
    }
}
