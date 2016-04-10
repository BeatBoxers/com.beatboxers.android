package com.beatboxers.groups;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;

import com.beatboxers.R;

import java.util.ArrayList;
import java.util.List;

public class Groups {
    static private Groups mSharedInstance = null;
    private List<Group> mGroups = new ArrayList<>();

    private Groups(Context context) {
        Resources resources = context.getResources();
        populateGroups(resources);
    }

    private void populateGroups(Resources resources) {
        TypedArray groupsArray = resources.obtainTypedArray(R.array.instrumentsGroups);
        for (int i = 0; i < groupsArray.length(); i++) {
            int resourceId = groupsArray.getResourceId(i, 0);
            if (resourceId > 0) {
                TypedArray groupProperties = resources.obtainTypedArray(resourceId);
                List<Integer> instrumentsIds = new ArrayList<>(groupProperties.length() - 1);
                for (int j = 1; j < groupProperties.length(); j++) {
                    instrumentsIds.add(groupProperties.getInteger(j, -1));
                }
                Group group = new Group(groupProperties.getString(0), instrumentsIds);
                mGroups.add(group);
                groupProperties.recycle();
            }
        }
        groupsArray.recycle();
    }

    static public Groups init(Context context) {
        if (mSharedInstance == null) {
            mSharedInstance = new Groups(context);
        }
        return mSharedInstance;
    }

    static public Groups sharedInstance() {
        return mSharedInstance;
    }

    public List<Group> getGroups() {
        return mGroups;
    }
}
