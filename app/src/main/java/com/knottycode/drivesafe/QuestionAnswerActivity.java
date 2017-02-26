package com.knottycode.drivesafe;

import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Created by thammaknot on 2/18/17.
 */
public class QuestionAnswerActivity extends BaseDriveModeActivity {

    private MediaPlayer mediaPlayer;
    CountDownTimer adaptiveLoudnessTimer;

    private long qaModeStartTime;
    private long answerPhaseStartTime = -1;
    private List<QuestionAnswer> questions;
    private QuestionAnswer currentQuestion;

    private Random random = new Random();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = QUESTION_ANSWER_MODE;
        TAG = "DriveSafe: QuestionAnswerActivity";
        init();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_question_answer_mode);
        RelativeLayout wholeScreenLayout = (RelativeLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return QuestionAnswerActivity.this.onTouch(v, me);
            }
        });
        loadQuestions();
        startQuestion();
    }

    private void loadQuestions() {
        AssetManager am = getAssets();
        InputStream is;
        questions = new ArrayList<QuestionAnswer>();
        try {
            is = am.open(Constants.QUESTION_PATH_PREFIX + "/fun_questions.txt");
        } catch (IOException io) {
            // Leave the set empty.
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = r.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != 3) {
                    Log.w(TAG, "Malformed line in question file: " + line);
                    continue;
                }
                questions.add(
                        new QuestionAnswer(tokens[0], tokens[1],
                                QuestionAnswer.QuestionType.FUNNY, tokens[2]));
            }
        } catch (IOException io) {
            Log.e(TAG, "Exception while reading question file.");
            io.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopQuestion();
    }

    @Override
    public void onResume() {
        qaModeStartTime = System.currentTimeMillis();
        super.onResume();
    }

    private boolean onTouch(View v, MotionEvent me) {
        if (me.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        long responseTimeMillis = System.currentTimeMillis() - qaModeStartTime;
        checkpointManager.addResponseTime(responseTimeMillis);
        stopQuestion();
        startDriveMode();
        return true;
    }

    private QuestionAnswer getQuestionAnswer() {
        int r = random.nextInt(questions.size());
        return questions.get(r);
    }

    @Override
    protected TextToSpeech.OnInitListener getOnInitListener() {
        return new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.setLanguage(new Locale("th", "th"));
                QuestionAnswer qa = currentQuestion;
                tts.setSpeechRate(1.0f);
                tts.speak(qa.getQuestion(), TextToSpeech.QUEUE_ADD, null, "");
            }
        };
    }

    @Override
    protected UtteranceProgressListener getOnUtteranceProgressListener() {
        return new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {}

            @Override
            public void onError(String s) {}

            @Override
            public void onDone(String s) {
                answerPhaseStartTime = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startVoiceRecognitionActivity();
                    }
                });
            }
        };
    }

    private void startQuestion() {
        currentQuestion = getQuestionAnswer();
        speak();
    }

    private void stopQuestion() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    private void speakAnswer() {
        tts.speak(Constants.ANSWER_KEYWORD, TextToSpeech.QUEUE_ADD, null, "");
        tts.playSilentUtterance(Constants.UNIT_SILENCE_DURATION_MILLIS, TextToSpeech.QUEUE_ADD, "");
        tts.speak(currentQuestion.getAnswer(), TextToSpeech.QUEUE_ADD, null, "");
    }

    @Override
    protected void updateDisplay(long now) { }

    @Override
    protected boolean proceedToNextStep(long now) {
        if (answerPhaseStartTime == -1) { return false; }
        long checkpointElapsed = now - answerPhaseStartTime;
        if (checkpointElapsed >= Constants.QUESTION_ANSWER_GRACE_PERIOD_MILLIS) {
            speakAnswer();
            return true;
        }
        return false;
    }

    private boolean hasCorrectAnswer(List<String> results) {
        Set<String> answerKeywords = currentQuestion.getAnswerKeywords();
        for (String result : results) {
            boolean match = true;
            for (String key : answerKeywords) {
                if (!result.contains(key)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onASRResultsReady(List<String> results) {
        if (hasCorrectAnswer(results)) {
            long responseTimeMillis = System.currentTimeMillis() - qaModeStartTime;
            checkpointManager.addResponseTime(responseTimeMillis);
            stopQuestion();
            startDriveMode();
        } else {
            Log.d(TAG, "##### SAFETY PHRASE not found!!!!");
        }
    }

}
