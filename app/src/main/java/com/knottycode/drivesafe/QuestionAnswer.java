package com.knottycode.drivesafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by thammaknot on 2/26/17.
 */
public class QuestionAnswer {
    private String questionId;
    private String question;
    private String answer;
    private QuestionType type;
    private List<Set<String>> allAnswers;

    private static final String ANSWER_ALTERNATIVES_DELIM = "\\|";
    private static final String ANSWER_SYLLABLE_DELIM = ",";

    public enum QuestionType {
        // code, name, filename
        FUNNY(0, "funny", "fun_questions"),
        RIDDLE(1, "riddles", "riddles"),
        TRIVIA(2, "trivia", "trivia"),
        MATH(3, "math", ""),
        OTHER(4, "other", ""),
        SPORTS(5, "sports", "sports"),
        SCIENCE(6, "science", "science"),
        SONGS(7, "songs", "songs");

        private int code;
        private String name;
        private String filename;

        QuestionType(int code, String s, String f) {
            this.code = code;
            name = s;
            filename = f;
        }

        public String getFilename() {
            return filename;
        }

        public static QuestionType fromString(String s) {
            for (QuestionType t : QuestionType.values()) {
                if (t.name.equals(s)) {
                    return t;
                }
            }
            return OTHER;
        }

        public int getPreamble() {
            switch (code) {
                case 0:
                    return R.string.fun_question_preamble;
                case 1:
                    return R.string.riddle_preamble;
                case 2:
                    return R.string.trivia_preamble;
                case 3:
                    return R.string.math_question_preamble;
                case 5:
                    return R.string.sports_question_preamble;
                case 6:
                    return R.string.science_question_preamble;
                case 7:
                    return R.string.songs_question_preamble;
                case 4:
                default:
                    return R.string.other_question_preamble;
            }
        }

        public int getResId() {
            switch (code) {
                case 0:
                    return R.string.fun_question_name;
                case 1:
                    return R.string.riddle_name;
                case 2:
                    return R.string.trivia_name;
                case 3:
                    return R.string.math_question_name;
                case 5:
                    return R.string.sports_question_name;
                case 6:
                    return R.string.science_question_name;
                case 7:
                    return R.string.songs_question_name;
                case 4:
                default:
                    return R.string.other_question_name;
            }
        }
    }

    public QuestionAnswer(String id, String q, String a, QuestionType type, String keywords) {
        questionId = id;
        this.type = type;
        allAnswers = new ArrayList<>();
        if (type == QuestionType.MATH) {
            Random random = Constants.RANDOM;
            // 0: addition, 1: multiplication
            int operator = random.nextInt(2);
            if (operator == 0) {
                int r1 = random.nextInt(50) + 2;
                int r2 = random.nextInt(50) + 2;
                question = r1 + " " + Constants.MATH_ADDITION + " "
                        + r2 + " " + Constants.MATH_QUESTION_ENDING;;
                answer = String.valueOf(r1 + r2);
                Set<String> s = new HashSet<>();
                s.add("<" + answer + ">");
                allAnswers.add(s);
            } else {
                int r1 = random.nextInt(10) + 2;
                int r2 = random.nextInt(10) + 2;
                question = r1 + " " + Constants.MATH_MULTIPLICATION + " "
                        + r2 + " " + Constants.MATH_QUESTION_ENDING;
                answer = String.valueOf(r1 * r2);
                Set<String> s = new HashSet<>();
                s.add("<" + answer + ">");
                allAnswers.add(s);
            }
        } else {
            question = q;
            answer = a;
            String[] alts = keywords.split(ANSWER_ALTERNATIVES_DELIM);
            for (String alt : alts) {
                String[] tokens = alt.split(ANSWER_SYLLABLE_DELIM);
                Set<String> answerKeywords = new HashSet<String>(Arrays.asList(tokens));
                allAnswers.add(answerKeywords);
            }
        }
    }

    public String getQuestion() { return question; }
    public String getAnswer() {
        return answer;
    }
    public String getId() { return questionId; }
    public QuestionType getType() { return type; }
    public List<Set<String>> getAllAnswers() { return allAnswers; }

    public int getPreamble() {
        return type.getPreamble();
    }

    public boolean checkAnswer(List<String> userAnswers) {
        for (Set<String> answer : allAnswers) {
            for (String result : userAnswers) {
                boolean match = true;
                for (String key : answer) {
                    if (key.charAt(0) == '<' && key.charAt(key.length() - 1) == '>') {
                        key = key.substring(1, key.length() - 1);
                        if (!result.equals(key)) {
                            match = false;
                            break;
                        }
                    } else {
                        if (!result.contains(key)) {
                            match = false;
                            break;
                        }
                    }
                }
                if (match) {
                    return true;
                }
            }
        }
        return false;
    }
}
