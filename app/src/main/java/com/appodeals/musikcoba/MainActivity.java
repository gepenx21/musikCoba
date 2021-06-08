package com.appodeals.musikcoba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.appodeal.ads.Appodeal;
import com.appodeals.musikcoba.Adapter.SongAdapter;
import com.appodeals.musikcoba.Model.Song;
import com.appodeals.musikcoba.Request.ApiRequest;
import com.appodeals.musikcoba.Utility.ScrollTextView;
import com.appodeals.musikcoba.Utility.Utility;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private static final String CONSENT = "consent";
    private RecyclerView recycler;
    private SongAdapter mAdapter;
    public static ArrayList<Song> songList;
    private int currentIndex;
    private TextView tv_time, total_duration;
    private ImageView iv_play, iv_next, iv_previous, iv_share, iv_info;
    private ProgressBar pb_main_loader;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private boolean firstLaunch = true;
    Toolbar toolbar;
    Drawer result;

    final Handler mHandler = new Handler();
    private ScrollTextView tb_title;
    Animation anim = new AlphaAnimation(0.0f, 1.0f);
    boolean mBlinking = false;
    FragmentManager fm = getSupportFragmentManager();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Appodeal.initialize(this, getResources().getString(R.string.app_id),
                Appodeal.BANNER | Appodeal.INTERSTITIAL);
        LinearLayout linearlayout = findViewById(R.id.adView);
        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.show(this, Appodeal.BANNER_VIEW, String.valueOf(linearlayout));
        Appodeal.disableLocationPermissionCheck();
        Appodeal.setTesting(true);
        initializeViews();
        getSongList();

        songList = new ArrayList<>();
        recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mAdapter = new SongAdapter(getApplicationContext(), songList, (song, position) -> {
            firstLaunch = false;
            changeSelectedSong(position);
            if (mBlinking) {
                mBlinking = false;
                tv_time.clearAnimation();
                tv_time.setAlpha(1.0f);
            }
            prepareSong(song);
        });

        recycler.setAdapter(mAdapter);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this::togglePlay);

        anim.setDuration(500); //manage the blinking time
        anim.setStartOffset(50);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        mediaPlayer.setOnCompletionListener(mp -> {
            if (Config.isPlaying) {
                Config.isPlaying = false;
            }
            if (currentIndex + 1 < songList.size()) {
                Song next = songList.get(currentIndex + 1);
                changeSelectedSong(currentIndex + 1);
                prepareSong(next);
            } else {
                Song next = songList.get(0);
                changeSelectedSong(0);
                prepareSong(next);
            }
            Config.showIntersititial(this, true);
        });
        handleSeekbar();
        pushPlay();
        pushPrevious();
        pushNext();
        pushShare();
        pushInfo();
        initDrawer();
    }

    public static Intent getIntent(Context context, boolean consent) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(CONSENT, consent);
        return intent;
    }

    public void initDrawer(){
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        if (toolbar != null)
        setSupportActionBar(toolbar);
        @SuppressLint("UseCompatLoadingForDrawables") AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withCompactStyle(false)
                .addProfiles(new ProfileDrawerItem().
                        withName(getString(R.string.AUTHOR))
                        .withEmail(getString(R.string.PUBLISHER_EMAIL))
                        .withIcon(getResources().getDrawable(R.drawable.menu_profile)))
                .withOnAccountHeaderListener((view, profile, currentProfile) -> false)
                .build();

        result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .inflateMenu(R.menu.main_menu)
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    switch (position) {
                        case 2:
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, "Download " + getString(R.string.app_name) + " in : " + "\n" + "https://play.google.com/store/apps/details?id=" + getPackageName());
                            sendIntent.setType("text/plain");
                            startActivity(sendIntent);
                            break;
                        case 3:
                            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName());
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                            break;
                        case 4:
                            Intent intents = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.MORE_APP)));
                            startActivity(intents);
                            break;
                        default:
                            break;
                    }
                    return false;
                })
                .build();
        result.setSelection(-1);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
    }

    private void handleSeekbar(){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void prepareSong(Song song){
        Config.showIntersititial(this, true);
        Config.isPlaying = true;
        String title = song.getTitle();
        long currentSongLength = song.getDuration();
        pb_main_loader.setVisibility(View.VISIBLE);
        iv_play.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.selector_play));
        tb_title.setText(title);
        tb_title.startScroll();
        total_duration.setText(Utility.milliSecondsToTimer(currentSongLength));
        String stream = song.getStreamUrl();
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(stream);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void togglePlay(MediaPlayer mp){
        if(mp.isPlaying()){
            mp.stop();
            mp.reset();

        }else{
            pb_main_loader.setVisibility(View.GONE);
            tb_title.setVisibility(View.VISIBLE);
            seekBar.setMax(mp.getDuration() / 1000);
            mp.start();
            iv_play.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.selector_pause));
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    tv_time.setText(Utility.milliSecondsToTimer(mediaPlayer.getCurrentPosition()));
                    mHandler.postDelayed(this, 1000);
                }
            });
        }
    }

    private void initializeViews(){

        tb_title = findViewById(R.id.tb_title);
        iv_play = findViewById(R.id.iv_play);
        iv_next = findViewById(R.id.iv_next);
        iv_previous = findViewById(R.id.iv_previous);
        total_duration = findViewById(R.id.total_duration);
        pb_main_loader = findViewById(R.id.pb_main_loader);
        recycler = findViewById(R.id.recylerView);
        seekBar = findViewById(R.id.seekbar);
        tv_time = findViewById(R.id.tv_time);
        iv_share = findViewById(R.id.share);
        iv_info = findViewById(R.id.about);
    }

    public void getSongList(){
        RequestQueue queue = VolleySingleton.getInstance(this).getRequestQueue();
        ApiRequest request = new ApiRequest(queue);
        request.getSongList(new ApiRequest.ApiInterface() {
            @Override
            public void onSuccess(ArrayList<Song> songs) {
                currentIndex = 0;
                songList.clear();
                songList.addAll(songs);
                mAdapter.notifyDataSetChanged();
                mAdapter.setSelectedPosition(0);
            }
            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changeSelectedSong(int index){
        mAdapter.notifyItemChanged(mAdapter.getSelectedPosition());
        currentIndex = index;
        mAdapter.setSelectedPosition(currentIndex);
        mAdapter.notifyItemChanged(currentIndex);
    }

    private void pushPlay(){
        iv_play.setOnClickListener(v -> {
            if(mediaPlayer.isPlaying() && mediaPlayer != null){
                iv_play.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.selector_play));
                mediaPlayer.pause();
                tv_time.startAnimation(anim);
                tb_title.pauseScroll();
                mBlinking = true;
            }else{
                if(firstLaunch){
                    Song song = songList.get(0);
                    changeSelectedSong(0);
                    prepareSong(song);
                }else{
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
                    firstLaunch = false;
                }
                if (tb_title.isPaused()) {
                    tb_title.resumeScroll();
                }
                iv_play.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.selector_pause));
                tv_time.clearAnimation();
                tv_time.setAlpha(1.0f);
                mBlinking = false;
            }
        });
    }

    private void pushShare() {
        iv_share.setOnClickListener(v -> {
            Utility.animateButton(iv_share);
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = getString(R.string.share)+ getPackageName() +" \n";
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name)+" Application");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        });
    }

    private void pushInfo() {
        iv_info.setOnClickListener(v -> {
            Utility.animateButton(iv_info);
            AboutFragment aboutFragment = new AboutFragment();
            aboutFragment.show(fm, "about fragment");
        });
    }

    private void pushPrevious(){

        iv_previous.setOnClickListener(v -> {
            Config.showIntersititial(this, true);
            firstLaunch = false;
            if(mediaPlayer != null){
                if(currentIndex - 1 >= 0){
                    Song previous = songList.get(currentIndex - 1);
                    changeSelectedSong(currentIndex - 1);
                    prepareSong(previous);
                }else{
                    changeSelectedSong(songList.size() - 1);
                    prepareSong(songList.get(songList.size() - 1));
                }
            }
        });
    }

    private void pushNext(){
        iv_next.setOnClickListener(v -> {
            Config.showIntersititial(this, true);
            firstLaunch = false;
            if(mediaPlayer != null){
                if(currentIndex + 1 < songList.size()){
                    Song next = songList.get(currentIndex + 1);
                    changeSelectedSong(currentIndex + 1);
                    prepareSong(next);
                }else{
                    changeSelectedSong(0);
                    prepareSong(songList.get(0));
                }

            }
        });
    }

    @Override
    public void onBackPressed() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle("Confirm quit");
            builder.setMessage("Are you sure?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                //super.onBackPressed();
                //if user pressed "yes", then he is allowed to exit from application
                if (mediaPlayer != null ) {
                    if(mediaPlayer.isPlaying()){
                        try {
                            mediaPlayer.stop();
                            mediaPlayer.release();
                            mHandler.removeCallbacksAndMessages(null);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                finishAffinity();
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                //if user select "No", just cancel this dialog and continue with app
                dialog.cancel();
            });
        builder.show();
    }
}
