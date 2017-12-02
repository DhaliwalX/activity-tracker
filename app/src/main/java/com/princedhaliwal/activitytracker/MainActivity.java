package com.princedhaliwal.activitytracker;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.animation.DynamicAnimation;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.location.LocationServices;
import com.princedhaliwal.activitytracker.activities.kt.ActivityDataToUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_CONTACTS;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private ImageView imageView;
    private LinearLayout expandView;
    private FrameLayout frameLayout;
    public static final String TAG = "MyApp";
    private StepsFragment stepsFragment;
    private GoogleApiClient googleApiClient;
    private ActivityDataToUI activityDataToUI;
    private StepsFragment activityFragment;
    private ShareActionProvider shareActionProvider;

    private GestureDetector gestureDetector;

    boolean opened = false;

    ArrayAdapter<String> simpleAdapter;

    private static final int RC_SIGN_IN = 9001;

    private static final String[] LOCATION_AND_CONTACTS =
            {ACCESS_FINE_LOCATION, READ_CONTACTS};
    private static final int RC_CAMERA_PERM = 123;
    private static final int RC_CONTACTS_PERM = 124;

    private static boolean ready = false;
    private static Integer userId = 0;
    private static Queue<Map<String, Object>> requestQueue = new ArrayDeque<>();
    private static Context context;

    public MainActivity() {
        super();
        context = this;
    }

    public static boolean isReady() {
        return ready;
    }

    public static void setReady(boolean ready) {
        MainActivity.ready = ready;
    }

    protected void subscribe() {
        activityDataToUI = new ActivityDataToUI(googleApiClient, activityFragment);
        activityDataToUI.createSubscription();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "RequestCode" + " " + requestCode);

        if (requestCode != RC_SIGN_IN) {
            Toast.makeText(this, "RequestCode: " + requestCode, Toast.LENGTH_LONG).show();
            return;
        }

        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

        if (!result.isSuccess()) {
            Toast.makeText(this, "Unable to get information from account.", Toast.LENGTH_LONG).show();
            return;
        }
        GoogleSignInAccount acct = result.getSignInAccount();
        Log.i(TAG, "ACCOUNT: " + acct.getEmail());
        String personName = acct.getDisplayName();
        String personGivenName = acct.getGivenName();
        String personFamilyName = acct.getFamilyName();
        String personEmail = acct.getEmail();
        String personId = acct.getId();
        Uri personPhoto = acct.getPhotoUrl();

        String mobile = "";
        Map<String, Object> user = new HashMap<>();
        user.put("name", personName);
        user.put("email", personEmail);
        user.put("mobile", mobile);
        user.put("gender", "Male");

        // get the information about user from server
        // else store it in the database
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "http://ec2-52-15-146-42.us-east-2.compute.amazonaws.com/user";
        JSONObject jsonObject = new JSONObject(user);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(url, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString("status").equals("SUCCESS")) {
                        saveUserId(response.getInt("id"));
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "" + error);
            }
        });

        requestQueue.add(jsonRequest);
    }

    public static void saveToServer(String name, String dataType, Object value) {
        Log.i(TAG, "Saving to server: " + name);
        Map<String, Object> activityInfo = new HashMap<>();
        activityInfo.put("activity", name);
        activityInfo.put("dataType", dataType);
        activityInfo.put("value", value);

        requestQueue.add(activityInfo);
        if (isReady()) {
            saveActivitiesToServer();
        }
    }

    private static void saveActivitiesToServer() {
        RequestQueue q = Volley.newRequestQueue(context);


        while (!requestQueue.isEmpty()) {
            Map<String, Object> data = requestQueue.remove();
            String url = "http://ec2-52-15-146-42.us-east-2.compute.amazonaws.com/user/" + userId + "/activity";

            JSONObject body = new JSONObject(data);
            JsonObjectRequest request = new JsonObjectRequest(url, body, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (response.getString("status").equals("SUCCESS")) {
                            Log.i(TAG, "Successfully saved activity.");
                        } else {
                            Log.i(TAG, "Failed to save activity.");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "" + error);
                }
            });

            q.add(request);
        }
    }

    protected void saveUserId(int id) {
        Log.i(TAG, "Saving user: " + id);
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt("userId", id).apply();
        userId = id;
        setReady(true);
        saveActivitiesToServer();
    }

    protected void buildFitnessClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.GOALS_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(@Nullable Bundle bundle) {
                                    Log.i(TAG, "Connected!!");
                                    subscribe();
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    if (i == CAUSE_NETWORK_LOST) {
                                        Log.w(TAG, "Connection lost!!");
                                    } else if (i == CAUSE_SERVICE_DISCONNECTED) {
                                        Log.w(TAG, "Service disconnected!");
                                    }
                                }
                            }
                    )
                    .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Log.w(TAG, "Google Play services connection failed. Cause: " +
                                    connectionResult.toString());
                            Toast.makeText(getApplicationContext(), "Exception while connecting to Google Play services: " +
                                            connectionResult.getErrorMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }).build();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        GoogleApiClient apiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(apiClient);
        this.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private boolean hasLocationAndContactsPermissions() {
        return EasyPermissions.hasPermissions(this, READ_CONTACTS);
    }

    @AfterPermissionGranted(RC_CONTACTS_PERM)
    public void setExpandedView() {

        if (hasLocationAndContactsPermissions()) {
            List<String> list = new ArrayList<>();
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            while (phones.moveToNext()) {
                Map<String, String> map = new HashMap<>();
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                map.put("phoneNumber", phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                list.add(name);
            }
            phones.close();
            Set<String> uni = new HashSet<>();
            uni.addAll(list);
            list.clear();
            list.addAll(uni);
            Collections.sort(list);
            View view = getLayoutInflater().inflate(R.layout.friends_layout, null);
            ListView contactsList = (ListView) view.findViewById(R.id.contacts_list);

            simpleAdapter = new ArrayAdapter<String>(this, R.layout.fragment_contacts, R.id.friend_name, list);
            contactsList.setAdapter(simpleAdapter);
            expandView.addView(view);
        } else {
            EasyPermissions.requestPermissions(this,
                                                getString(R.string.permission_rationale),
                    RC_CONTACTS_PERM,
                    LOCATION_AND_CONTACTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    protected void removeExpandedView() {
        expandView.removeView(expandView.findViewById(R.id.friends_view));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        buildFitnessClient();
        AssetManager assetManager = getApplicationContext().getAssets();

        View view = getLayoutInflater().inflate(R.layout.list_header, null);
        frameLayout = (FrameLayout)findViewById(R.id.container);
        frameLayout.addView(view);

        View bottomView = getLayoutInflater().inflate(R.layout.expand_view, null);
        frameLayout.addView(bottomView);

        imageView = (ImageView)findViewById(R.id.imageView5);
        expandView = (LinearLayout)findViewById(R.id.expand_view);
        expandView.setBackgroundColor(Color.WHITE);
        setExpandedView();
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startFriendsActivity();
                if (true) {
                    return;
                }
                SpringForce spring = new SpringForce(0)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                        .setStiffness(SpringForce.STIFFNESS_LOW);
                if (!opened) {
                    expandView.setLayoutParams(new RelativeLayout.LayoutParams(frameLayout.getWidth(), frameLayout.getHeight()));

                    final SpringAnimation springAnimation = new SpringAnimation(expandView, DynamicAnimation.TRANSLATION_Y, 0);
                    springAnimation.setStartValue((frameLayout.getHeight() - imageView.getHeight()));
                    springAnimation.setSpring(spring);
                    springAnimation.start();
                    opened = true;

                } else {

                    final SpringAnimation springAnimation = new SpringAnimation(expandView, DynamicAnimation.TRANSLATION_Y, (frameLayout.getHeight() - imageView.getHeight()));
                    springAnimation.setStartValue(expandView.getTranslationY());
                    spring.setFinalPosition(frameLayout.getHeight() - imageView.getHeight());
                    springAnimation.setSpring(spring);
                    springAnimation.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
                        @Override
                        public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                            expandView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        }
                    });

                    springAnimation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
                        @Override
                        public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                        }
                    });

                    springAnimation.start();
                    opened = false;
                }
            }
        });

        frameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                expandView.setTranslationY(frameLayout.getHeight() - imageView.getHeight());
            }
        });
        imageView.setClickable(true);

        GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (expandView.getTranslationY() >= frameLayout.getHeight() - imageView.getHeight() && distanceY < 0) {
                    expandView.setTranslationY(frameLayout.getHeight() - imageView.getHeight());
                    opened = false;
                    return false;
                } else if (expandView.getTranslationY() <= 0 && distanceY > 0) {
                    expandView.setTranslationY(0);
                    opened = true;
                    return false;
                }

                if (expandView.getTranslationY() > 1) {
                    expandView.setTranslationY(expandView.getTranslationY() - distanceY);
                    opened = true;
                } else if (expandView.getTranslationY() < (frameLayout.getHeight() - imageView.getHeight())) {
                    expandView.setTranslationY(expandView.getTranslationY() - distanceY);
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                SpringForce spring = new SpringForce(0)
                        .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                        .setStiffness(SpringForce.STIFFNESS_LOW);

                if (velocityY < 0) {
                    opened = true;

                    final SpringAnimation springAnimation = new SpringAnimation(expandView, DynamicAnimation.TRANSLATION_Y);
                    springAnimation.setStartValue(expandView.getTranslationY());
                    springAnimation.setSpring(spring);
                    springAnimation.start();
                    opened = true;
                } else if (velocityY > 0) {

                    final SpringAnimation springAnimation = new SpringAnimation(expandView, DynamicAnimation.TRANSLATION_Y);
                    springAnimation.setStartValue(expandView.getTranslationY());
                    springAnimation.setSpring(spring);
                    spring.setFinalPosition(frameLayout.getHeight() - imageView.getHeight());
                    springAnimation.start();
                    opened = false;

                }
                return false;
            }
        };

        gestureDetector = new GestureDetector(this, gestureListener);

        activityFragment = StepsFragment.newInstance(0l);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content, activityFragment);
        transaction.commit();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return gestureDetector.onTouchEvent(event);
    }


    public void startFriendsActivity() {
        Intent intent = new Intent(this, FriendsActivityJava.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.navigation, menu);
        MenuItem menuItem = menu.findItem(R.id.share_menu_item);
        shareActionProvider = (ShareActionProvider)menuItem.getActionProvider();
        return true;
    }
}
