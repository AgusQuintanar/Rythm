package com.example.rythm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class HomeView extends AppCompatActivity {

    private static final String TAG_FRAGMENT = "fragment";
    private HomeFragment homeFragment;
    private LibraryFragment libraryFragment;
    private UserFragment userFragment;
    private SearchFragment searchFragment;
    private LinearLayout btnPlayLists,
            btnUser,
            btnSearch,
            btnHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_view);


        this.btnPlayLists = findViewById(R.id.btnLibrary);
        this.btnUser = findViewById(R.id.btnUser);
        this.btnSearch = findViewById(R.id.btnSearch);
        this.btnHome = findViewById(R.id.btnHome);
        this.homeFragment = new HomeFragment();
        this.libraryFragment = new LibraryFragment();
        this.userFragment = new UserFragment();
        this.searchFragment = new SearchFragment();

        this.setFragment(homeFragment);
        this.btnHome.setOnClickListener(v -> {
            this.setFragment(this.homeFragment);
        });
        this.btnPlayLists.setOnClickListener(v -> {
            this.setFragment(this.libraryFragment);
        });

        this.btnUser.setOnClickListener(v -> {
            this.setFragment(this.userFragment);
        });

        this.btnSearch.setOnClickListener(v -> {
            this.setFragment(this.searchFragment);
        });
    }

    private void setFragment(Fragment fragment){
        FragmentManager mr = getSupportFragmentManager();
        FragmentTransaction transaction = mr.beginTransaction();
        transaction.replace(R.id.container, fragment, TAG_FRAGMENT);
        transaction.commit();
    }
}