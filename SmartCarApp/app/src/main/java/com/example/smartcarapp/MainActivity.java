package com.example.smartcarapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartcarapp.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    static boolean darkmode = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new HomeFragment());

        binding.bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    if(!(getVisibleFragment() instanceof HomeFragment)){
                        replaceFragment(new HomeFragment());
                    }
                    break;
                case R.id.profile:
                    if(!(getVisibleFragment() instanceof ProfileFragment)) {
                    replaceFragment(new ProfileFragment());
                } break;
                case R.id.settings:

                    replaceFragment(new SettingsFragment());
                    break;
            }
            return true;
        });
    }
    public void replaceFragment(Fragment fragment){
        if (!darkmode) {} else {fragment = new SettingsFragment();}
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frame_layout, fragment);
            fragmentTransaction.commit();
            darkmode = false;
        }

    //This is to check which fragment is the current one(created by Ergi)
    public Fragment getVisibleFragment() {
        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible())
                return fragment;
        }
        return null;
    }
}