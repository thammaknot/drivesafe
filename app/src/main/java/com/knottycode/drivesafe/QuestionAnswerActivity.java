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
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static com.knottycode.drivesafe.R.id.debugTime;
import static com.knottycode.drivesafe.R.string.minutes;
import static com.knottycode.drivesafe.R.string.seconds;

/**
 * Created by thammaknot on 2/18/17.
 */
public class QuestionAnswerActivity extends BaseDriveModeActivity {

    private MediaPlayer mediaPlayer;
    CountDownTimer adaptiveLoudnessTimer;
    TextView asrOutputTextView;

    private long qaModeStartTime;
    private long answerPhaseStartTime = -1;
    private List<QuestionAnswer> questions;
    private QuestionAnswer currentQuestion;

    private Random random = new Random();
    private TextToSpeech tts;
    private boolean delayedAnswer = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = QUESTION_ANSWER_MODE;
        TAG = "DriveSafe: QuestionAnswerActivity";
        init();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_question_answer_mode);
        RelativeLayout wholeScreenLayout = (RelativeLayout) findViewById(R.id.wholeScreenLayout);
        wholeScreenLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent me) {
                return QuestionAnswerActivity.this.onTouch(v, me);
            }
        });
        asrOutputTextView = (TextView) findViewById(R.id.asrOutputTextView);
        loadQuestions();
        initTTS();
    }

    private void initTTS() {
        qaModeStartTime = System.currentTimeMillis();
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                startQuestion();
            }
        });
        tts.setLanguage(new Locale("th", "th"));
        tts.setOnUtteranceProgressListener(getOnUtteranceProgressListener());
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

    protected UtteranceProgressListener getOnUtteranceProgressListener() {
        return new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
            }

            @Override
            public void onError(String s) {
            }

            @Override
            public void onDone(final String uttId) {
                answerPhaseStartTime = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (uttId.equals(Constants.QUESTION_UTT_ID)) {
                            Log.d(TAG, "&&&&&&&&&&& QUESTION DONE...STARTING ASR");
                            startVoiceRecognitionActivity();
                        } else if (uttId.equals(Constants.ANSWER_UTT_ID)) {
                            stopQuestion();
                            startDriveMode();
                        } else if (uttId.equals(Constants.CORRECT_KEYWORD_UTT_ID)) {
                            tts.setSpeechRate(1.0f);
                            stopQuestion();
                            startDriveMode();
                        } else {
                            Log.d(TAG, "*** Doing nothing!!!!! ***");
                        }
                    }
                });
            }
        };
    }

    private void startQuestion() {
        currentQuestion = getQuestionAnswer();
        tts.speak(currentQuestion.getQuestion(), TextToSpeech.QUEUE_ADD, null, Constants.QUESTION_UTT_ID);
    }

    private void stopQuestion() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    private void speakAnswer() {
        tts.speak(Constants.ANSWER_KEYWORD, TextToSpeech.QUEUE_ADD, null, Constants.ANSWER_KEYWORD_UTT_ID);
        tts.playSilentUtterance(Constants.UNIT_SILENCE_DURATION_MILLIS, TextToSpeech.QUEUE_ADD, Constants.SILENCE_UTT_ID);
        tts.speak(currentQuestion.getAnswer(), TextToSpeech.QUEUE_ADD, null, Constants.ANSWER_UTT_ID);
    }

    private void acknowledgeCorrectAnswer() {
        tts.setSpeechRate(2.0f);
        tts.speak(Constants.CORRECT_KEYWORD, TextToSpeech.QUEUE_ADD, null, Constants.CORRECT_KEYWORD_UTT_ID);
    }

    @Override
    protected void updateDisplay(long now) {
        if (answerPhaseStartTime == -1) { return; }
        TextView debugTime = (TextView) findViewById(R.id.debugTime);
        long elapsed = (now - answerPhaseStartTime) / 1000;
        long seconds = elapsed % 60;
        // long minutes = elapsed / 60;
        debugTime.setText(String.format("00:%02d [%s]", seconds, asrListener.isListening() ? "listen" : "not listen"));
    }

    @Override
    protected boolean proceedToNextStep(long now) {
        if (answerPhaseStartTime == -1) { return false; }
        long checkpointElapsed = now - answerPhaseStartTime;
        if (checkpointElapsed >= Constants.QUESTION_ANSWER_GRACE_PERIOD_MILLIS) {
            if (asrListener.isListening()) {
                delayedAnswer = true;
                return false;
            }
            if (!delayedAnswer) {
                speakAnswer();
                return true;
            }
        }
        return false;
    }

    private boolean isSkipWord(List<String> results) {
        for (String result : results) {
            if (Constants.SKIP_WORDS.contains(result)) {
                return true;
            }
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
        delayedAnswer = false;
        if (results.size() == 0) {
            return;
        }
        String topResult = results.get(0);
        asrOutputTextView.setText(topResult);
        if (isSkipWord(results)) {
            Log.d(TAG, "******* SKIP WORD FOUND ==========");
            speakAnswer();
        } else if (hasCorrectAnswer(results)) {
            long responseTimeMillis = System.currentTimeMillis() - qaModeStartTime;
            checkpointManager.addResponseTime(responseTimeMillis);
            acknowledgeCorrectAnswer();
        } else {
            Log.d(TAG, "##### SAFETY PHRASE not found!!!!");
        }
    }

}
