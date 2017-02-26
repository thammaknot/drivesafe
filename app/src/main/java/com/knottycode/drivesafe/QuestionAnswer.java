package com.knottycode.drivesafe;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by thammaknot on 2/26/17.
 */
public class QuestionAnswer {
    private String question;
    private String answer;
    private QuestionType type;
    private Set<String> answerKeywords;

    public enum QuestionType {
        FUNNY, RIDDLE, TRIVIA, OTHER;
    }

    public QuestionAnswer(String q, String a, QuestionType type, String keywords) {
        question = q;
        answer = a;
        this.type = type;
        String[] tokens = keywords.split(",");
        answerKeywords = new HashSet<String>(Arrays.asList(tokens));
    }

    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public QuestionType getType() { return type; }
    public Set<String> getAnswerKeywords() { return answerKeywords; }
}
