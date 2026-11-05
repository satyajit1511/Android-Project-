package com.example.myapplication.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.myapplication.model.AttendanceRecord;
import com.example.myapplication.model.StudentAttendanceSummary;
import com.example.myapplication.model.Subject;
import com.example.myapplication.model.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AttendanceSystem.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_ROLE = "role"; // TEACHER or STUDENT

    // Subjects table
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String COLUMN_SUBJECT_ID = "id";
    public static final String COLUMN_SUBJECT_NAME = "subject_name";
    public static final String COLUMN_SUBJECT_CODE = "subject_code";

    // Attendance table
    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String COLUMN_ATTENDANCE_ID = "id";
    public static final String COLUMN_STUDENT_ROLL = "student_roll";
    public static final String COLUMN_ATTENDANCE_SUBJECT_ID = "subject_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_STATUS = "status"; // PRESENT or ABSENT

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_ROLE + " TEXT" + ")";

        String CREATE_SUBJECTS_TABLE = "CREATE TABLE " + TABLE_SUBJECTS + "("
                + COLUMN_SUBJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SUBJECT_NAME + " TEXT,"
                + COLUMN_SUBJECT_CODE + " TEXT" + ")";

        String CREATE_ATTENDANCE_TABLE = "CREATE TABLE " + TABLE_ATTENDANCE + "("
                + COLUMN_ATTENDANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_STUDENT_ROLL + " TEXT,"
                + COLUMN_ATTENDANCE_SUBJECT_ID + " INTEGER,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_STATUS + " TEXT,"
                + "UNIQUE(" + COLUMN_STUDENT_ROLL + ", " + COLUMN_ATTENDANCE_SUBJECT_ID + ", " + COLUMN_DATE + ")" + ")";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_SUBJECTS_TABLE);
        db.execSQL(CREATE_ATTENDANCE_TABLE);

        // Pre-insert default Teacher account
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, "teacher");
        values.put(COLUMN_PASSWORD, "password123");
        values.put(COLUMN_NAME, "Head Teacher");
        values.put(COLUMN_ROLE, "TEACHER");
        db.insert(TABLE_USERS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        onCreate(db);
    }

    // Authenticate User
    public User authenticateUser(String username, String password, String role) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USER_ID, COLUMN_USERNAME, COLUMN_PASSWORD, COLUMN_NAME, COLUMN_ROLE},
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=? AND " + COLUMN_ROLE + "=?",
                new String[]{username, password, role}, null, null, null);

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new User(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE))
            );
            cursor.close();
        }
        return user;
    }

    // Add Student
    public boolean addStudent(String rollNumber, String name, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, rollNumber);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_ROLE, "STUDENT");

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Add / Register Teacher
    public boolean addTeacher(String username, String name, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_ROLE, "TEACHER");

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Get All Students
    public List<User> getAllStudents() {
        List<User> students = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_ROLE + "=?", new String[]{"STUDENT"},
                null, null, COLUMN_USERNAME + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                User student = new User(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE))
                );
                students.add(student);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return students;
    }

    // Delete Student and associated attendance records
    public boolean deleteStudent(String rollNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ATTENDANCE, COLUMN_STUDENT_ROLL + "=?", new String[]{rollNumber});
        int rows = db.delete(TABLE_USERS, COLUMN_USERNAME + "=? AND " + COLUMN_ROLE + "=?", new String[]{rollNumber, "STUDENT"});
        return rows > 0;
    }

    // Add Subject
    public boolean addSubject(String subjectName, String subjectCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SUBJECT_NAME, subjectName);
        values.put(COLUMN_SUBJECT_CODE, subjectCode);

        long result = db.insert(TABLE_SUBJECTS, null, values);
        return result != -1;
    }

    // Get All Subjects
    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SUBJECTS, null, null, null, null, null, COLUMN_SUBJECT_NAME + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Subject subject = new Subject(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT_CODE))
                );
                subjects.add(subject);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return subjects;
    }

    // Mark Attendance (Insert or Replace)
    public boolean markAttendance(String studentRoll, int subjectId, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STUDENT_ROLL, studentRoll);
        values.put(COLUMN_ATTENDANCE_SUBJECT_ID, subjectId);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_STATUS, status);

        long result = db.insertWithOnConflict(TABLE_ATTENDANCE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    // Check if attendance has been marked for subject and date
    public boolean hasAttendanceRecord(int subjectId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[]{COLUMN_ATTENDANCE_ID},
                COLUMN_ATTENDANCE_SUBJECT_ID + "=? AND " + COLUMN_DATE + "=?",
                new String[]{String.valueOf(subjectId), date},
                null, null, null);

        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return exists;
    }

    // Get Attendance Status for specific student, subject, and date (returns null if not marked yet)
    public String getAttendanceStatus(String studentRoll, int subjectId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[]{COLUMN_STATUS},
                COLUMN_STUDENT_ROLL + "=? AND " + COLUMN_ATTENDANCE_SUBJECT_ID + "=? AND " + COLUMN_DATE + "=?",
                new String[]{studentRoll, String.valueOf(subjectId), date},
                null, null, null);

        String status = null;
        if (cursor != null && cursor.moveToFirst()) {
            status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
            cursor.close();
        }
        return status;
    }

    // Get Student Attendance Summary for all subjects
    public List<StudentAttendanceSummary> getStudentAttendanceSummaries(String studentRoll) {
        List<StudentAttendanceSummary> summaries = new ArrayList<>();
        List<Subject> subjects = getAllSubjects();
        SQLiteDatabase db = this.getReadableDatabase();

        for (Subject subject : subjects) {
            // Count total classes conducted for this subject
            String totalQuery = "SELECT COUNT(DISTINCT " + COLUMN_DATE + ") FROM " + TABLE_ATTENDANCE
                    + " WHERE " + COLUMN_ATTENDANCE_SUBJECT_ID + "=?";
            Cursor totalCursor = db.rawQuery(totalQuery, new String[]{String.valueOf(subject.getId())});
            int totalClasses = 0;
            if (totalCursor != null && totalCursor.moveToFirst()) {
                totalClasses = totalCursor.getInt(0);
                totalCursor.close();
            }

            // Count classes attended by this student for this subject
            String attendedQuery = "SELECT COUNT(*) FROM " + TABLE_ATTENDANCE
                    + " WHERE " + COLUMN_STUDENT_ROLL + "=? AND " + COLUMN_ATTENDANCE_SUBJECT_ID + "=? AND " + COLUMN_STATUS + "='PRESENT'";
            Cursor attendedCursor = db.rawQuery(attendedQuery, new String[]{studentRoll, String.valueOf(subject.getId())});
            int attendedClasses = 0;
            if (attendedCursor != null && attendedCursor.moveToFirst()) {
                attendedClasses = attendedCursor.getInt(0);
                attendedCursor.close();
            }

            double percentage = totalClasses > 0 ? ((double) attendedClasses / totalClasses) * 100.0 : 0.0;
            summaries.add(new StudentAttendanceSummary(subject.getSubjectName(), subject.getSubjectCode(), totalClasses, attendedClasses, percentage));
        }

        return summaries;
    }

    // Get Overall Attendance Percentage for a student
    public double getStudentOverallPercentage(String studentRoll) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Total classes recorded for this student
        String totalQuery = "SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE " + COLUMN_STUDENT_ROLL + "=?";
        Cursor totalCursor = db.rawQuery(totalQuery, new String[]{studentRoll});
        int total = 0;
        if (totalCursor != null && totalCursor.moveToFirst()) {
            total = totalCursor.getInt(0);
            totalCursor.close();
        }

        if (total == 0) return 0.0;

        // Total present classes for this student
        String presentQuery = "SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE " + COLUMN_STUDENT_ROLL + "=? AND " + COLUMN_STATUS + "='PRESENT'";
        Cursor presentCursor = db.rawQuery(presentQuery, new String[]{studentRoll});
        int present = 0;
        if (presentCursor != null && presentCursor.moveToFirst()) {
            present = presentCursor.getInt(0);
            presentCursor.close();
        }

        return ((double) present / total) * 100.0;
    }
}

