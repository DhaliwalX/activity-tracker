package com.princedhaliwal.activitytracker.utils;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.princedhaliwal.activitytracker.R;
import com.princedhaliwal.activitytracker.StepsFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.princedhaliwal.activitytracker.MainActivity.TAG;
import static java.text.DateFormat.getDateInstance;

/**
 * Read the current daily step total, computed from midnight of the current day
 * on the device's current timezone.
 */
public class VerifyDataTask extends AsyncTask<Void, Void, Void> {
    long total;
    float totalDistance;
    Float calories;
    GoogleApiClient googleApiClient;

    protected Void doInBackground(Void... params) {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleApiClient, readRequest).await(30, TimeUnit.SECONDS);

        Log.i(TAG, "" + dataReadResult.getBuckets().size());
        for (Bucket bucket : dataReadResult.getBuckets()) {
            Log.i(TAG, "BUCKET: " + bucket.toString());

            for (DataSet dataSet : bucket.getDataSets()) {
                Log.i(TAG, "DATASET: " + dataSet.toString());

                for (DataPoint dataPoint : dataSet.getDataPoints()) {
                    Log.i(TAG, "DATAPOINT: " + dataPoint.toString() + " DAtaTYpe: " + dataPoint.getDataType().toString());

                    if (dataPoint.getDataType().equals(DataType.AGGREGATE_DISTANCE_DELTA)) {
                        Log.i(TAG, "DATAVALUE: " + dataPoint.getValue(Field.FIELD_DISTANCE));
                        totalDistance = dataPoint.getValue(Field.FIELD_DISTANCE).asFloat();
                        totalDistance /= 1000;
                    } else if (dataPoint.getDataType().equals(DataType.AGGREGATE_CALORIES_EXPENDED)) {
                        Log.i(TAG, "CALORIES: " + dataPoint.getValue(Field.FIELD_CALORIES));
                        calories = dataPoint.getValue(Field.FIELD_CALORIES).asFloat();
                    } else if (dataPoint.getDataType().equals(DataType.AGGREGATE_STEP_COUNT_DELTA)) {
                        Log.i(TAG, "STEPS: " + dataPoint.getValue(Field.FIELD_STEPS));
                        total = dataPoint.getValue(Field.FIELD_STEPS).asInt();
                    }

                }
            }
        }
        Log.i(TAG, "Total steps: " + total);
        return null;
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}