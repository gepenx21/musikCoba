package com.appodeals.musikcoba;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.appodeals.musikcoba.Request.RequestPermissionHandler;
import com.appodeals.musikcoba.Utility.ScrollTextView;
import com.appodeals.musikcoba.Utility.Utility;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
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
//    private LinearLayout no_conn;
//    private Button btn_reload;
    Toolbar toolbar;
    Drawer result;
    private ProgressDialog pDialog;
    private String title;

    final Handler mHandler = new Handler();
    private ScrollTextView tb_title;
    Animation anim = new AlphaAnimation(0.0f, 1.0f);
    boolean mBlinking = false;
    FragmentManager fm = getSupportFragmentManager();
    private RequestPermissionHandler mRequestPermissionHandler;
    ApiRequest apiRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Appodeal.initialize(this,getResources().getString(R.string.app_id),
                Appodeal.BANNER | Appodeal.INTERSTITIAL);
        Appodeal.disableLocationPermissionCheck();
        Appodeal.setTesting(true);
        initializeViews();

        mRequestPermissionHandler = new RequestPermissionHandler();
        permissionRequest();

//        checkInternetAvailibility();
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
        LinearLayout linearlayout = findViewById(R.id.adView);
        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.show(this, Appodeal.BANNER_VIEW, String.valueOf(linearlayout));
        recycler.setAdapter(mAdapter);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(this::togglePlay);

        anim.setDuration(500); //manage the blinking time
        anim.setStartOffset(50);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        mediaPlayer.setOnCompletionListener(mp -> {
            if (Config.isPlaying){
                Config.isPlaying=false;
            }
            if(currentIndex + 1 < songList.size()){
                Song next = songList.get(currentIndex + 1);
                changeSelectedSong(currentIndex+1);
                prepareSong(next);
            }else{
                Song next = songList.get(0);
                changeSelectedSong(0);
                prepareSong(next);
            }
            Appodeal.show(MainActivity.this, Appodeal.INTERSTITIAL);
        });
        handleSeekbar();
        pushPlay();
        pushPrevious();
        pushNext();
        pushShare();
        pushInfo();
//        reloadBtn();
        initDrawer();
    }

    private void permissionRequest(){
        mRequestPermissionHandler.requestPermission(this, new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        }, 123, new RequestPermissionHandler.RequestPermissionListener() {
            @Override
            public void onSuccess() {
//                for (int j = 0 ; j<apiRequest.listLenght;j++) {
//                    new DownloadFiles().execute("URL");
//                }
                getSongList();
                //Toast.makeText(MainActivity.this, "request permission success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed() {
                //Toast.makeText(MainActivity.this, "request permission failed", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mRequestPermissionHandler.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
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
                        case 5:
                            Intent i = new Intent(MainActivity.this, PrivacyPolicy.class);
                            i.putExtra("TITLE", "Privacy Policy");
                            startActivity(i);
                            break;
                        case 6:
                            Intent in = new Intent(MainActivity.this, PrivacyPolicy.class);
                            in.putExtra("TITLE", "Disclaimer");
                            startActivity(in);
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

        Appodeal.show(MainActivity.this, Appodeal.INTERSTITIAL);
        Config.isPlaying = true;
        title = song.getTitle();
        long currentSongLength = song.getDuration();
        pb_main_loader.setVisibility(View.VISIBLE);
        //tb_title.setVisibility(View.GONE);
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
//        no_conn = findViewById(R.id.no_conn);
//        btn_reload = findViewById(R.id.btn_reload);
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

//    private void reloadBtn() {
//        btn_reload.setOnClickListener(v -> {
//            getSongList();
//            recycler.setVisibility(View.VISIBLE);
//            no_conn.setVisibility(View.GONE);
//        });
//    }

//    public void checkInternetAvailibility()
//    {
//        if(isInternetAvailable(this))
//        {
//            Intent intent = getIntent();
//            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//            alertDialog.setCancelable(false);
//            alertDialog.setTitle("Error");
//            alertDialog.setMessage("Please check your internet connection and try again.");
//            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Try Again", (dialog, which) -> {
//                finish();
//                startActivity(intent);
//            });
//        }
//        else {
//            getSongList();
//        }
//    }

//    public static boolean isInternetAvailable(Context context) {
//        if(context == null)  return false;
//
//        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//
//        if (connectivityManager != null) {
//
//
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
//                if (capabilities != null) {
//                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
//                        return true;
//                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
//                        return true;
//                    }  else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
//                        return true;
//                    }
//                }
//            }
//
//            else {
//
//                try {
//                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
//                        Log.i("update_status", "Network is available : true");
//                        return true;
//                    }
//                } catch (Exception e) {
//                    Log.i("update_status", "" + e.getMessage());
//                }
//            }
//        }
//        Log.i("update_status","Network is available : FALSE ");
//        return false;
//    }

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
            Appodeal.show(MainActivity.this, Appodeal.INTERSTITIAL);
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
            Appodeal.show(MainActivity.this, Appodeal.INTERSTITIAL);
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

    class DownloadFiles extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("Starting download");

            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Loading... Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setMax(100);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... fUrl) {
            try {
                URL url = new URL(fUrl[0]);
                URLConnection conn = url.openConnection();
                conn.connect();

                int fileLenght = conn.getContentLength();
                InputStream inputStream = new BufferedInputStream(url.openStream());
                String fileName = title+".mp3";
                OutputStream outputStream = getBaseContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                byte[] data = new byte[1024];
                int count = inputStream.read(data);
                long total = count;

                while (count != -1) {
                    outputStream.write(data, 0, count);
                    count = inputStream.read(data);
                    total += count;
                    publishProgress(""+ (int) ((total * 100)/fileLenght));
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return null;
        }
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            pDialog.dismiss();

        }
    }
}
