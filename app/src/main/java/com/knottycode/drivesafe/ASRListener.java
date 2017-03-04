package com.knottycode.drivesafe;

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
    private boolean isListening = false;

    public ASRListener(BaseDriveModeActivity activity) {
        this.activity = activity;
    }

    public void onResults(Bundle bundle) {
        Log.d(TAG, "onResults " + bundle);
        results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < results.size(); i++)
        {
            Log.d(TAG, "result " + results.get(i));
        }
        activity.onASRResultsReady(results);
    }

    public void onBeginningOfSpeech() {
        Log.d(TAG, "##############################\nonBeginningOfSpeech\n#####################");
        isListening = true;
    }

    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    public void onRmsChanged(float rmsdB) {}

    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>\nonBufferReceived\n>>>>>>>>>>>>>>>>>>>>>>>(" + buffer.length + ")");
    }

    public void onEndOfSpeech() {
        Log.d(TAG, "******************************\nonEndofSpeech\n*************************");
        isListening = false;
    }

    public void onError(int error) {
        Log.d(TAG,  "error " +  error);
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
}
