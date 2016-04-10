package com.beatboxers.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.beatboxers.R;
import com.beatboxers.groups.Group;

import java.util.List;

public class GroupsListAdapter extends ArrayAdapter<Group> {


    public GroupsListAdapter(Context context, int textViewResourceId,
                             List<Group> values) {
        super(context, textViewResourceId, values);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (null == view) {
            LayoutInflater inflator = LayoutInflater.from(getContext());
            view = inflator.inflate(R.layout.cell_group, parent, false);
        }

        TextView label = (TextView) view.findViewById(R.id.groupLabel);
        label.setText(this.getItem(position).name);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setSingleLine(true);
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (null == view) {
            LayoutInflater inflator = LayoutInflater.from(getContext());
            view = inflator.inflate(R.layout.cell_group, parent, false);
        }

        TextView label = (TextView) view.findViewById(R.id.groupLabel);
        label.setText(this.getItem(position).name);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setSingleLine(true);
        return label;
    }
}
