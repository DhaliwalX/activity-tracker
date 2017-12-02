package com.princedhaliwal.activitytracker;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class FriendsActivityJava extends AppCompatActivity {

    private ProgressBar progressBar;
    public final static String TAG = "FriendsActivityJava";

    void renderNoFriends() {
        Log.i(TAG, "No friends found for the user");
    }

    void removeProgressBar() {
        progressBar.setVisibility(GONE);
    }

    void showProgressBar() {
        progressBar.setVisibility(VISIBLE);
        progressBar.setIndeterminate(true);
        ((ViewGroup)findViewById(R.id.friends_container)).addView(progressBar);
    }

    void showFriends(JSONArray response) {
        removeProgressBar();
        if (response == null) {
            renderNoFriends();
            return;
        }

        if (response.length() == 0) {
            renderNoFriends();
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> friendsList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < response.length(); i++) {
            try {
                HashMap<String, Object> user = (HashMap<String, Object>) new ObjectMapper().readValue(response.get(i).toString(), HashMap.class);
                Log.i(TAG, user.toString());
                friendsList.add(user);
            } catch (Exception e) {
                Log.e(TAG, "Exception occured");
                renderNoFriends();
            }
        }

        renderFriends(friendsList);
    }

    void renderFriends(List<Map<String, Object>> list) {
        LinearLayout layout = (LinearLayout) findViewById(R.id.friends_list);
        if (layout == null) {
            Log.i(TAG, "Layout is null");
        }

        Collections.sort(list, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String a = (String)o2.get("activity");
                String b = (String)o1.get("activity");
                if (a.length() != b.length())
                    return a.length() - b.length();
                return a.compareTo(b);
            }
        });

        for (Map<String, Object> friend : list) {
            ContactFragment fragment = ContactFragment.newInstance((String)friend.get("name"),
                    (Integer)friend.get("id"),
                    (String)friend.get("activity"),
                    (String)friend.get("calorie"),
                    (String)friend.get("distance")
            );
            FragmentManager manager = getFragmentManager();
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            fragmentTransaction.add(R.id.friends_list, fragment);
            fragmentTransaction.commit();
        }
    }

    void renderFriendsList() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        Integer userId = getSharedPreferences(getString(R.string.preferences_file), MODE_PRIVATE).getInt("userId", -1);

        if (userId == -1) {
            renderNoFriends();
            return;
        }

        Log.i(TAG, "Fetching friends for " + userId);

        String url = "http://ec2-52-15-146-42.us-east-2.compute.amazonaws.com/user/" + userId +"/friends/" + 6 + "/7/8";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        final JSONArray resp = response;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "Rendering friends.");
                                showFriends(resp);
                            }
                        });
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, "" + error);
                    }
                });
        showProgressBar();

        requestQueue.add(request);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        progressBar = new ProgressBar(this);
//        renderFriendsList();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Log.i(TAG, "Starting " + FriendsActivityJava.class.getName());
        ((MyTextView)findViewById(R.id.textView3)).setBold();
        renderFriendsList();
    }
}
