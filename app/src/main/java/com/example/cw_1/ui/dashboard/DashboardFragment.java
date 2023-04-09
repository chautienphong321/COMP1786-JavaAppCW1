package com.example.cw_1.ui.dashboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.cw_1.ItemDetailActivity;
import com.example.cw_1.MainActivity;
import com.example.cw_1.R;
import com.example.cw_1.models.Activity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    final List<String> listTripId = new ArrayList<>();
    FirebaseFirestore firebase = FirebaseFirestore.getInstance();

    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        LayoutInflater lf = getActivity().getLayoutInflater();
        View view =  lf.inflate(R.layout.fragment_dashboard, container, false);

        TextView checkExistActivity = view.findViewById(R.id.checkExistActivity);
        TextView monthPickerTitle = view.findViewById(R.id.monthPickerTitle);
        String timeStamp = format.format(Calendar.getInstance().getTime());
        Spinner spinner = view.findViewById(R.id.spinnerTripActivity);
        monthPickerTitle.setText(timeStamp);

        //Set data activities
        ArrayList<Activity> arrayOfActivities = Activity.getActivities();
        ActivitiesAdapter adapter = new ActivitiesAdapter(getActivity().getBaseContext(), arrayOfActivities);
        ListView listView = (ListView)view.findViewById(R.id.listActivity);
        listView.setAdapter(adapter);
        if(arrayOfActivities.isEmpty())
        {
            checkExistActivity.setVisibility(View.GONE);
        }


        // Item in listview clicked
        listView.setOnItemClickListener((adapterView, view1, i, l) -> {
            Intent intent = new Intent(getActivity(), ItemDetailActivity.class);
            intent.putExtra("tripId", listTripId.get(i));
            intent.putExtra("id", arrayOfActivities.get(i).getId());
            intent.putExtra("category", arrayOfActivities.get(i).getCategory());
            intent.putExtra("money", arrayOfActivities.get(i).getMoney());
            intent.putExtra("date", arrayOfActivities.get(i).getIssueDate());
            intent.putExtra("note", arrayOfActivities.get(i).getNote());
            startActivity(intent);
        });

        // Btn backup

        Button btnBackup = view.findViewById(R.id.btnBackup);
        btnBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Connection connection = MainActivity.connectionClass();
                    ResultSet resultSet = null;
                    try{
                        if(connection != null){
                            String sqlScript = "SELECT * FROM Trip";
                            Statement st = connection.createStatement();
                            resultSet = st.executeQuery(sqlScript);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    if(resultSet == null){
                         Toast.makeText(getActivity(), "Data backed up successfully", Toast.LENGTH_SHORT).show();
                         return;
                    } else {

                    // Convert the retrieved data into JSON format
                    JSONArray jsonArray = new JSONArray();
                    while (resultSet.next()) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("TripId", resultSet.getString("TripId"));
                        jsonObject.put("TripName", resultSet.getString("TripName"));
                        jsonObject.put("Destination", resultSet.getString("Destination"));
                        jsonObject.put("TripDate", resultSet.getString("TripDate"));
                        jsonObject.put("RiskAssessment", resultSet.getString("RiskAssessment"));
                        jsonObject.put("Description", resultSet.getString("Description"));
                        jsonArray.put(jsonObject);
                    }
                    String jsonData = jsonArray.toString();
                    List<Map<String, Object>> dataList = new Gson().fromJson(jsonData, new TypeToken<List<Map<String, Object>>>(){}.getType());

                    // Push the converted data to Firebase
                    for (Map<String, Object> data : dataList) {
                        firebase.collection("trips").add(data); // Push the converted data to Firebase
                    }
                    Toast.makeText(getActivity(), "Data backed up successfully", Toast.LENGTH_SHORT).show();
                    }

                } catch (SQLException | JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Backup failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Btn delete trip
        Button btnDeleteTrip = view.findViewById(R.id.btnDeleteTrip);
        btnDeleteTrip.setOnClickListener(v->{
            String tripId = listTripId.get(spinner.getSelectedItemPosition());
            Connection connection = MainActivity.connectionClass();

            try{
                if(connection != null){
                    String sqlScript = "DELETE FROM Trip Where TripId = '" + tripId + "'";
                    Statement st = connection.createStatement();
                    st.executeUpdate(sqlScript);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            firebase.collection("Trip").document(tripId)
                .delete()
                .addOnSuccessListener(unused -> Toast.makeText(getActivity(), "Delete trip success", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Delete failure !", Toast.LENGTH_SHORT).show());
            //checkExistActivity(arrayOfActivities);
            Navigation.findNavController(view).navigate(R.id.navigation_dashboard);
        });

        // Btn delete activities
        Button btnDeleteActivities = view.findViewById(R.id.btnDeleteActivities);
        btnDeleteActivities.setOnClickListener(v->{
            if(arrayOfActivities.isEmpty()){
                Toast.makeText(getActivity(), "There is no activity to delete !!!", Toast.LENGTH_SHORT).show();
            } else {
                String tripId = listTripId.get(spinner.getSelectedItemPosition());
                Connection connection = MainActivity.connectionClass();

                try{
                    if(connection != null){
                        String sqlScript2 = "DELETE FROM Activity Where TripId = '" + tripId + "'";
                        Statement st2 = connection.createStatement();
                        st2.executeUpdate(sqlScript2);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                DocumentReference docRef = firebase.collection("Trip").document(tripId);
                docRef.collection("Activity").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            firebase.collection("Trip").document(tripId).collection("Activity").document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(unused -> Toast.makeText(getActivity(), "Delete all activities success", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Delete failure !", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
                arrayOfActivities.clear();
                checkExistActivity(arrayOfActivities);
            }
        });


        //Btn prev, next date clicked
        Button btnPrev = view.findViewById(R.id.btnPrevDay);
        Button btnNext = view.findViewById(R.id.btnNextDay);
        btnPrev.setOnClickListener(v -> {
            arrayOfActivities.clear();

            // Decrease date text
            Date dt = null;
            try {
                dt = format.parse(monthPickerTitle.getText().toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Calendar cal = Calendar.getInstance();
                    assert dt != null;
                    cal.setTime(dt);
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                    monthPickerTitle.setText(format.format(cal.getTime()));

            String tripId = listTripId.get(spinner.getSelectedItemPosition());

            DocumentReference docRef = firebase.collection("Trip").document(tripId);
            docRef.collection("Activity").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    arrayOfActivities.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        int money = document.getLong("money").intValue();

                        Calendar cal1 = Calendar.getInstance();
                        cal1.setTime(document.getDate("issueDate"));
                        Boolean isSameDay = cal1.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
                                cal1.get(Calendar.YEAR) == cal.get(Calendar.YEAR);

                        if(isSameDay){
                            arrayOfActivities.add(new Activity(document.getId(),document.getString("category"), money, document.getDate("issueDate"),document.getString("note")));
                            listTripId.add(document.getId());
                        }
                    }

                }
                listView.setAdapter(adapter);
                checkExistActivity(arrayOfActivities);
            });
        });
        btnNext.setOnClickListener(v -> {
            arrayOfActivities.clear();

            // Decrease date text
            Date dt = null;
            try {
                dt = format.parse(monthPickerTitle.getText().toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Calendar cal = Calendar.getInstance();
            assert dt != null;
            cal.setTime(dt);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            monthPickerTitle.setText(format.format(cal.getTime()));

            String tripId = listTripId.get(spinner.getSelectedItemPosition());

            DocumentReference docRef = firebase.collection("Trip").document(tripId);
            docRef.collection("Activity").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    arrayOfActivities.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        int money = document.getLong("money").intValue();

                        Calendar cal1 = Calendar.getInstance();
                        cal1.setTime(document.getDate("issueDate"));
                        Boolean isSameDay = cal1.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
                                cal1.get(Calendar.YEAR) == cal.get(Calendar.YEAR);

                        if (isSameDay) {
                            arrayOfActivities.add(new Activity(document.getId(), document.getString("category"), money, document.getDate("issueDate"), document.getString("note")));
                            listTripId.add(document.getId());
                        }
                    }

                }
                listView.setAdapter(adapter);
                checkExistActivity(arrayOfActivities);
            });
        });

        // Set data of trip
        FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
        CollectionReference tripsRef = rootRef.collection("Trip");
        List<String> trips = new ArrayList<>();
        ArrayAdapter<String> tripAdapter = new ArrayAdapter<>(getActivity().getBaseContext(), android.R.layout.simple_spinner_item, trips);
        tripAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(tripAdapter);
        tripsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String trip = document.getString("tripName") + " in " + document.getString("destination");

                    trips.add(trip);
                    listTripId.add(document.getId());
                }
                tripAdapter.notifyDataSetChanged();
            }
        });

        // Event on choosing trip from spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tripId = listTripId.get(spinner.getSelectedItemPosition());
                DocumentReference tripRef = firebase.collection("Trip").document(tripId);
                tripRef.get().addOnCompleteListener( task -> {
                    if (task.isSuccessful()) {
                        arrayOfActivities.clear();
                        DocumentSnapshot document = task.getResult();

                        Calendar cal = Calendar.getInstance();
                        cal.setTime(new Date());
                        Calendar calTrip = Calendar.getInstance();
                        calTrip.setTime(document.getDate("tripDate"));
                        Boolean isTodayTrip = cal.get(Calendar.DAY_OF_YEAR) == calTrip.get(Calendar.DAY_OF_YEAR) &&
                                cal.get(Calendar.YEAR) == calTrip.get(Calendar.YEAR);

                        if(isTodayTrip){
                            DocumentReference activityRef = firebase.collection("Trip").document(tripId);
                            activityRef.collection("Activity").get().addOnCompleteListener(tasks -> {
                                if (task.isSuccessful()) {
                                    arrayOfActivities.clear();
                                    for (QueryDocumentSnapshot documents : tasks.getResult()) {
                                        int money = documents.getLong("money").intValue();

                                        Calendar cal1 = Calendar.getInstance();
                                        Calendar cal2 = Calendar.getInstance();
                                        cal1.setTime(documents.getDate("issueDate"));
                                        cal2.setTime(new Date());
                                        Boolean isToday = cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                                                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);

                                        if(isToday){
                                            arrayOfActivities.add(new Activity(documents.getId(),documents.getString("category"), money, documents.getDate("issueDate"),documents.getString("note")));
                                            listTripId.add(document.getId());
                                        }
                                    }
                                    tripAdapter.notifyDataSetChanged();
                                }
                                listView.setAdapter(adapter);
                                checkExistActivity(arrayOfActivities);
                            });
                        }
                        else {
                            monthPickerTitle.setText(format.format(calTrip.getTime()));
                            arrayOfActivities.clear();

                            DocumentReference docRef = firebase.collection("Trip").document(tripId);
                            docRef.collection("Activity").get().addOnCompleteListener(tasks -> {
                                if (tasks.isSuccessful()) {
                                    arrayOfActivities.clear();
                                    for (QueryDocumentSnapshot documents : tasks.getResult()) {
                                        int money = documents.getLong("money").intValue();

                                        Calendar cal2 = Calendar.getInstance();
                                        cal2.setTime(documents.getDate("issueDate"));
                                        Boolean isToday = cal2.get(Calendar.DAY_OF_YEAR) == calTrip.get(Calendar.DAY_OF_YEAR) &&
                                                cal2.get(Calendar.YEAR) == calTrip.get(Calendar.YEAR);

                                        if(isToday){
                                            arrayOfActivities.add(new Activity(documents.getId(),documents.getString("category"), money, documents.getDate("issueDate"),documents.getString("note")));
                                            listTripId.add(document.getId());
                                        }
                                    }

                                }
                                listView.setAdapter(adapter);
                                checkExistActivity(arrayOfActivities);
                            });
                        };
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void checkExistActivity(ArrayList arr){
        TextView check = getView().findViewById(R.id.checkExistActivity);
        if(arr.isEmpty())
        {
            check.setVisibility(View.VISIBLE);
        } else {
            check.setVisibility(View.GONE);
        }
    }
}