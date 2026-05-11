package com.example.rag.controller;

import com.example.rag.service.GBrainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller to expose GBrain skills for inspection and manual invocation.
 * Clients can list available skills and trigger them via HTTP. In a real
 * application you might secure these endpoints or integrate with a
 * scheduling framework instead of manual calls.
 */
@RestController
@RequestMapping("/api/gbrain/skills")
public class GBrainSkillController {

    private final GBrainService gBrainService;

    public GBrainSkillController(GBrainService gBrainService) {
        this.gBrainService = gBrainService;
    }

    /**
     * List the names of registered skills.
     *
     * @return list of skill identifiers
     */
    @GetMapping
    public ResponseEntity<List<String>> listSkills() {
        return ResponseEntity.ok(gBrainService.getSkillNames());
    }

    /**
     * Run all registered skills immediately. Returns HTTP 204 on success.
     */
    @PostMapping("/run-all")
    public ResponseEntity<Void> runAllSkills() {
        gBrainService.runAllSkills();
        return ResponseEntity.noContent().build();
    }
}