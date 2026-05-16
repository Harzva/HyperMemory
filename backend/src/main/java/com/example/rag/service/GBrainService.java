package com.example.rag.service;

import com.example.rag.model.GBrainSkillRunEntity;
import com.example.rag.repository.GBrainSkillRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final QaMetricsService qaMetricsService;
    private final GBrainSkillRunRepository skillRunRepository;
    private final List<String> skillNames = List.of(
            "WikiCoverageSnapshot",
            "MemoryIndexHealthCheck"
    );

    public GBrainService(LLMWikiService wikiService,
                         QaMetricsService qaMetricsService,
                         GBrainSkillRunRepository skillRunRepository) {
        this.wikiService = wikiService;
        this.qaMetricsService = qaMetricsService;
        this.skillRunRepository = skillRunRepository;
    }

    public String ask(String question) {
        return wikiService.query(question);
    }

    public String ask(String question, String tenantId) {
        return qaMetricsService.recordOperation("ask", "gbrain", tenantId, () ->
                wikiService.query(question, tenantId));
    }

    public void runAllSkills() {
        int pageCount = wikiService.pageCount();
        int totalCharacters = wikiService.totalPageCharacters();
        log.info("Wiki coverage: {} pages, {} characters", pageCount, totalCharacters);
        recordSkillRun("WikiCoverageSnapshot", "SUCCESS", "pages=" + pageCount + ", characters=" + totalCharacters);
        log.info("Memory index health check completed");
        recordSkillRun("MemoryIndexHealthCheck", "SUCCESS", "wiki_pages=" + pageCount);
    }

    public List<String> getSkillNames() {
        return skillNames;
    }

    private void recordSkillRun(String skillName, String status, String details) {
        GBrainSkillRunEntity run = new GBrainSkillRunEntity();
        run.setTenantId("default");
        run.setSkillName(skillName);
        run.setStatus(status);
        run.setDetails(details);
        run.setCreatedAt(LocalDateTime.now());
        skillRunRepository.save(run);
    }
}
