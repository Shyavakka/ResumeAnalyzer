package com.navadhiti.resumeanalyzer.model;

import java.util.List;

/**
 * Plain data holder for the structured feedback we show on the results page.
 * Matches exactly the JSON shape we ask the AI model to return, so Jackson
 * can deserialize the AI response straight into this object.
 */
public class AnalysisResult {

    private int score;
    private String profileSummary;
    private List<String> keyStrengths;
    private List<String> areasForImprovement;
    private List<String> missingSkillsOrSections;
    private List<String> suggestions;

    public AnalysisResult() {
        // needed by Jackson
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getProfileSummary() {
        return profileSummary;
    }

    public void setProfileSummary(String profileSummary) {
        this.profileSummary = profileSummary;
    }

    public List<String> getKeyStrengths() {
        return keyStrengths;
    }

    public void setKeyStrengths(List<String> keyStrengths) {
        this.keyStrengths = keyStrengths;
    }

    public List<String> getAreasForImprovement() {
        return areasForImprovement;
    }

    public void setAreasForImprovement(List<String> areasForImprovement) {
        this.areasForImprovement = areasForImprovement;
    }

    public List<String> getMissingSkillsOrSections() {
        return missingSkillsOrSections;
    }

    public void setMissingSkillsOrSections(List<String> missingSkillsOrSections) {
        this.missingSkillsOrSections = missingSkillsOrSections;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
