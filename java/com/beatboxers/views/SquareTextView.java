package com.beatboxers.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class SquareTextView extends TextView {
    public SquareTextView(Context context) {
        super(context);
    }

    public SquareTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    //we want our height to match the width, the width is dynamic for the screen size so we must
    //update the height to be the measured width to make a perfect square
    public void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        //int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        //width, height
        //setMeasuredDimension(width, width);
        setMeasuredDimension(height, height);
    }
}