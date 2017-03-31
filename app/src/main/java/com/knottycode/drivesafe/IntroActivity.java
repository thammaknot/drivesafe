package com.knottycode.drivesafe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.gc.materialdesign.views.ButtonFlat;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

public class IntroActivity extends FragmentActivity {

    private static final String TAG = "***IntroActivity***";

    private ViewPager pager;
    private SmartTabLayout indicator;
    private ButtonFlat skip;
    private ButtonFlat next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);

        pager = (ViewPager) findViewById(R.id.pager);
        indicator = (SmartTabLayout) findViewById(R.id.indicator);
        skip = (ButtonFlat) findViewById(R.id.skip);
        next = (ButtonFlat) findViewById(R.id.next);

        next.setRippleSpeed(30);
        skip.setRippleSpeed(30);

        FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new IntroFragment1();
                    case 1:
                        return new IntroFragment2();
                    case 2:
                        return new IntroFragment3();
                    case 3:
                        return new IntroFragment4();
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };

        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(4);
        indicator.setViewPager(pager);

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pager.getCurrentItem() == 3) { // The last screen
                    finishOnboarding();
                } else {
                    pager.setCurrentItem(pager.getCurrentItem() + 1, true);
                }
            }
        });

        indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if(position == 3){
                    skip.setVisibility(View.GONE);
                    next.setText(getString(R.string.done));
                } else {
                    skip.setVisibility(View.VISIBLE);
                    next.setText(getString(R.string.next));
                }
            }
        });
    }

    private void finishOnboarding() {
        SharedPreferences preferences =
                getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE);
        preferences.edit()
             .putBoolean(getString(R.string.onboarding_complete_key), true).apply();
        // Launch the main Activity, called MainActivity.
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);

        // Close this Activity.
        finish();
    }
}