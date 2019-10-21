package net.autogroup.apkextractor.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.autogroup.apkextractor.R;

public abstract class ProgressDialog extends AlertDialog {

    ProgressBar progressBar;
    TextView att;
    TextView att_left;
    TextView att_right;

    public ProgressDialog(@NonNull Context context,@NonNull String title) {
        super(context);
        View dialog_view=LayoutInflater.from(context).inflate(R.layout.dialog_with_progress,null);
        setView(dialog_view);
        progressBar=dialog_view.findViewById(R.id.dialog_progress_bar);
        att=dialog_view.findViewById(R.id.dialog_att);
        att_left=dialog_view.findViewById(R.id.dialog_att_left);
        att_right=dialog_view.findViewById(R.id.dialog_att_right);
        setTitle(title);
    }

}
