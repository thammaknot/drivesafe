package com.knottycode.drivesafe;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thammaknot on 2/19/17.
 */
public class ASRListener implements RecognitionListener {
    private static final String TAG = "**ASR Listener**";

    private List<String> results;
    private BaseDriveModeActivity activity;
    private long asrListeningStartTime = -1;
    private boolean asrActive = false;
    private boolean isListening = false;
    // Whether to continue listening for ASR.
    private boolean continuousAsr = true;
    private SpeechRecognizer recognizer;
    private Intent intent;

    public ASRListener(BaseDriveModeActivity activity) {
        this.activity = activity;
    }

    public void onResults(Bundle bundle) {
        asrListeningStartTime = -1;
        results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        List<String> trimmedResults = new ArrayList<String>();
        FirebaseCrash.log("ASRListener: onResults size = " + results.size());
        for (int i = 0; i < results.size(); i++)
        {
            trimmedResults.add(results.get(i).trim());
            Log.d(TAG, "result " + results.get(i));
        }
        activity.onASRResultsReady(trimmedResults);
    }

    public void onBeginningOfSpeech() {
        FirebaseCrash.log("ASRListener: onBeginningOfSpeech");
        Log.d(TAG, "####################  onBeginning of speech");
        isListening = true;
    }

    public void onReadyForSpeech(Bundle params) {
        FirebaseCrash.log("ASRListener: onReadyForSpeech");
    }

    public void onRmsChanged(float rmsdB) {}

    public void onBufferReceived(byte[] buffer) {
        FirebaseCrash.log("ASRListener: onBufferReceived");
    }

    public void onEndOfSpeech() {
        FirebaseCrash.log("ASRListener: onEndOfSpeech");
        isListening = false;
    }

    public void onError(int error) {
        FirebaseCrash.log("ASRListener: onError. Code = " + error);
        Log.d(TAG,  "!!!!!!! error " +  error);
        if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            // Possibly empty input audio, restart ASR.
            startRecognition();
        }
    }

    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "onPartialResults");
    }

    public void onEvent(int eventType, Bundle params) {
        FirebaseCrash.log("ASRListener: onEvent: " + eventType);
    }

    public void forceStopListening() {
        FirebaseCrash.log("ASRListener: Forced stop listening called.");
        Log.d(TAG, ">>>>>forceStopListening");
        if (!asrActive) {
            return;
        }
        asrActive = false;
        asrListeningStartTime = -1;
        recognizer.stopListening();
    }

    public boolean isListening() {
        return isListening;
    }

    public long getAsrListeningStartTime() {
        return asrListeningStartTime;
    }

    public void setRecognizer(SpeechRecognizer recognizer, Intent intent) {
        this.recognizer = recognizer;
        this.intent = intent;
    }

    public void startRecognition() {
        FirebaseCrash.log("ASRListener: Starting recognition");
        FirebaseCrash.log("ASRListener: Recognition available? " + recognizer.isRecognitionAvailable(activity));
        if (asrActive) {
            recognizer.stopListening();
        }
        asrActive = true;
        FirebaseCrash.log("ASRListener: Start listening");
        asrListeningStartTime = System.currentTimeMillis();
        recognizer.startListening(intent);
    }

    public void stopRecognition() {
        FirebaseCrash.log("ASRListener: Stop recognition called");
        asrActive = false;
        recognizer.cancel();
    }
}
