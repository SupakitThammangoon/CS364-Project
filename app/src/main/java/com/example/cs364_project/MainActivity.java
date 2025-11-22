package com.example.cs364_project;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements HomeFragment.HomeFragmentListener {

    private BottomNavigationView bottomNav;
    private int lastTotalMl = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // โหลดข้อมูลจาก SharedPreferences -> WaterTracker
        WaterTracker.init(getApplicationContext());

        // ดึง target เดิมมาเก็บใน lastTotalMl
        lastTotalMl = WaterTracker.getTargetMl();

        bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selected = null;

            if (id == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (id == R.id.nav_goal) {
                selected = createGoalFragmentWithTotal(lastTotalMl);
            } else if (id == R.id.nav_history) {
                selected = new HistoryFragment();
            }

            if (selected != null) {
                loadFragment(selected);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private GoalFragment createGoalFragmentWithTotal(int totalMl) {
        GoalFragment fragment = new GoalFragment();
        Bundle args = new Bundle();
        args.putInt("TOTAL_ML", totalMl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onTotalCalculated(int totalMl) {
        // callback จาก HomeFragment เวลา user กดคำนวณสำเร็จ
        lastTotalMl = totalMl;
        WaterTracker.setTargetMl(totalMl);

        GoalFragment goalFragment = createGoalFragmentWithTotal(totalMl);
        loadFragment(goalFragment);
        bottomNav.setSelectedItemId(R.id.nav_goal);
    }
}
