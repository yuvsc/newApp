package com.example.yuval.newapp;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.speech.v1beta1.Speech;
import com.google.api.services.speech.v1beta1.SpeechRequestInitializer;
import com.google.api.services.speech.v1beta1.model.RecognitionAudio;
import com.google.api.services.speech.v1beta1.model.RecognitionConfig;
import com.google.api.services.speech.v1beta1.model.SpeechRecognitionResult;
import com.google.api.services.speech.v1beta1.model.SyncRecognizeRequest;
import com.google.api.services.speech.v1beta1.model.SyncRecognizeResponse;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private final String CLOUD_API_KEY = "AIzaSyDyp0yvPqn1MrJDGNpHkCSQCek5RsSW8oM";

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button browseButton = (Button) findViewById(R.id.browse_button);

        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent filePicker = new Intent(Intent.ACTION_GET_CONTENT);
                filePicker.setType("audio/flac");
                startActivityForResult(filePicker, 1);
            }


        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri soundUri = data.getData();

            showProgress(true);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    InputStream stream = null;
                    try {
                        stream = getContentResolver()
                                .openInputStream(soundUri);
                        byte[] audioData = IOUtils.toByteArray(stream);
                        stream.close();
                        String base64EncodedData = Base64.encodeBase64String(audioData);

                        Speech speechService = new Speech.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new AndroidJsonFactory(),
                                null
                        ).setSpeechRequestInitializer(
                                new SpeechRequestInitializer(CLOUD_API_KEY))
                                .build();
                        RecognitionConfig recognitionConfig = new RecognitionConfig();
                        recognitionConfig.setLanguageCode("en-US");
                        RecognitionAudio recognitionAudio = new RecognitionAudio();
                        recognitionAudio.setContent(base64EncodedData);

                        // Create request
                        SyncRecognizeRequest request = new SyncRecognizeRequest();
                        request.setConfig(recognitionConfig);
                        request.setAudio(recognitionAudio);

                        // Generate response
                        SyncRecognizeResponse response = null;
                        try {
                            response = speechService.speech().syncrecognize(request).execute();

                            // Extract transcript
                            SpeechRecognitionResult result = response.getResults().get(0);
                            final String transcript = result.getAlternatives().get(0)
                                    .getTranscript();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView speechToTextResult =
                                            (TextView) findViewById(R.id.text_view);
                                    speechToTextResult.setText(transcript);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // More code here

                    //TODO: This is probably not the best way to do it but it works for now.
                    // You have to run on ui thread here because AsyncTasks run in background.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(false);
                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
