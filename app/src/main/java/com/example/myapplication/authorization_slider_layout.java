package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class authorization_slider_layout extends PagerAdapter {
    Context context;
    LayoutInflater layoutInflater;

    public void Authorization_Slider_Layout(Context context)
    {
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
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (RelativeLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.authorization_slider_layout,container,false);

        ImageView slideImageView = (ImageView) view.findViewById(R.id.first_ico);
        TextView slideHeading = (TextView) view.findViewById(R.id.Heading_text);
        TextView slideLowText = (TextView) view.findViewById(R.id.low_text);

        slideImageView.setImageResource(slide_images[position]);
        slideHeading.setText(slide_heading[position]);
        slideLowText.setText(slide_descs[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {

        container.removeView((RelativeLayout) object);

    }

}

