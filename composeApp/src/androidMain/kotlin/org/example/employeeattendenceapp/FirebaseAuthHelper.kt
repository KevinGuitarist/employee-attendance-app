package org.example.employeeattendenceapp.Auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

private const val PREFS_NAME = "user_prefs"
private const val KEY_USER_ROLE = "user_role"

actual fun signUpWithEmailPassword(
    email: String,
    password: String,
    role: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    FirebaseAuth.getInstance()
        .createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Immediately call onSuccess since auth is complete
                onSuccess()

                // Continue with database write in background
                val uid = task.result?.user?.uid
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                    val userData = hashMapOf(
                        "email" to email,
                        "role" to role
                    )
                    dbRef.setValue(userData).addOnFailureListener { e ->
                        Log.e("Auth", "Failed to save user data", e)
                        // Optional: log the error but don't show to user
                    }
                }
            } else {
                onError(task.exception?.localizedMessage ?: "Sign-up failed")
            }
        }
}

actual fun signInWithEmailPassword(
    email: String,
    password: String,
    expectedRole: String,
    onSuccess: () -> Unit,
    onRoleMismatch: () -> Unit,
    onError: (String) -> Unit
) {
    Log.d("Auth", "signInWithEmailPassword called") // ADDED LOG
    FirebaseAuth.getInstance()
        .signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("role")
                    Log.d("Auth", "Database reference: $dbRef") // ADDED LOG
                    dbRef.get().addOnSuccessListener { dataSnapshot ->
                        val storedRole = dataSnapshot.getValue(String::class.java)
                        Log.d("Auth", "About to print Expected role: $expectedRole, Stored role: $storedRole") // ADD THIS LINE
                        Log.d("Auth", "Expected role: $expectedRole, Stored role: $storedRole") // ADD THIS LINE
                        if (storedRole == expectedRole) {
                            onSuccess()
                        } else {
                            onRoleMismatch()
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Auth", "Failed to fetch role: ${e.message}") // ADD THIS LINE
                        onError(e.localizedMessage ?: "Failed to fetch role")
                    }
                } else {
                    val errorMessage = "Failed to get user UID"
                    Log.e("Auth", errorMessage)  // ADD THIS LINE
                    onError(errorMessage)
                }
            } else {
                val errorMessage = task.exception?.localizedMessage ?: "Login failed"
                Log.e("Auth", errorMessage)  // ADD THIS LINE
                onError(errorMessage)
            }
        }
}

actual fun isUserLoggedIn(): Boolean {
    return FirebaseAuth.getInstance().currentUser != null
}

actual fun signOut() {
    FirebaseAuth.getInstance().signOut()
}

fun getPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

actual fun saveUserRole(context: Any, role: String) {
    val ctx = context as Context
    getPrefs(ctx).edit().putString(KEY_USER_ROLE, role).apply()
}

actual fun getUserRole(context: Any): String? {
    val ctx = context as Context
    return getPrefs(ctx).getString(KEY_USER_ROLE, null)
}

actual fun clearUserRole(context: Any) {
    val ctx = context as Context
    getPrefs(ctx).edit().remove(KEY_USER_ROLE).apply()
}

// Update these functions in FirebaseAuthHelper.kt

fun updateEmployeeAttendance(
    uid: String,
    name: String,
    date: String,
    day: String,
    latitude: Double?,
    longitude: Double?,
    checkInTime: String,
    workingHours: String,
    attendance: String,
    status: String
) {
    // This will ensure no duplicates - overwrites existing data
    val attendanceRef = FirebaseDatabase.getInstance()
        .getReference("attendance")
        .child(date)
        .child(uid)

    val attendanceData = hashMapOf(
        "name" to name,
        "date" to date,
        "day" to day,
        "latitude" to latitude,
        "longitude" to longitude,
        "checkInTime" to checkInTime,
        "workingHours" to workingHours,
        "attendance" to attendance,
        "status" to status
    )

    attendanceRef.setValue(attendanceData)
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error updating attendance: ${e.message}")
        }
}

fun saveDailyRecord(
    uid: String,
    name: String,
    date: String,
    day: String,
    checkInTime: String,
    workingHours: String,
    attendance: String,
    status: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val dailyRecordRef = FirebaseDatabase.getInstance()
        .getReference("daily_records")
        .child(date)
        .child(uid)

    val recordData = hashMapOf(
        "name" to name,
        "date" to date,
        "day" to day,
        "checkInTime" to checkInTime,
        "workingHours" to workingHours,
        "attendance" to attendance,
        "status" to status
    )

    // Force write with setValue
    dailyRecordRef.setValue(recordData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(e.message ?: "Failed to save daily record")
            Log.e("Firebase", "Error saving daily record: ${e.message}")
        }
}