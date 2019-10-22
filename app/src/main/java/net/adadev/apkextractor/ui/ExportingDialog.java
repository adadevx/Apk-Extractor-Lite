package net.adadev.apkextractor.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.format.Formatter;

import net.adadev.apkextractor.AppItem;
import net.adadev.apkextractor.R;

public class ExportingDialog extends ProgressDialog {

    public ExportingDialog(@NonNull Context context){
        super(context,context.getResources().getString(R.string.dialog_export_title));
    }

    public void setProgressOfApp(int current, int total, @NonNull AppItem item, @NonNull String write_path){
        setTitle(getContext().getResources().getString(R.string.dialog_export_title)+"("+current+"/"+total+")"+":"+item.getAppName());
        setIcon(item.getIcon(getContext()));
        att.setText(getContext().getResources().getString(R.string.dialog_export_msg_apk)+write_path);
    }

    public void setProgressOfWriteBytes(Long current,Long total){
        if(current<0)return;
        if(current>total)return;
        progressBar.setMax((int)(total/1024));
        progressBar.setProgress((int)(current/1024));
        int percent=(int)((double)current/total*100);
        att_right.setText(String.format("%s/%s(%d%%)", Formatter.formatFileSize(getContext(), current), Formatter.formatFileSize(getContext(), total), percent));
    }

    public void setSpeed(long bytes){
        att_left.setText(Formatter.formatFileSize(getContext(),bytes)+"/s");
    }

    public void setProgressOfCurrentZipFile(@NonNull String write_path){
        att.setText(getContext().getResources().getString(R.string.dialog_export_zip)+write_path);
    }
}
