package com.knottycode.drivesafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by thammaknot on 2/26/17.
 */
public class QuestionAnswer {
    private String question;
    private String answer;
    private QuestionType type;
    private List<Set<String>> allAnswers;

    private static final String ANSWER_ALTERNATIVES_DELIM = "\\|";
    private static final String ANSWER_SYLLABLE_DELIM = ",";

    public enum QuestionType {
        FUNNY, RIDDLE, TRIVIA, OTHER;
    }

    public QuestionAnswer(String q, String a, QuestionType type, String keywords) {
        question = q;
        answer = a;
        this.type = type;
        String[] alts = keywords.split(ANSWER_ALTERNATIVES_DELIM);
        allAnswers = new ArrayList<>();
        for (String alt : alts) {
            String[] tokens = alt.split(ANSWER_SYLLABLE_DELIM);
            Set<String> answerKeywords = new HashSet<String>(Arrays.asList(tokens));
            allAnswers.add(answerKeywords);
        }
    }

    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public QuestionType getType() { return type; }
    public List<Set<String>> getAllAnswers() { return allAnswers; }
}
