package com.beatboxers.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.beatboxers.R;

public class AboutUsDialog extends DialogFragment {
    static public final String TAG = "aboutUsDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //setup our views first
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_about_us, null);
        View titleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_about_us_title, null);

        Button doneButton = (Button)view.findViewById(R.id.buttonOk);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //set our views to the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCustomTitle(titleView)
                .setView(view);

        return builder.create();
    }
}