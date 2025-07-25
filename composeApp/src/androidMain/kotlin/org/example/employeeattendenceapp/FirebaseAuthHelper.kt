package org.example.employeeattendenceapp.Auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "user_prefs"
private const val KEY_USER_ROLE = "user_role"

actual fun signUpWithEmailPassword(
    email: String,
    password: String,
    role: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
){
    FirebaseAuth.getInstance()
        .createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                    dbRef.child("role").setValue(role)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to save role") }
                } else {
                    onError("Failed to get user UID")
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
    FirebaseAuth.getInstance()
        .signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("role")
                    dbRef.get().addOnSuccessListener { dataSnapshot ->
                        val storedRole = dataSnapshot.getValue(String::class.java)
                        if (storedRole == expectedRole) {
                            onSuccess()
                        } else {
                            onRoleMismatch()
                        }
                    }.addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Failed to fetch role")
                    }
                } else {
                    onError("Failed to get user UID")
                }
            } else {
                onError(task.exception?.localizedMessage ?: "Login failed")
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
    val dbRef = FirebaseDatabase.getInstance().getReference("attendance").child(uid)
    val attendanceData = mapOf(
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
    dbRef.setValue(attendanceData)
}
