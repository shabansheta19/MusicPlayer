package com.example.shaban.musicplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Button playPauseBtn;
    private Button prevBtn;
    private Button nextBtn;
    private Button forwardBtn;
    private Button backWardBtn;
    private SeekBar seekBar;
    private TextView leftTime;
    private TextView durationTime;
    private ListView listView;
    private Thread thread;
    private ArrayList<String> songsTitles = new ArrayList<>();
    private ArrayList<String> songsPaths = new ArrayList<>();
    private NotificationCompat.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();
        setButtonsClickListener();
        getSongs();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,songsTitles);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Uri song = MediaStore.Audio.Media.getContentUriForPath(songsPaths.get(position));
                Toast.makeText(getApplicationContext(),song.toString(),Toast.LENGTH_LONG).show();
                //mediaPlayer.release();
                //mediaPlayer = new MediaPlayer();
                //mediaPlayer = MediaPlayer.create(getApplicationContext(),song);
                //mediaPlayer.start();
            }
        });

        mediaPlayer = new MediaPlayer();
        mediaPlayer = MediaPlayer.create(getApplicationContext(),R.raw.fatha);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Toast.makeText(getApplicationContext(),"finish",Toast.LENGTH_LONG).show();
                seekBar.setProgress(0);
                mediaPlayer.seekTo(0);
                playPauseBtn.setBackgroundResource(R.drawable.play);
            }
        });

        seekBar.setMax(mediaPlayer.getDuration());
    }

    @Override
    protected void onDestroy() {
        if(mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        thread.interrupt();
        thread = null;
        super.onDestroy();
    }

    private void setupUI() {
        seekBar = (SeekBar)findViewById(R.id.SongSeekBar);
        playPauseBtn = (Button)findViewById(R.id.playPauseBtn);
        prevBtn = (Button)findViewById(R.id.prevBtn);
        nextBtn = (Button)findViewById(R.id.nextBtn);
        backWardBtn = (Button)findViewById(R.id.backwardBtn);
        forwardBtn = (Button)findViewById(R.id.forwardBtn);
        leftTime = (TextView)findViewById(R.id.leftTimeTextView);
        durationTime = (TextView)findViewById(R.id.durationTimeTextView);
        listView = (ListView)findViewById(R.id.songsListView);
    }

    private void setButtonsClickListener() {
        //play pause button
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        playPauseBtn.setBackgroundResource(R.drawable.play);
                    } else {
                        mediaPlayer.start();
                        updateSeekBar();
                        playPauseBtn.setBackgroundResource(R.drawable.pause);
                    }
                }
            }
        });

        //seek bar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)
                    mediaPlayer.seekTo(progress);

                int currentPos = mediaPlayer.getCurrentPosition() / 1000;
                int duration = mediaPlayer.getDuration() / 1000;
                updateTextViews(currentPos,duration);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //forward button
        forwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPos = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                currentPos += 5000;
                if(currentPos > duration)
                    currentPos = duration;
                seekBar.setMax(duration);
                seekBar.setProgress(currentPos);
                mediaPlayer.seekTo(currentPos);
                updateTextViews(currentPos/1000 , duration /1000);

            }
        });

        //backward button
        backWardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPos = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                currentPos -= 5000;
                if(currentPos < 0)
                    currentPos = 0;
                seekBar.setMax(duration);
                seekBar.setProgress(currentPos);
                mediaPlayer.seekTo(currentPos);
                updateTextViews(currentPos/1000 , duration /1000);

            }
        });
    }

    private void updateSeekBar() {
        thread = new Thread() {
            @Override
            public void run() {
                while(mediaPlayer != null && mediaPlayer.isPlaying()) {
                    try {
                        Thread.sleep(50);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int currentPos = mediaPlayer.getCurrentPosition();
                                int duration = mediaPlayer.getDuration();
                                seekBar.setMax(duration);
                                seekBar.setProgress(currentPos);
                                updateTextViews(currentPos/1000 , duration /1000);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        thread.start();
    }

    private  void  updateTextViews(int currentTime , int TotalTime) {
        leftTime.setText((currentTime / 60) + ":" + (currentTime % 60));
        durationTime.setText((TotalTime / 60) + ":" + (TotalTime % 60));
    }

    private void getSongs() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri,null,null,null,null);

        if (songCursor != null && songCursor.moveToFirst()) {
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songPath = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do {
                String title = songCursor.getString(songTitle);
                songsTitles.add(title);
                songsPaths.add(songCursor.getString(songPath));
            } while (songCursor.moveToNext());
        }
    }

    private void buildNotification() {
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentTitle("Notification Alert, Click Me!");
        mBuilder.setContentText("Hi, This is Android Notification Detail!");

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
    }

    private void issueNotification () {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // notificationID allows you to update the notification later on.
        mNotificationManager.notify(100, mBuilder.build());
    }
}
