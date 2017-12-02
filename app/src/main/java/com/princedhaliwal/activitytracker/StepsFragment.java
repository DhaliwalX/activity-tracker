package com.princedhaliwal.activitytracker;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.data.Value;
import com.princedhaliwal.activitytracker.activities.kt.ActivityTrackerCallback;
import com.princedhaliwal.activitytracker.models.User;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StepsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StepsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StepsFragment extends Fragment implements ActivityTrackerCallback, View.OnClickListener {
    private static final String ARG_PARAM1 = "steps";

    private String steps = "0";
    private TextView stepsView;
    private TextView distanceView;
    private TextView caloriesView;
    private ProgressBar progressBar;
    private LinearLayout linearLayout;
    private String distance = "0";
    private String calories = "0";
    private OnFragmentInteractionListener mListener;

    public StepsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param steps Parameter 1.
     * @return A new instance of fragment StepsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StepsFragment newInstance(Long steps) {
        StepsFragment fragment = new StepsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, steps.toString());
        fragment.setArguments(args);
        return fragment;
    }

    public void setSteps(String steps) {
        this.steps = steps;

        if (stepsView != null) {
            stepsView.setText(steps);
        }
    }

    @Override
    public void onCaloriesChanged(Value data) {
        String calString = String.format("%2.0f", data.asFloat());
        setCalories(calString);
        MainActivity.saveToServer("calories", "float", data.asFloat());
    }

    @Override
    public void onDistanceMeasured(Value d) {
        float distance = d.asFloat();
        String unit = "m";
        if (distance - 1000 > 0) {
            distance = distance / 1000;
            unit = "km";
        }

        String disString = String.format("%2.2f", distance);
        setDistance(disString, unit);

        MainActivity.saveToServer("distance", "float", d.asFloat());
    }

    @Override
    public void onStepsChanged(Value steps) {
        setSteps(Integer.toString(steps.asInt()));

        MainActivity.saveToServer("steps", "int", steps.asInt());
    }

    @Override
    public void onError(Status status) {
        Toast.makeText(getContext(), status.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            steps = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        stepsView = (TextView)getView().findViewById(R.id.steps);
        stepsView.setText(steps.toString());

        distanceView = (TextView)getView().findViewById(R.id.distance);
        SpannableString str = new SpannableString(distance + "km");
        str.setSpan(new ForegroundColorSpan(Color.GRAY), distance.length(), str.length(), distance.length());
        distanceView.setText(str);

        caloriesView = (TextView) getView().findViewById(R.id.calories);
        caloriesView.setText(calories);

        TextView desc = (TextView) getView().findViewById(R.id.steps_desc);
        progressBar = (ProgressBar)getView().findViewById(R.id.progressbar);
//        progressBar.setVisibility(ProgressBar.VISIBLE);
        linearLayout = (LinearLayout) getView().findViewById(R.id.linear_layout);

        final RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        String url = "http://pecfest.in/v1/user/" + "PRINCE";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        User user = null;
                        try {
                            user = User.fromJSON(response);
                            Log.i("HelloFragment", user.toString());
                            handleUser(user);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("ERROR", error.toString());
                            Toast.makeText(getActivity(), "Could not get user info.", Toast.LENGTH_SHORT).show();
                    }
        });

        ((ImageButton)view.findViewById(R.id.share_button)).setOnClickListener(this);

        requestQueue.add(request);
    }

    protected void handleUser(User user) {
        progressBar.setVisibility(ProgressBar.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_steps, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setDistance(String distance, String unit) {
        this.distance = distance;

        if (distanceView != null) {
            SpannableString str = new SpannableString(distance + unit);
            str.setSpan(new ForegroundColorSpan(Color.GRAY), distance.length(), str.length(), distance.length());
            distanceView.setText(str);
        }
    }

    public void setCalories(String calories) {
        this.calories = calories;

        if (caloriesView != null) {
            caloriesView.setText(calories);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    void shareScreenshot() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, "Track your fitness on PEC Fit+. \nToday's Step Count: " + steps);
        intent.setType("text/plain");
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.share_button:
                Log.i(MainActivity.TAG, "Share button clicked.");
                shareScreenshot();
                break;
        }
    }
}
