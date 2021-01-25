package com.example.myapplication;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
//класс для отступов между элементами кард вью
public class Cardview_item_decor {
    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration
    {
        private int space;

        public SpacesItemDecoration(int space)
        {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
        {
            //добавить переданное кол-во пикселей отступа снизу
            outRect.bottom = space;
        }
    }
}
