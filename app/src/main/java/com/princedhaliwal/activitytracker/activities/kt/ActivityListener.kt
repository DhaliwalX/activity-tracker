package com.princedhaliwal.activitytracker.activities.kt

import com.google.android.gms.common.api.Status
import com.google.android.gms.fitness.data.Value

data class ActivityListener (var onChange: (value: Value) -> Unit = { value ->  }, var onError: (status: Status) -> Unit = { status -> })
