package com.princedhaliwal.activitytracker;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static com.princedhaliwal.activitytracker.FriendsActivityJava.TAG;

public class ContactFragment extends Fragment {
    private String name;
    private Integer id;
    private String value;

    private String calories;
    private String distance;

    private TextView stepsView;

    public static ContactFragment newInstance(String name, Integer id, String value, String calorie, String distance) {

        Bundle args = new Bundle();

        args.putString("name", name);
        args.putInt("id", id);
        args.putString("value", value);
        args.putString("calories", calorie);
        args.putString("distance", distance);

        ContactFragment fragment = new ContactFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        name = args.getString("name");
        id = args.getInt("id");
        value = args.getString("value");
        calories = args.getString("calories");
        distance = args.getString("distance");

        if (calories.indexOf('.') > 0)
            calories = (calories.substring(0, calories.indexOf('.')));
        if (distance.indexOf('.') > 0)
            distance = (distance.substring(0, distance.indexOf('.')));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    void renderActivity() {
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        String url = "http://ec2-52-15-146-42.us-east-2.compute.amazonaws.com/user/" + id + "/" + 6;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                updateUI(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "" + error);
            }
        });

        requestQueue.add(request);
    }

    void updateUI(JSONObject response) {
        try {
            if (response.has("value")) {
                String value = response.getString("value");
                stepsView.setText(value);
            }
        } catch (Exception e) {
            Log.e(TAG, "" + e);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textView = (TextView)view.findViewById(R.id.friend_name);
        textView.setText(name);
        stepsView = (TextView)view.findViewById(R.id.friend_activity);
        stepsView.setText(value);

        ((TextView)view.findViewById(R.id.friend_calorie)).setText(calories);
        ((TextView)view.findViewById(R.id.friend_distance)).setText(distance);
    }
}
