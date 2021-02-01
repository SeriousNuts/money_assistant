package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class AddedUsers_progress extends Fragment {
    RecyclerView recyclerView;
    FirebaseUser payment = FirebaseAuth.getInstance().getCurrentUser();
    String PaymentKey = payment.getUid();
    Query queryPayments;
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    RecycleViewAdapter firestoreRecyclerAdapter;
    String chartname;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View RootView = inflater.inflate(R.layout.fragment_added_users_progress, container, false);
        Intent intentReceived= getActivity().getIntent();
        Bundle bundleChartName=intentReceived.getExtras();
        if(bundleChartName != null){
            chartname=bundleChartName.getString("chartname");
        }
        queryPayments = firebaseFirestore.collection(PaymentKey).whereEqualTo("ParentChart",chartname);


        recyclerView = RootView.findViewById(R.id.PaymentResView);


        FirestoreRecyclerOptions<Payment> options =
                new FirestoreRecyclerOptions.Builder<Payment>()
                        .setQuery(queryPayments, Payment.class)
                        .build();
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        firestoreRecyclerAdapter = new RecycleViewAdapter(options);
        firestoreRecyclerAdapter.setOnItemListenerListener(new RecycleViewAdapter.OnItemListener() {
            @Override
            public void OnItemClickListener(View view, int position) {
                //заглушка для клика
            }

            @Override
            public void OnItemLongClickListener(View view, int position) {
                //заглушка для долгого клика
            }
        });
        recyclerView.setAdapter(firestoreRecyclerAdapter);
        return RootView;
    }
    @Override
    public void onStop() {
        super.onStop();
        firestoreRecyclerAdapter.stopListening();
    }

    @Override
    public void onStart() {
        super.onStart();
        firestoreRecyclerAdapter.startListening();
    }
}
