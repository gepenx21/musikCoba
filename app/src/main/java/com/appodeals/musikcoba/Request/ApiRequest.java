package com.appodeals.musikcoba.Request;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.appodeals.musikcoba.Model.Song;
import com.appodeals.musikcoba.Utility.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.appodeals.musikcoba.Config.JSON_ID;
import static com.appodeals.musikcoba.Config.URL;

public class ApiRequest {

    public interface ApiInterface{
        void onSuccess(ArrayList<Song> songs);
        void onError(String message);
    }

    private RequestQueue queue;
    private static final String JSON_URL = URL + JSON_ID;
    private static final String TAG = "APP";
    public int listLenght;
    
    public ApiRequest(RequestQueue queue) {
        this.queue = queue;
    }

    public void getSongList(final ApiInterface callback){

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, JSON_URL, null, response -> {
            ArrayList<Song> songs = new ArrayList<>();
            if (response.length() > 0) {
                listLenght += response.length();
                for (int i = 0; i < response.length(); i++) {
                    try {
                        JSONObject songObject = response.getJSONObject(i);
                        String artist = songObject.getString("singer");
                        String title = songObject.getString("song");
                        String streamUrl = songObject.getString("source");
                        String durasi = songObject.getString("duration");
                        int duration = Utility.calculateTime(durasi);

                        Song song = new Song(artist, title, duration, streamUrl);
                        songs.add(song);

                    } catch (JSONException e) {
                        Log.d(TAG, "onResponse: " + e.getMessage());
                        callback.onError("Under Maintenance");
                        e.printStackTrace();
                    }
                }
                callback.onSuccess(songs);

            } else {
                callback.onError("No songs found");
            }
        }, error -> {

        });
        Log.d(TAG, "getSongList: " + JSON_URL);

        queue.add(request);

    }
}
