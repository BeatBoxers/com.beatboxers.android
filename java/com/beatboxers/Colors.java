package com.beatboxers;

import android.content.Context;

public class Colors {
    //just a shortcut for this chain of method calls
    static public int get(Context context, int resource) {
        return context.getResources().getColor(resource);
    }
}