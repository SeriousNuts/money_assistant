package com.example.myapplication.ViewPager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.myapplication.ContactsList;
import com.example.myapplication.R;
import com.example.myapplication.StartMenu;


public class StartFragmentA extends Fragment {
 TextView createEventBnt;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View RootView = inflater.inflate(R.layout.fragment_start_a, container, false);;
        createEventBnt = RootView.findViewById(R.id.createEvent);
        createEventBnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permissionStatus = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS);
                if(permissionStatus == PackageManager.PERMISSION_GRANTED) {
                    Intent contactIntent = new Intent(getActivity(), ContactsList.class);
                    startActivity(contactIntent);
                }
                else {
                    ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS},
                            1);
                }
            }
        });
        return RootView;
    }
}