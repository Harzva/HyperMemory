package com.example.rag.dto;

import java.util.ArrayList;
import java.util.List;

public class AnswerWithSources {
    private String answer;
    private List<SourceCitation> sources;

    public AnswerWithSources() {
    }

    public AnswerWithSources(String answer, List<SourceCitation> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public static AnswerWithSources of(String answer, List<SourceCitation> sources) {
        return new AnswerWithSources(answer, sources != null ? sources : new ArrayList<>());
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<SourceCitation> getSources() {
        return sources;
    }

    public void setSources(List<SourceCitation> sources) {
        this.sources = sources;
    }
}
