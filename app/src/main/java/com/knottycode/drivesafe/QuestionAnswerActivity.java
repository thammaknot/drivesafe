package com.knottycode.drivesafe;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.knottycode.drivesafe.CheckpointManager.ScoreMode;

/**
 * Created by thammaknot on 2/18/17.
 */
public class QuestionAnswerActivity extends BaseDriveModeActivity {

    private MediaPlayer mediaPlayer;
    CountDownTimer adaptiveLoudnessTimer;
    TextView asrOutputTextView;

    private long qaModeStartTime;
    private long answerPhaseStartTime = -1;
    private QuestionAnswer currentQuestion;

    private Random random = new Random();
    private TextToSpeech tts;
    private boolean delayedAnswer = false;
    private boolean hasSpeechAnswer = false;
    private boolean gracePeriodExtension = false;

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
        // asrOutputTextView = (TextView) findViewById(R.id.asrOutputTextView);
        initTTS();
    }

    private void initTTS() {
        qaModeStartTime = System.currentTimeMillis();
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                tts.setLanguage(Constants.THAI_LOCALE);
                Set<Voice> voices = tts.getVoices();
                for (Voice v : voices) {
                    if (v.isNetworkConnectionRequired() && v.getQuality() >= Voice.QUALITY_HIGH &&
                            v.getLocale().equals(Constants.THAI_LOCALE)) {
                        tts.setVoice(v);
                    }
                }

                currentQuestion = getQuestionAnswer();
                speakPreamble();
                startQuestion();
            }
        });
        tts.setOnUtteranceProgressListener(getOnUtteranceProgressListener());
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
        checkpointManager.updateScore(ScoreMode.SKIP);
        stopQuestion();
        startDriveMode();
        return true;
    }

    private QuestionAnswer getQuestionAnswer() {
        return checkpointManager.getNextQuestion();
    }

    protected UtteranceProgressListener getOnUtteranceProgressListener() {
        return new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {}

            @Override
            public void onError(String s) {}

            @Override
            public void onDone(final String uttId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (uttId.equals(Constants.QUESTION_UTT_ID)) {
                            if (answerPhaseStartTime == -1) {
                                answerPhaseStartTime = System.currentTimeMillis();
                            }
                            startVoiceRecognitionActivity();
                        } else if (uttId.equals(Constants.ANSWER_UTT_ID)) {
                            stopQuestion();
                            startDriveMode();
                        } else if (uttId.equals(Constants.CORRECT_KEYWORD_UTT_ID)) {
                            tts.setSpeechRate(1.0f);
                            stopQuestion();
                            startDriveMode();
                        } else if (uttId.equals(Constants.TRY_AGAIN_UTT_ID)
                                || uttId.equals(Constants.PREAMBLE_UTT_ID)) {
                            // Nothing.
                        } else if (uttId.equals(Constants.EXTEND_GRACE_PERIOD_UTT_ID)) {
                            asrListener.startRecognition();
                        } else {
                            Log.d(TAG, "*** Doing nothing!!!!! ***");
                        }
                    }
                });
            }
        };
    }

    private void speakPreamble() {
        tts.speak(getString(currentQuestion.getPreamble()), TextToSpeech.QUEUE_ADD, null, Constants.PREAMBLE_UTT_ID);
    }

    private void startQuestion() {
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
        int index = random.nextInt(Constants.CORRECT_KEYWORDS.size());
        String correctKeyword = Constants.CORRECT_KEYWORDS.get(index);
        tts.speak(correctKeyword, TextToSpeech.QUEUE_ADD, null, Constants.CORRECT_KEYWORD_UTT_ID);
    }

    private void tryAgain() {
        int index = random.nextInt(Constants.TRY_AGAIN_KEYWORDS.size());
        String tryAgainKeyword = Constants.TRY_AGAIN_KEYWORDS.get(index);
        tts.speak(tryAgainKeyword, TextToSpeech.QUEUE_ADD, null, Constants.TRY_AGAIN_UTT_ID);
    }

    private void extendGracePeriod() {
        int index = random.nextInt(Constants.EXTEND_GRACE_PERIOD_KEYWORDS.size());
        String extendGracePeriodKeyword = Constants.EXTEND_GRACE_PERIOD_KEYWORDS.get(index);
        extendGracePeriodKeyword = extendGracePeriodKeyword + " " + Constants.GRACE_PERIOD_EXTENSION_SECONDS + " วินาที";
        tts.speak(extendGracePeriodKeyword, TextToSpeech.QUEUE_ADD, null, Constants.EXTEND_GRACE_PERIOD_UTT_ID);
        gracePeriodExtension = true;
    }

    @Override
    protected void updateDisplay(long now) {
        if (answerPhaseStartTime == -1) { return; }
        /*
        TextView debugTime = (TextView) findViewById(R.id.debugTime);
        long elapsed = (now - answerPhaseStartTime) / 1000;
        long seconds = elapsed % 60;
        debugTime.setText(String.format("00:%02d [%s]", seconds, asrListener.isListening() ? "listen" : "not listen"));
        */
    }

    @Override
    protected boolean proceedToNextStep(long now) {
        if (answerPhaseStartTime == -1) { return false; }
        long checkpointElapsed = now - answerPhaseStartTime;
        long extension = 0;
        if (gracePeriodExtension) {
            extension += Constants.GRACE_PERIOD_EXTENSION_SECONDS * 1000;
        }
        if (checkpointElapsed >= Constants.QUESTION_ANSWER_MAX_GRACE_PERIOD_MILLIS + extension) {
            if (hasSpeechAnswer) {
                checkpointManager.updateScore(ScoreMode.INCORRECT);
                speakAnswer();
            } else {
                checkpointManager.updateScore(ScoreMode.NO_RESPONSE);
                startAlarmMode();
            }
            return true;
        } else if (checkpointElapsed >= Constants.QUESTION_ANSWER_GRACE_PERIOD_MILLIS + extension) {
            if (asrListener.isListening()) {
                delayedAnswer = true;
                return false;
            }
            if (!delayedAnswer) {
                if (hasSpeechAnswer) {
                    checkpointManager.updateScore(ScoreMode.INCORRECT);
                    speakAnswer();
                } else {
                    checkpointManager.updateScore(ScoreMode.NO_RESPONSE);
                    startAlarmMode();
                }
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

    private boolean isExtensionWord(List<String> results) {
        for (String result : results) {
            if (Constants.EXTENSION_WORDS.contains(result)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCorrectAnswer(List<String> results) {
        return currentQuestion.checkAnswer(results);
    }

    private long getTimeRemainingInMillis() {
        if (answerPhaseStartTime == -1) {
            return Constants.QUESTION_ANSWER_GRACE_PERIOD_MILLIS;
        }
        return Constants.QUESTION_ANSWER_GRACE_PERIOD_MILLIS -
                (System.currentTimeMillis() - answerPhaseStartTime);
    }

    @Override
    public void onASRResultsReady(List<String> results) {
        delayedAnswer = false;
        if (results.size() == 0) {
            return;
        }
        hasSpeechAnswer = true;
        String topResult = results.get(0);
        // asrOutputTextView.setText(topResult);
        if (isSkipWord(results)) {
            Log.d(TAG, "******* SKIP WORD FOUND ==========");
            checkpointManager.updateScore(ScoreMode.SKIP);
            speakAnswer();
        } else if (hasCorrectAnswer(results)) {
            long responseTimeMillis = System.currentTimeMillis() - qaModeStartTime;
            checkpointManager.addResponseTime(responseTimeMillis);
            checkpointManager.updateScore(ScoreMode.CORRECT);
            acknowledgeCorrectAnswer();
        } else if (!gracePeriodExtension && isExtensionWord(results)) {
            extendGracePeriod();
        } else {
            // Incorrect answer.
            // If there is sufficient time, start ASR again.
            // If not, just go straight to answer.
            if (getTimeRemainingInMillis() >= Constants.MIN_TIME_TO_RESTART_ASR_MILLIS) {
                tryAgain();
                startQuestion();
            } else {
                checkpointManager.updateScore(ScoreMode.INCORRECT);
                speakAnswer();
            }
        }
    }

}
