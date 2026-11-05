package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.StudentAdapter;
import com.example.myapplication.db.DatabaseHelper;
import com.example.myapplication.model.User;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class AddStudentActivity extends AppCompatActivity {

    private TextInputEditText etStudentName, etStudentRoll, etStudentPassword;
    private Button btnAddStudent, btnImportCsv;
    private RecyclerView rvStudents;
    private DatabaseHelper dbHelper;
    private StudentAdapter adapter;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importStudentsFromCsv(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        dbHelper = new DatabaseHelper(this);

        etStudentName = findViewById(R.id.etStudentName);
        etStudentRoll = findViewById(R.id.etStudentRoll);
        etStudentPassword = findViewById(R.id.etStudentPassword);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        btnImportCsv = findViewById(R.id.btnImportCsv);
        rvStudents = findViewById(R.id.rvStudents);

        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        loadStudents();

        btnAddStudent.setOnClickListener(v -> {
            String name = etStudentName.getText() != null ? etStudentName.getText().toString().trim() : "";
            String roll = etStudentRoll.getText() != null ? etStudentRoll.getText().toString().trim() : "";
            String password = etStudentPassword.getText() != null ? etStudentPassword.getText().toString().trim() : "";

            if (name.isEmpty() || roll.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = dbHelper.addStudent(roll, name, password);
            if (success) {
                Toast.makeText(this, "Student registered successfully!", Toast.LENGTH_SHORT).show();
                etStudentName.setText("");
                etStudentRoll.setText("");
                etStudentPassword.setText("");
                loadStudents();
            } else {
                Toast.makeText(this, "Failed to register student (Roll Number might already exist)", Toast.LENGTH_LONG).show();
            }
        });

        btnImportCsv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimetypes = {"text/csv", "text/comma-separated-values", "application/csv", "application/excel", "application/vnd.ms-excel", "application/vnd.msexcel"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            filePickerLauncher.launch(intent);
        });
    }

    private void importStudentsFromCsv(Uri uri) {
        int successCount = 0;
        int duplicateCount = 0;

        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    // Skip header if it looks like a column description
                    if (line.toLowerCase().contains("roll") || line.toLowerCase().contains("name")) continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 3) {
                    String roll = tokens[0].trim();
                    String name = tokens[1].trim();
                    String password = tokens[2].trim();

                    if (!roll.isEmpty() && !name.isEmpty() && !password.isEmpty()) {
                        boolean success = dbHelper.addStudent(roll, name, password);
                        if (success) {
                            successCount++;
                        } else {
                            duplicateCount++;
                        }
                    }
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Import Complete")
                    .setMessage("Successfully added: " + successCount + " students.\nFailed (Duplicates or Errors): " + duplicateCount)
                    .setPositiveButton("OK", null)
                    .show();

            loadStudents();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading file. Ensure it is a valid CSV.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadStudents() {
        List<User> students = dbHelper.getAllStudents();
        adapter = new StudentAdapter(students, this::confirmDeleteStudent);
        rvStudents.setAdapter(adapter);
    }

    private void confirmDeleteStudent(User student) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Student")
                .setMessage("Are you sure you want to remove " + student.getName() + " (Roll No: " + student.getUsername() + ") from the system?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteStudent(student.getUsername());
                    if (deleted) {
                        Toast.makeText(this, "Student removed successfully", Toast.LENGTH_SHORT).show();
                        loadStudents();
                    } else {
                        Toast.makeText(this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
