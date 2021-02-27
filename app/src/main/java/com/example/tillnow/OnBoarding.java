package com.example.tillnow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OnBoarding extends AppCompatActivity {

    //local
    private int currentPos;

    //Context
    private Context context =this;

    //TextView
    private TextView[] dots;

    //Buttons
    private Button letsGetStarted;

    //animation
    private Animation animation;

    //Viewpager
    private ViewPager viewPager;

    //linear layout
    private LinearLayout dotsLayout;

    //slider adapter
    private SliderAdapter sliderAdapter;

    //Shared prefrence
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        //hooks
        viewPager = findViewById(R.id.slider);
        dotsLayout = findViewById(R.id.dots);
        letsGetStarted = findViewById(R.id.get_started_btn);

        //setting slider adapter and view pager
        sliderAdapter = new SliderAdapter(this);
        viewPager.setAdapter(sliderAdapter);

        //settting the default dot positon
        addDots(0);

        //setting the page change listner
        viewPager.addOnPageChangeListener(changeListener);
    }

    //methode to skip the OnBoearding activity
    public void skip(View view){
        //starting activity for login
        startActivity(new Intent(getApplicationContext(),Welcome.class));

        //setting up the shared profrence
        sharedPreferences = getSharedPreferences("onBoardingScreen",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("new",false);
        editor.commit();
        finish();
    }

    //methode to show the next page
    public void next(View view){
        currentPos++;
        if(currentPos>=sliderAdapter.getCount()){
            currentPos=sliderAdapter.getCount()-1;
        }
        viewPager.setCurrentItem(currentPos);
    }

    //methode to show the provius page
    public void prev(View view){
        currentPos--;
        if(currentPos<0){
            currentPos=0;
        }
        viewPager.setCurrentItem(currentPos);
    }

    //methode to show the page  according to the current position
    private void addDots(int position){
        dots = new TextView[sliderAdapter.getCount()];
        dotsLayout.removeAllViews();

        for(int i=0;i<dots.length;i++){
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);

            dotsLayout.addView(dots[i]);
        }

        dots[position].setTextColor(getResources().getColor(R.color.colorPrimary));
    }
    ViewPager.OnPageChangeListener changeListener= new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            addDots(position);
            currentPos=position;
            if(position == sliderAdapter.getCount()-1){
                animation = AnimationUtils.loadAnimation(OnBoarding.this,R.anim.button_animation);
                letsGetStarted.setAnimation(animation);
                letsGetStarted.setVisibility(View.VISIBLE);

            }else{
                letsGetStarted.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}
