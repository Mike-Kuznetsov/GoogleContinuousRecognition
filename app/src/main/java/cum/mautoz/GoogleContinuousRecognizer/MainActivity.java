package cum.mautoz.GoogleContinuousRecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.SeekBar;
import android.widget.TextView;

import cum.mautoz.GoogleContinuousRecognizer.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener{
    MediaRecorder recorder;
    private SpeechRecognizer speechRecognizer;
    private static final short PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    TextView resultView;
    private String result;
    Timer timer;
    private int activationValue=70;
    private TextView mTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        muteAudio();
        resultView = findViewById(R.id.result_text);
        result = "";
        mTextView = (TextView)findViewById(R.id.activation);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            recorderInit();
        }
        final SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

        mTextView = (TextView)findViewById(R.id.activation);
        mTextView.setText("Activation threshold: 70");
    }
    public void muteAudio(){
        AudioManager mAlramMAnager = (AudioManager) this.getSystemService(this.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            //mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        } else {
            mAlramMAnager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_ALARM, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_RING, true);
            mAlramMAnager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }
    private void recorderInit(){
        recorder=new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        timer = new Timer();
        timer.scheduleAtFixedRate(new RecorderTask(recorder), 0, 100);
        recorder.setOutputFile("/dev/null");

        try {
            recorder.prepare();
            recorder.start();
        } catch(IllegalStateException e)
        {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //startRecognition();
    }
    private void startRecognition() {
        timer.cancel();
        recorder.stop();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new listener());
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US");
        speechRecognizer.startListening(intent);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                recorderInit();
            } else {
                finish();
            }
        }
    }
    private class RecorderTask extends TimerTask {
        TextView sound = (TextView) findViewById(R.id.DB);
        private MediaRecorder recorder;

        public RecorderTask(MediaRecorder recorder) {
            this.recorder = recorder;
        }

        public void run() {
            int amplitude = recorder.getMaxAmplitude();
            double amplitudeDb = 20 * Math.log10((double)Math.abs(amplitude));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sound.setText("Volume: " + amplitudeDb);
                    if (amplitudeDb>activationValue){
                        startRecognition();
                    }
                }
            });

        }
    }
    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params){}
        public void onBeginningOfSpeech() {}
        public void onRmsChanged(float rmsdB) {}
        public void onBufferReceived(byte[] buffer){}
        public void onEndOfSpeech(){
            //Toast.makeText(getApplicationContext(), "onEndOfSpeech", Toast.LENGTH_SHORT).show();
        }
        public void onError(int error){
            //Toast.makeText(getApplicationContext(), "onError", Toast.LENGTH_SHORT).show();
            recorderInit();
        }
        public void onResults(Bundle results){
            //Toast.makeText(getApplicationContext(), "On result", Toast.LENGTH_LONG).show();

           ArrayList<String> matches = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            //Toast.makeText(this, matches.get(0), Toast.LENGTH_LONG).show();
            //Log.e("onResults: ", matches.get(0)) ;

            result=result+" "+matches.get(0);
            resultView = findViewById(R.id.result_text);
            resultView.setText(result);

            recorderInit();

        }
        public void onPartialResults(Bundle partialResults){}
        public void onEvent(int eventType, Bundle params){
            //Toast.makeText(getApplicationContext(), "onEvent", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        activationValue=seekBar.getProgress();
        mTextView.setText("Activation threshold: "+String.valueOf(seekBar.getProgress()));

    }


}