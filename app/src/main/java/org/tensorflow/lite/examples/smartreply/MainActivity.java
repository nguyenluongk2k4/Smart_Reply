/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.examples.smartreply;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * The main (and only) activity of this demo app. Displays a text box which updates as messages are
 * received.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "SmartReplyDemo";
  private static final int SPEECH_REQUEST_CODE = 100;
  private SmartReplyClient client;
  private TextView messageTextView;
  private EditText messageInput;
  private ScrollView scrollView;
  private ImageButton micButton;

  private Handler handler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");
    setContentView(R.layout.tfe_sr_main_activity);

    client = new SmartReplyClient(getApplicationContext());
    handler = new Handler();

    scrollView = findViewById(R.id.scroll_view);
    messageTextView = findViewById(R.id.message_text);

    messageInput = findViewById(R.id.message_input);
    messageInput.setOnKeyListener(
        (view, keyCode, keyEvent) -> {
          if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_UP) {
            // Send message when pressing Enter on keyboard.
            send(messageInput.getText().toString());
            return true;
          }
          return false;
        });

    Button sendButton = findViewById(R.id.send_button);
    sendButton.setOnClickListener((View v) -> send(messageInput.getText().toString()));

    micButton = findViewById(R.id.mic_button);
    micButton.setOnClickListener((View v) -> startSpeechRecognition());
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.v(TAG, "onStart");
    handler.post(
        () -> {
          client.loadModel();
        });
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.v(TAG, "onStop");
    handler.post(
        () -> {
          client.unloadModel();
        });
  }

  private void send(final String message) {
    handler.post(
        () -> {
          StringBuilder textToShow = new StringBuilder();
          textToShow.append("Input: ").append(message).append("\n\n");

          // Get suggested replies from the model.
          SmartReply[] ans = client.predict(new String[] {message});
          for (SmartReply reply : ans) {
            textToShow.append("Reply: ").append(reply.getText()).append("\n");
          }
          textToShow.append("------").append("\n");

          runOnUiThread(
              () -> {
                // Show the message and suggested replies on screen.
                messageTextView.append(textToShow);

                // Clear the input box
                messageInput.setText(null);

                // Scroll to the bottom to show latest entry's classification result.
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
              });
        });
  }

  private void startSpeechRecognition() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói gì đó...");

    try {
      startActivityForResult(intent, SPEECH_REQUEST_CODE);
    } catch (Exception e) {
      Toast.makeText(this, "Thiết bị không hỗ trợ nhận dạng giọng nói", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
      ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
      if (results != null && !results.isEmpty()) {
        String recognizedText = results.get(0);
        messageInput.setText(recognizedText);
      }
    }
  }
}
