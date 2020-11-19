package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

public class authorization_slider_layout extends PagerAdapter{
    Context context;
    LayoutInflater layoutInflater;

    public authorization_slider_layout(Context context) {
        this.context = context;
    }

    //Arrays
    public int[] slide_images = {
            R.drawable.authorization_first_ico,
            R.drawable.authorization_second_ico,
            R.drawable.authorization_third_ico,
    };
    public  String[] slide_heading = {
            "FirstIco",
            "SecondICo",
            "ThirdICo",
    };
    public String[] slide_descs = {
            "A lot of text to authorization menu. This text is testible"  + " first slide",
            "A lot of text to authorization menu. This text is testible" + "second slide",
            "A lot of text to authorization menu. This text is testible" + " third ico",
    };
    @Override
    public int getCount() {
        return slide_heading.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.authorization_slider_layout,container,false);

        ImageView slideImageView = view.findViewById(R.id.first_ico);
        TextView slideHeading = view.findViewById(R.id.Heading_text);
        TextView slideLowText = view.findViewById(R.id.low_text);

        slideImageView.setImageResource(slide_images[position]);
        slideHeading.setText(slide_heading[position]);
        slideLowText.setText(slide_descs[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position,Object object) {

        container.removeView((ConstraintLayout) object);

    }

}

