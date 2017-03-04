package com.knottycode.drivesafe;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.List;

/**
 * Created by thammaknot on 2/19/17.
 */
public class ASRListener implements RecognitionListener {
    private static final String TAG = "**ASR Listener**";

    private List<String> results;
    private BaseDriveModeActivity activity;
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
        Log.d(TAG, "onResults inside ASR Listener");
        results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < results.size(); i++)
        {
            Log.d(TAG, "result " + results.get(i));
        }
        activity.onASRResultsReady(results);
    }

    public void onBeginningOfSpeech() {
        isListening = true;
    }

    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    public void onRmsChanged(float rmsdB) {}

    public void onBufferReceived(byte[] buffer) {
    }

    public void onEndOfSpeech() {
        isListening = false;
    }

    public void onError(int error) {
        Log.d(TAG,  "error " +  error);
        if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            // Possibly empty input audio, restart ASR.
            startRecognition();
        }
    }

    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "onPartialResults");
    }

    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "onEvent " + eventType);
    }

    public boolean isListening() {
        return isListening;
    }

    public void setRecognizer(SpeechRecognizer recognizer, Intent intent) {
        this.recognizer = recognizer;
        this.intent = intent;
    }

    public void startRecognition() {
        if (asrActive) {
            recognizer.stopListening();
        }
        asrActive = true;
        recognizer.startListening(intent);
    }

    public void stopRecognition() {
        asrActive = false;
        recognizer.stopListening();
        recognizer.cancel();
    }

    public boolean isAsrActive() {
        return asrActive;
    }
}
