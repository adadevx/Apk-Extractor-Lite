package net.adadev.apkextractor.ui;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import net.adadev.apkextractor.AppItem;
import net.adadev.apkextractor.Global;
import net.adadev.apkextractor.R;
import net.adadev.apkextractor.utils.FileUtil;
import net.adadev.apkextractor.utils.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataObbDialog extends AlertDialog implements View.OnClickListener {

    private final View view;
    private DialogDataObbConfirmedCallback callback;
    private final List<AppItem>list;
    private final List<AppItem> list_data_controllable =new ArrayList<>();
    private final List<AppItem> list_obb_controllable=new ArrayList<>();
    private CheckBox cb_data;
    private CheckBox cb_obb;
    /**
     * @param export_list 传递进来的AppItem务必为使用wrapper构造的副本，初始Data和Obb导出值为false
     */
    public DataObbDialog(@NonNull Context context, @NonNull List<AppItem>export_list, final DialogDataObbConfirmedCallback callback) {
        super(context);
        this.list=export_list;
        this.callback=callback;
        view=LayoutInflater.from(context).inflate(R.layout.dialog_data_obb,null);
        cb_data=view.findViewById(R.id.dialog_checkbox_data);
        cb_obb=view.findViewById(R.id.dialog_checkbox_obb);
        setView(view);
        setTitle(context.getResources().getString(R.string.dialog_data_obb_title));

        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        setButton(AlertDialog.BUTTON_NEGATIVE,context.getResources().getString(R.string.dialog_button_cancel),new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });


    }

    @Override
    public void show(){
        super.show();
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DataObbDialog.this){
                    long data=0,obb=0;
                    for(AppItem item:list){
                        long data_item= FileUtil.getFileOrFolderSize(new File(Storage.getMainExternalStoragePath()+"/android/data/"+item.getPackageName()));
                        long obb_item=FileUtil.getFileOrFolderSize(new File(Storage.getMainExternalStoragePath()+"/android/obb/"+item.getPackageName()));
                        data+=data_item;
                        obb+=obb_item;
                        if(data>0) list_data_controllable.add(item);
                        if(obb>0) list_obb_controllable.add(item);
                    }
                    final long data_total=data;
                    final long obb_total=obb;
                    Global.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(data_total==0&&obb_total==0){
                                cancel();
                                if(callback!=null)callback.onDialogDataObbConfirmed(list);
                                return;
                            }
                            view.findViewById(R.id.dialog_data_obb_wait_area).setVisibility(View.GONE);
                            view.findViewById(R.id.dialog_data_obb_show_area).setVisibility(View.VISIBLE);
                            cb_data.setEnabled(data_total>0);
                            cb_obb.setEnabled(obb_total>0);
                            cb_data.setText("Data("+ Formatter.formatFileSize(getContext(),data_total)+")");
                            cb_obb.setText("Obb("+Formatter.formatFileSize(getContext(),obb_total)+")");
                            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(DataObbDialog.this);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        if(v.equals(getButton(AlertDialog.BUTTON_POSITIVE))){
            if(cb_data.isChecked()) for(AppItem item:list_data_controllable) item.exportData=true;
            if(cb_obb.isChecked()) for (AppItem item:list_obb_controllable) item.exportObb=true;
            if(callback!=null)callback.onDialogDataObbConfirmed(list);
            cancel();
        }
    }

    public interface DialogDataObbConfirmedCallback{
        void onDialogDataObbConfirmed(@NonNull List<AppItem>export_list);
    }
}
