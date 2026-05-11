package com.example.rag.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple service demonstrating the concept of GBrain: a persistent
 * knowledge base augmented with autonomous skills. In this example we
 * reuse the LLM‑Wiki as the underlying memory and register a few dummy
 * skills. A real implementation would schedule cron jobs and define
 * skills that carry out meaningful work (e.g. syncing new data, sending
 * reminders, or executing code).
 */
@Service
public class GBrainService {

    private final LLMWikiService wikiService;
    private final List<Runnable> skills;

    @Autowired
    public GBrainService(LLMWikiService wikiService) {
        this.wikiService = wikiService;
        this.skills = new ArrayList<>();
        // Register some example skills. In production these could be
        // annotated with @Scheduled to run automatically.
        skills.add(() -> {
            // Example skill: print a summary of the wiki length
            String wiki = wikiService.query("");
            System.out.println("[GBrain] Wiki length: " + wiki.length());
        });
        skills.add(() -> {
            // Example skill: log that a periodic sync is happening
            System.out.println("[GBrain] Performing periodic sync (demo)");
        });
    }

    /**
     * Answer a user question by delegating to the wiki memory. Real
     * implementations could combine retrieval with reasoning or tool calls.
     *
     * @param question the question being asked
     * @return answer drawn from the wiki
     */
    public String ask(String question) {
        return wikiService.query(question);
    }

    /**
     * Run all registered skills once. In a real GBrain this would be
     * orchestrated by a scheduler or cron subsystem.
     */
    public void runAllSkills() {
        skills.forEach(Runnable::run);
    }

    /**
     * List the names of the registered skills. Here we just return
     * placeholder names corresponding to the lambdas above.
     *
     * @return list of skill names
     */
    public List<String> getSkillNames() {
        List<String> names = new ArrayList<>();
        names.add("WikiLengthLogger");
        names.add("PeriodicSync");
        return names;
    }
}