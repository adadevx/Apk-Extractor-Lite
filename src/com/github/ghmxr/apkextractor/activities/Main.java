package com.github.ghmxr.apkextractor.activities;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.adapters.AppListAdapter;
import com.github.ghmxr.apkextractor.data.AppItemInfo;
import com.github.ghmxr.apkextractor.data.AppItemInfoSortConfig;
import com.github.ghmxr.apkextractor.ui.AppDetailDialog;
import com.github.ghmxr.apkextractor.ui.FileCopyDialog;
import com.github.ghmxr.apkextractor.ui.LoadListDialog;
import com.github.ghmxr.apkextractor.ui.SortDialog;
import com.github.ghmxr.apkextractor.utils.APKChecker;
import com.github.ghmxr.apkextractor.utils.CopyFilesTask;
import com.github.ghmxr.apkextractor.utils.FileSize;
import com.github.ghmxr.apkextractor.utils.PermissionChecker;
import com.github.ghmxr.apkextractor.utils.SearchTask;
import com.github.ghmxr.apkextractor.utils.StorageUtil;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
//import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;


public class Main extends BaseActivity {
	
	boolean showSystemApp=false;
	private PackageManager packagemanager;
	private List<PackageInfo> packagelist;
	private boolean isMultiSelectMode=false,isSearchMode=false;
	private boolean isExtractSuccess=true;
	private boolean []ifneedextract; 
	private String errorMessage="";
	private boolean isCheckPermissionReady=true;
	private Message msg;
	
	ListView applist,applist_searchmode;
	CheckBox cb_showSystemApp;
	RelativeLayout multiselectarea,topArea;
	SwipeRefreshLayout swrlayout;
	TextView appinst,selectall,deselectall,extract,share;	
	Thread thread_getappinfo,thread_search,thread_extractapp;
	CopyFilesTask runnable_extractapp;
	SearchTask runnable_search;
	SearchView searchview;
	InputMethodManager inputmgr;
	ProgressBar pg_search;
	
	Menu menu;	
	
	AppDetailDialog dialog_appdetail;
	LoadListDialog  dialog_loadlist;
	FileCopyDialog  dialog_copyfile;
	SortDialog dialog_sort;
		
	public static final int MESSAGE_SET_NORMAL_TEXTATT				= 0x0012;
	
	public static final int MESSAGE_EXTRACT_SINGLE_APP				= 0x0020;
	public static final int MESSAGE_EXTRACT_MULTI_APP				= 0x0021;
				
	public static final int MESSAGE_SHARE_SINGLE_APP				= 0x0022;
	public static final int MESSAGE_SHARE_MULTI_APP					= 0x0023;
	
	public static final int MESSAGE_SEARCH_COMPLETE					= 0x0030;
	
	public static final int MESSAGE_PERMISSION_DENIED				= 0x0040;
	public static final int MESSAGE_PERMISSION_GRANTED				= 0x0041;
	
	AppListAdapter listadapter;
	
	AdapterView.OnItemClickListener listener_listview_onclick_normalmode=new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
			// TODO Auto-generated method stub						
			if(Main.this.listadapter!=null){
				final List<AppItemInfo> list = Main.this.listadapter.getAppList();							
				
				Main.this.dialog_appdetail=new AppDetailDialog(Main.this);
				Main.this.dialog_appdetail.setTitle(list.get(position).getAppName()+" "+list.get(position).getVersion());		
				Main.this.dialog_appdetail.setIcon(list.get(position).getIcon());
				Main.this.dialog_appdetail.setAppInfo(list.get(position).getVersionCode(), list.get(position).getLastUpdateTime(), list.get(position).getPackageSize());
				if(Build.VERSION.SDK_INT>=24) {
					Main.this.dialog_appdetail.setAPPMinSDKVersion(list.get(position).getMinSDKVersion());
				}						
				Main.this.dialog_appdetail.show();
				
				/*Main.this.dialog_appdetail.listview_operations.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position_dialog, long id) {
						// TODO Auto-generated method stub
						Main.this.dialog_appdetail.cancel();
						
						
						switch(position_dialog){
							default:break;
							case 0:{ //extract								
								boolean isSelected[]=new boolean [list.size()];
								isSelected[position]=true;
								APKChecker apkchecker=new APKChecker(list,isSelected);
								apkchecker.startCheck();								
								if(StorageUtil.getSDAvaliableSize()<(list.get(position).getPackageSize()+1024*1024)){
									showStorageNotEnoughDialog();
								}
								else if(apkchecker.getIsApkAlreadyExist()){
									
									new AlertDialog.Builder(Main.this)
									.setIcon(R.drawable.ic_warn)
									.setTitle("���������ļ�")
									.setCancelable(true)									
									.setMessage("�������������ļ���\n"+apkchecker.getDuplicatedAPKInfo()+"�Ƿ񸲸ǣ�")
									.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// TODO Auto-generated method stub
											Message msg_extract=new Message();
											msg_extract.what=Main.MESSAGE_EXTRACT_SINGLE_APP;
											msg_extract.obj=Integer.valueOf(position);
											Main.this.processExtractMsg(msg_extract);
										}
									})
									.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// TODO Auto-generated method stub
											
										}
									})
									.show();
									
								}								
								else{
									Message msg_extract=new Message();
									msg_extract.what=Main.MESSAGE_EXTRACT_SINGLE_APP;
									msg_extract.obj=Integer.valueOf(position);
									Main.this.processExtractMsg(msg_extract);
								}
								
							}
							break;
							case 1:{ //share
								Message msg_share=new Message();
								msg_share.what=Main.MESSAGE_SHARE_SINGLE_APP;
								msg_share.obj=Integer.valueOf(position);
								Main.sendMessage(msg_share);
								//Main.this.processShareMsg(msg_share);
							}
							break;
							case 2:{ //detail
								Intent appdetail = new Intent();  
								appdetail.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);  
								appdetail.setData(Uri.fromParts("package", list.get(position).getPackageName(), null));   
								startActivity(appdetail); 
							}
							break;
						}
						
					}
				});   */
				Main.this.dialog_appdetail.area_extract.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						//extract
						if(Main.this.dialog_appdetail!=null) Main.this.dialog_appdetail.cancel();
						boolean isSelected[]=new boolean [list.size()];
						isSelected[position]=true;
						APKChecker apkchecker=new APKChecker(list,isSelected);
						apkchecker.startCheck();								
						if(StorageUtil.getSDAvaliableSize()<(list.get(position).getPackageSize()+1024*1024)){
							showStorageNotEnoughDialog();
						}
						else if(apkchecker.getIsApkAlreadyExist()){
							
							new AlertDialog.Builder(Main.this)
							.setIcon(R.drawable.ic_warn)
							.setTitle("���������ļ�")
							.setCancelable(true)									
							.setMessage("�������������ļ���\n"+apkchecker.getDuplicatedAPKInfo()+"�Ƿ񸲸ǣ�")
							.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									Message msg_extract=new Message();
									msg_extract.what=Main.MESSAGE_EXTRACT_SINGLE_APP;
									msg_extract.obj=Integer.valueOf(position);
									Main.this.processExtractMsg(msg_extract);
								}
							})
							.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									
								}
							})
							.show();
							
						}								
						else{
							Message msg_extract=new Message();
							msg_extract.what=Main.MESSAGE_EXTRACT_SINGLE_APP;
							msg_extract.obj=Integer.valueOf(position);
							Main.this.processExtractMsg(msg_extract);
						}
					}										
				});
				
				Main.this.dialog_appdetail.area_share.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if(Main.this.dialog_appdetail!=null) Main.this.dialog_appdetail.cancel();
						Message msg_share=new Message();
						msg_share.what=Main.MESSAGE_SHARE_SINGLE_APP;
						msg_share.obj=Integer.valueOf(position);
						Main.sendMessage(msg_share);
						//Main.this.processShareMsg(msg_share);
					}
				});
				
				Main.this.dialog_appdetail.area_detail.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if(Main.this.dialog_appdetail!=null) Main.this.dialog_appdetail.cancel();
						Intent appdetail = new Intent();  
						appdetail.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);  
						appdetail.setData(Uri.fromParts("package", list.get(position).getPackageName(), null));   
						startActivity(appdetail); 
					}
				});
				
			}												
		}
	};
	
	AdapterView.OnItemClickListener listener_listview_onclick_multiselectmode=new AdapterView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub															
			Main.this.updateMuliSelectMode(position);
		}
	};
		
	AdapterView.OnItemLongClickListener listener_listview_onlongclick=new AdapterView.OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			// TODO Auto-generated method stub
			Main.this.startMultiSelectMode(position);
			return true;
		}
	};
	
	
	View.OnClickListener listener_extract=new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Main.this.closeMultiSelectMode();
					
			if(Main.this.listadapter!=null){
				List<AppItemInfo> list=Main.this.listadapter.getAppList();
				
				APKChecker apkchecker=new APKChecker(list,Main.this.listadapter.getIsSelected()).startCheck();												
				if(StorageUtil.getSDAvaliableSize()<(Main.this.listadapter.getSelectedAppsSize()+1024*1024)){
					showStorageNotEnoughDialog();
				}
				else if(apkchecker.getIsApkAlreadyExist()){
					
					new AlertDialog.Builder(Main.this)
					.setIcon(R.drawable.ic_warn)
					.setTitle("���������ļ�")
					.setCancelable(true)									
					.setMessage("�������������ļ���\n"+apkchecker.getDuplicatedAPKInfo()+"�Ƿ񸲸ǣ�")
					.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Message msg=new Message();
							msg.what=MESSAGE_EXTRACT_MULTI_APP;
							Main.this.processExtractMsg(msg);
						}
					})
					.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					})
					.show();
					
				}								
				else{
					Message msg=new Message();
					msg.what=MESSAGE_EXTRACT_MULTI_APP;
					Main.this.processExtractMsg(msg);
				}
				
			}
												
		}
	};
	View.OnClickListener listener_share=new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
			Main.sendEmptyMessage(MESSAGE_SHARE_MULTI_APP);
			//Message msg_share = new Message();
			//msg_share.what=MESSAGE_SHARE_MULTI_APP;
			//Main.this.processShareMsg(msg_share);
			
		}
	};
	
	DialogInterface.OnClickListener listener_stopbutton=new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			if(Main.this.thread_extractapp!=null){
				if(Main.this.runnable_extractapp!=null){
					Main.this.runnable_extractapp.setInterrupted();
				}
				Main.this.thread_extractapp.interrupt();
				Main.this.thread_extractapp=null;
			}
			Main.this.dialog_copyfile.cancel();
		}
	};
	
	SwipeRefreshLayout.OnRefreshListener listener_swr=new SwipeRefreshLayout.OnRefreshListener() {
		
		@Override
		public void onRefresh() {
			// TODO Auto-generated method stub
			if(Main.this.isMultiSelectMode){
				Main.this.closeMultiSelectMode();
			}
			Main.this.refreshList(false);
		}
	};
	
	
	
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);						
		this.getActionBar().setDisplayShowHomeEnabled(true);
		this.getActionBar().setDisplayShowTitleEnabled(true);
		this.packagemanager=this.getPackageManager();
		this.packagelist = packagemanager.getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
		this.inputmgr=(InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		getView();		
		setActionBarSearchView();
		
		//this.swrlayout.setColorSchemeColors(R.color.color_swiperefresh_color1,R.color.color_swiperefresh_color2,R.color.color_swiperefresh_color3,R.color.color_swiperefresh_color4);
		this.swrlayout.setColorSchemeColors(Color.parseColor(this.getResources().getString(R.color.color_actionbar)));
		this.swrlayout.setSize(SwipeRefreshLayout.DEFAULT);
		this.swrlayout.setDistanceToTriggerSync(100);
		this.swrlayout.setProgressViewEndTarget(false, 200);
		this.swrlayout.setOnRefreshListener(listener_swr);
		
		this.multiselectarea.setVisibility(View.GONE);
		this.topArea.setVisibility(View.VISIBLE);
				
		dialog_loadlist=new LoadListDialog(this);
		dialog_loadlist.setTitle("���ڼ���");
		dialog_loadlist.setCancelable(false);
		dialog_loadlist.setCanceledOnTouchOutside(false);
		dialog_loadlist.setMax(packagelist.size());
		
		//this.initialSelfPermission();	
		this.isCheckPermissionReady=PermissionChecker.isHaveRWPermissions(this);
		if(!this.isCheckPermissionReady){
			PermissionChecker.requestRWPermissions(this);
			this.msg=new Message();
			this.msg.what=MESSAGE_SET_NORMAL_TEXTATT;
		}
		
		refreshList(true);						
		cb_showSystemApp.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			public void onCheckedChanged(CompoundButton button , boolean isChecked){
				if(Main.this.isMultiSelectMode){
					Main.this.closeMultiSelectMode();
				}
				if(isChecked){
					showSystemApp=true;
				}
				else{
					showSystemApp=false;
				}
				refreshList(true);								
			}
		});
										
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if(!this.isSearchMode)
		Main.sendEmptyMessage(Main.MESSAGE_SET_NORMAL_TEXTATT);
	}
					
	private void getView(){
		this.setContentView(R.layout.layout_main);
		cb_showSystemApp=(CheckBox)findViewById(R.id.showSystemAPP);
		cb_showSystemApp.setChecked(false);
		applist=(ListView)findViewById(R.id.applist);
		applist.setDivider(null);
		applist_searchmode=(ListView)findViewById(R.id.applist_searchmode);
		applist_searchmode.setDivider(null);
		multiselectarea=(RelativeLayout)findViewById(R.id.area_multiselect);
		topArea=(RelativeLayout)findViewById(R.id.topArea);
		selectall=(TextView)findViewById(R.id.text_selectall);
		deselectall=(TextView)findViewById(R.id.text_deselectall);
		extract=(TextView)findViewById(R.id.text_extract);
		share=(TextView)findViewById(R.id.text_share);
		appinst=(TextView)findViewById(R.id.appinst);
		pg_search=(ProgressBar)findViewById(R.id.progressbar_search);
		swrlayout=(SwipeRefreshLayout)findViewById(R.id.main_swiperefreshlayout);
	}
	
	private void setActionBarSearchView(){
		View actionbarSearchView = LayoutInflater.from(this).inflate(R.layout.layout_main_actionbar_search, null); 
		this.searchview=(SearchView)actionbarSearchView.findViewById(R.id.actionbar_main_search);
		this.searchview.setIconifiedByDefault(false);
		int id = this.searchview.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);		
		TextView tv_searchview=(TextView)searchview.findViewById(id);
		if(tv_searchview!=null){			
			tv_searchview.setHintTextColor(Color.parseColor(this.getResources().getString(R.color.color_searchview_hinttext)));
		}		
        this.getActionBar().setCustomView(actionbarSearchView);  	
	}
		
	
	private void showSearchView(){
		this.isSearchMode=true;
		listsearch=new ArrayList<AppItemInfo>();
		
		this.listadapter=new AppListAdapter(this,listsearch,false);								
				
		if(this.applist_searchmode!=null){
			this.applist_searchmode.setVisibility(View.VISIBLE);
			this.applist_searchmode.setAdapter(listadapter);
			this.applist_searchmode.setOnItemClickListener(this.listener_listview_onclick_normalmode);
			this.applist_searchmode.setOnItemLongClickListener(null);
		}	
		if(this.swrlayout!=null){
			if(this.applist!=null){				
				this.applist.setAdapter(null);
				this.applist.setOnItemClickListener(null);
				this.applist.setOnItemLongClickListener(null);
			}
			this.swrlayout.setVisibility(View.GONE);			
			this.swrlayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				
				@Override
				public void onRefresh() {
					// TODO Auto-generated method stub
					Main.this.swrlayout.setRefreshing(false);
				}
			});
		}
		if(this.topArea!=null)
			this.topArea.setVisibility(View.GONE);
		
		this.setMenuVisible(false);
		this.getActionBar().setDisplayShowCustomEnabled(true);
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowHomeEnabled(false);				
		this.searchview.setFocusable(true);
		this.searchview.setFocusableInTouchMode(true);
		this.searchview.requestFocus();
		this.searchview.setSubmitButtonEnabled(false);
		
		this.searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				// TODO Auto-generated method stub								
				Main.this.updateSearchList(newText);												
				return true;
			}
		});
		
		
		this.inputmgr.showSoftInput(searchview.findFocus(), 0);
				
	}
	
	private void closeSearchView(){
		this.isSearchMode=false;
		if(this.runnable_search!=null){
			this.runnable_search.setInterrupted();
		}
		if(this.thread_search!=null){
			this.thread_search.interrupt();
			this.thread_search=null;
		}
		
		
		this.listadapter=new AppListAdapter(this,listsum,true);			
		
		
		if(this.pg_search!=null){
			this.pg_search.setVisibility(View.GONE);
		}
		if(this.applist_searchmode!=null){
			this.applist_searchmode.setAdapter(null);
			this.applist_searchmode.setOnItemClickListener(null);
			this.applist_searchmode.setOnItemLongClickListener(null);
			this.applist_searchmode.setVisibility(View.GONE);
		}
			
		
		if(this.swrlayout!=null){
			this.swrlayout.setVisibility(View.VISIBLE);
			this.swrlayout.setOnRefreshListener(this.listener_swr);
		}
		
		if(this.applist!=null){
			this.applist.setAdapter(this.listadapter);
			this.applist.setOnItemClickListener(this.listener_listview_onclick_normalmode);
			this.applist.setOnItemLongClickListener(this.listener_listview_onlongclick);
		}
		this.getActionBar().setDisplayShowCustomEnabled(false);
		this.getActionBar().setDisplayHomeAsUpEnabled(false);
		this.getActionBar().setDisplayShowHomeEnabled(true);
		this.setMenuVisible(true);
		if(this.topArea!=null)
			this.topArea.setVisibility(View.VISIBLE);
		Main.sendEmptyMessage(Main.MESSAGE_SET_NORMAL_TEXTATT);
		this.searchview.setFocusable(false);		
		View view=this.getCurrentFocus();
		if(view!=null){			
			this.inputmgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
						
	}
	
	private void startMultiSelectMode(int position){
		this.isMultiSelectMode=true;
		this.getActionBar().setDisplayHomeAsUpEnabled(true);		
		if(Main.this.listadapter!=null){
			Main.this.listadapter.setMultiSelectMode(position);	
			Main.this.updateMuliSelectMode(position);
			
			if(this.selectall!=null){
				this.selectall.setClickable(true);
				this.selectall.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
				this.selectall.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Main.this.listadapter.selectAll();
						Main.this.updateMuliSelectMode(-1);
					}
				});
			}
			
			if(this.deselectall!=null){
				this.deselectall.setClickable(true);
				this.deselectall.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
				this.deselectall.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Main.this.listadapter.deselectAll();
						Main.this.updateMuliSelectMode(-1);
					}
				});
			}
		}						
					
		if(Main.this.applist!=null){
			Main.this.applist.setOnItemClickListener(listener_listview_onclick_multiselectmode);
			Main.this.applist.setOnItemLongClickListener(null);
		}
		
		if(this.multiselectarea!=null){			
			Animation anim=AnimationUtils.loadAnimation(this, R.anim.anim_multiselectarea_entry);
			this.multiselectarea.startAnimation(anim);
			this.multiselectarea.setVisibility(View.VISIBLE);					
		}
				
						
	}
	
	private void updateMuliSelectMode(int position){
		if(Main.this.listadapter!=null){
			if(position!=-1)
			Main.this.listadapter.onItemClicked(position);
			Main.this.appinst.setText("��ѡ��"+Main.this.listadapter.getSelectedNum()+"��"+"\n"+"���ƴ�С��"+Formatter.formatFileSize(Main.this, listadapter.getSelectedAppsSize()));			
			extract.setText(Main.this.getResources().getString(R.string.button_extract)+"("+this.listadapter.getSelectedNum()+")");
			share.setText(Main.this.getResources().getString(R.string.button_share)+"("+this.listadapter.getSelectedNum()+")");
			extract.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
			extract.getPaint().setAntiAlias(true);
			share.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
			share.getPaint().setAntiAlias(true);
			
			if(Main.this.listadapter.getSelectedNum()>0){
				extract.setClickable(true);
				share.setClickable(true);
				extract.setOnClickListener(listener_extract);
				share.setOnClickListener(listener_share);
				extract.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_true)));
				share.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_true)));
			}
			else{			
				share.setOnClickListener(null);
				extract.setOnClickListener(null);				
				extract.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_false)));
				share.setTextColor(Color.parseColor(this.getResources().getString(R.color.color_text_clickable_false)));
			}
		}
		
		
		
	}

	
	private void closeMultiSelectMode(){		
    	isMultiSelectMode=false; 
    	extract.setOnClickListener(null);
    	share.setOnClickListener(null);
    	selectall.setOnClickListener(null);
    	deselectall.setOnClickListener(null);
    	topArea.setVisibility(View.VISIBLE);
    	Main.sendEmptyMessage(MESSAGE_SET_NORMAL_TEXTATT);
    	if(listadapter!=null){
    		listadapter.cancelMutiSelectMode();
    		applist.setOnItemClickListener(listener_listview_onclick_normalmode);
        	applist.setOnItemLongClickListener(listener_listview_onlongclick);
        	Animation anim=AnimationUtils.loadAnimation(this, R.anim.anim_multiselectarea_exit);
			this.multiselectarea.startAnimation(anim);
        	multiselectarea.setVisibility(View.GONE);
        	Main.this.getActionBar().setDisplayHomeAsUpEnabled(false);       	
    	} 
    	
	}
	
	private void refreshList(boolean ifshowProcessDialog){
		this.packagemanager=this.getPackageManager();
		this.packagelist = packagemanager.getInstalledPackages(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
		applist.setAdapter(null);
		Main.sendEmptyMessage(MESSAGE_SET_NORMAL_TEXTATT);
		cb_showSystemApp.setEnabled(false);
		listsum = new ArrayList<AppItemInfo>();
		//AppItemInfoSortConfig.SortConfig=0;
		if(this.dialog_loadlist!=null&&ifshowProcessDialog){
			this.dialog_loadlist.show();
		}
		this.thread_getappinfo=new Thread(new Runnable(){
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub									                                    
		        for (int i = 0; i < packagelist.size(); i++) {
		            PackageInfo pak = (PackageInfo) packagelist.get(i); 
		            AppItemInfo appitem=new AppItemInfo();			  
		            // if()���ֵ���<=0��Ϊ�Լ�װ�ĳ��򣬷���Ϊϵͳ�����Դ�  
		            if(!Main.this.showSystemApp){
		            	if ((pak.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {                      
				            appitem.setIcon(packagemanager.getApplicationIcon(pak.applicationInfo));  		            
				            appitem.setAppName(packagemanager.getApplicationLabel(pak.applicationInfo).toString());  		             
				            appitem.setPackageName(pak.applicationInfo.packageName);  
				            appitem.setPackageSize(FileSize.getFileSize(pak.applicationInfo.sourceDir));
				            appitem.setResourcePath(pak.applicationInfo.sourceDir);
				            appitem.setVersion(pak.versionName);
				            appitem.setVersionCode(pak.versionCode);
				            appitem.setLastUpdateTime(pak.lastUpdateTime);
				            if(Build.VERSION.SDK_INT>=24)
				            appitem.setMinSDKVersion(pak.applicationInfo.minSdkVersion);
				            listsum.add(appitem);                    
		                }  
		            }
		            else{
		            	appitem.setIcon(packagemanager.getApplicationIcon(pak.applicationInfo));  		            
			            appitem.setAppName(packagemanager.getApplicationLabel(pak.applicationInfo).toString());  		             
			            appitem.setPackageName(pak.applicationInfo.packageName);  
			            appitem.setPackageSize(FileSize.getFileSize(pak.applicationInfo.sourceDir));
			            appitem.setResourcePath(pak.applicationInfo.sourceDir);
			            appitem.setVersion(pak.versionName);
			            appitem.setVersionCode(pak.versionCode);
			            appitem.setLastUpdateTime(pak.lastUpdateTime);
			            if(Build.VERSION.SDK_INT>=24)
			            appitem.setMinSDKVersion(pak.applicationInfo.minSdkVersion);
			            listsum.add(appitem);  
		            }		            
		            Message msg_thisloop = new Message();
		            Integer progress=i+1;           		            
		            msg_thisloop.what=MESSAGE_LOADLIST_REFRESH_PROGRESS;
		            msg_thisloop.obj=progress;
		            Main.sendMessage(msg_thisloop);
		            
		        }		       
		        Main.sendEmptyMessage(MESSAGE_LOADLIST_COMPLETE);
			}
			
		});
		this.thread_getappinfo.start();	
	}
	
	private void updateSearchList(String text){
		final String searchinfo=text.trim().toLowerCase(Locale.ENGLISH);
		this.pg_search.setVisibility(View.VISIBLE);
		this.applist_searchmode.setAdapter(null);				
		if(this.thread_search!=null){
			if(this.runnable_search!=null){
				this.runnable_search.setInterrupted();
			}
			this.thread_search.interrupt();		
			this.thread_search=null;			
		}
		this.runnable_search=new SearchTask(searchinfo);
		this.thread_search=new Thread(this.runnable_search);
		this.thread_search.start();				
				
	}
	
	private void showStorageNotEnoughDialog(){
		new AlertDialog.Builder(Main.this)
		.setTitle(Main.this.getResources().getString(R.string.dialog_storage_notenough_title))
		.setIcon(R.drawable.ic_warn)
		.setCancelable(true)									
		.setMessage(Main.this.getResources().getString(R.string.dialog_storage_notenough_message))
		.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		})									
		.show();
	}
	
	
	private void processExtractMsg(Message msg){
		this.isExtractSuccess=true;
		this.errorMessage="";		 		
		if(msg.what==Main.MESSAGE_EXTRACT_SINGLE_APP||msg.what==Main.MESSAGE_EXTRACT_MULTI_APP){					
			if(!PermissionChecker.isHaveRWPermissions(this)){
				this.isCheckPermissionReady=false;
				this.msg=msg;
				PermissionChecker.requestRWPermissions(this);
			}
			else{
				this.isCheckPermissionReady=true;
				sendMessage(msg);
			}
		}
								
	}
	
//	private void processShareMsg(final Message msg){
	//	sendMessage (msg);
		
		
	//	if(Build.VERSION.SDK_INT<24){   //APIΪAndroid7.0����ʱ������ֱ�ӻ�ȡ�ļ��� uri�����账����
			//��ʱ������Android7.0��Fileprovider ����ʱʹ��targetapi 23 �����з�����
	//	}
	/*	else{
			if(this.listadapter!=null&&(msg.what==MESSAGE_SHARE_SINGLE_APP||msg.what==MESSAGE_SHARE_MULTI_APP)){
				List<AppItemInfo> list = this.listadapter.getAppList();
				if(msg.what==MESSAGE_SHARE_SINGLE_APP&&PermissionChecker.isHaveRWPermissions(Main.this)){
					final AppItemInfo appitem=list.get((Integer)msg.obj);
					File file =new File(SAVEPATH+"/"+appitem.getPackageName()+"-"+appitem.versioncode+".apk");
					if(file.exists()&&!file.isDirectory()){
						sendMessage(msg);
					}
					else{
						this.msg=msg;
						new AlertDialog.Builder(this)
						.setTitle("��Ҫ���� "+appitem.getAppName())
						.setIcon(appitem.getIcon())
						.setMessage("���� "+appitem.getAppName()+" ��Ҫ�ȵ�����Ӧ�ã�ȷ��������")
						.setCancelable(true)
						.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub																
								if(StorageUtil.getSDAvaliableSize()<(appitem.getPackageSize()+1024*1024)){
									Main.this.showStorageNotEnoughDialog();
								}
								else{
									Main.this.msg=msg;
									Message msg_extract = new Message();
									msg_extract.what=Main.MESSAGE_EXTRACT_SINGLE_APP;
									msg_extract.obj=(Integer)msg.obj;
									Main.this.processExtractMsg(msg_extract);
								}
								
							}
						})
						.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						})
						.show();
					}										
					
				}
				else if(msg.what==MESSAGE_SHARE_MULTI_APP&&PermissionChecker.isHaveRWPermissions(Main.this)){
					final APKChecker checker = new APKChecker(this.listadapter.getAppList(),this.listadapter.getIsSelected()).startCheck();								
					if(checker.getIsApkAbsent()){
						new AlertDialog.Builder(this)
						.setTitle("��Ҫ����Ӧ��")
						.setIcon(R.drawable.ic_warn)
						.setMessage("������ЩӦ����Ҫ�ȵ�������Ӧ�ã�\n"+checker.getAbsentAPKInfo()+"�Ƿ������")
						.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								if(StorageUtil.getSDAvaliableSize()<(Main.this.listadapter.getSelectedAppsSize()+1024*1024)){
									Main.this.showStorageNotEnoughDialog();
								}
								else{
									Main.this.msg=msg;
									Message msg_extract_multi = new Message();
									msg_extract_multi.what=Main.MESSAGE_EXTRACT_MULTI_APP;
									Main.this.ifneedextract =checker.getIfNeedExtract();								
									processExtractMsg(msg_extract_multi);
								}
															
							}
						})
						.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
						})
						.show();
					}
					
				}
				else if(!PermissionChecker.isHaveRWPermissions(Main.this)){
					PermissionChecker.requestRWPermissions(Main.this);
				}
			}
		}   */				
	//}
	
	
	public void processMessage(Message msg){		
		switch(msg.what){
			default:break;			
			
			case MESSAGE_SET_NORMAL_TEXTATT:{
				if(this.appinst!=null&&!this.isMultiSelectMode&&!this.isSearchMode){
					if(PermissionChecker.isHaveRWPermissions(this)){
						appinst.setText(Main.this.getResources().getString(R.string.text_appinst)+"\n"+this.getResources().getString(R.string.text_avaliableroom)+Formatter.formatFileSize(this, StorageUtil.getSDAvaliableSize()));
					}
					else{
						appinst.setText(Main.this.getResources().getString(R.string.text_appinst)+"\n"+this.getResources().getString(R.string.text_avaliableroom)+"δ֪");
					}
					
				}				
			}
			break;
			
			case MESSAGE_LOADLIST_COMPLETE:{
				if(this.cb_showSystemApp!=null){
					this.cb_showSystemApp.setEnabled(true);
				}
				
				if(this.dialog_loadlist!=null){
					this.dialog_loadlist.cancel();
				}																		
				
				if(this.applist!=null){
					this.listadapter=new AppListAdapter(this,listsum,true);
					this.applist.setAdapter(listadapter);
					this.applist.setOnItemClickListener(listener_listview_onclick_normalmode);
					this.applist.setOnItemLongClickListener(listener_listview_onlongclick);
					if(AppItemInfoSortConfig.SortConfig!=0){
						Main.this.sortList();
					}
				}
				if(this.swrlayout!=null){
					this.swrlayout.setRefreshing(false);
				}
								
			}
			break;
			case MESSAGE_SEARCH_COMPLETE:{
				
				this.listadapter=new AppListAdapter(this,listsearch,false);
				
				if(this.applist_searchmode!=null){
					this.applist_searchmode.setAdapter(listadapter);
				}
				if(this.pg_search!=null){
					this.pg_search.setVisibility(View.GONE);
				}
				
			}
			break;
			case MESSAGE_LOADLIST_REFRESH_PROGRESS:{
				if(dialog_loadlist!=null){
					Integer progress=(Integer)msg.obj;
					
					dialog_loadlist.setProgress(progress);
				}
			}
			break;
			case MESSAGE_EXTRACT_SINGLE_APP:{
				List<AppItemInfo> list;
				boolean isSelected[];
				Integer position=(Integer)msg.obj;								
				if(this.listadapter!=null){
					list=this.listadapter.getAppList();
					if(list!=null){
						if(list.size()>0){
							isSelected=new boolean[list.size()];
							isSelected[position]=true;
							this.runnable_extractapp=new CopyFilesTask(list,isSelected);
							this.thread_extractapp=new Thread(this.runnable_extractapp);
							this.dialog_copyfile=new FileCopyDialog(this);
							this.dialog_copyfile.setCancelable(false);
							this.dialog_copyfile.setCanceledOnTouchOutside(false);
							this.dialog_copyfile.setMax(list.get(position).getPackageSize());
							this.dialog_copyfile.setIcon(list.get(position).getIcon());
							this.dialog_copyfile.setTitle("���ڵ�����"+list.get(position).getAppName());
							this.dialog_copyfile.setButton(AlertDialog.BUTTON_NEGATIVE, "ֹͣ", this.listener_stopbutton);
							this.dialog_copyfile.show();
							this.thread_extractapp.start();
						}
					}
				}																				
			}			
			break;
			
			case MESSAGE_EXTRACT_MULTI_APP:{							
				if(this.listadapter!=null){
					if(this.ifneedextract!=null){
						Main.this.runnable_extractapp=new CopyFilesTask(Main.this.listadapter.getAppList(),this.ifneedextract);
						this.ifneedextract=null;
					}
					else{
						Main.this.runnable_extractapp=new CopyFilesTask(Main.this.listadapter.getAppList(),Main.this.listadapter.getIsSelected());
					}					
					Main.this.thread_extractapp=new Thread(Main.this.runnable_extractapp);
					Main.this.dialog_copyfile=new FileCopyDialog(Main.this);
					Main.this.dialog_copyfile.setMax(Main.this.listadapter.getSelectedAppsSize());
					Main.this.dialog_copyfile.setTitle("���ڵ���");
					Main.this.dialog_copyfile.setIcon(R.drawable.ic_launcher);
					Main.this.dialog_copyfile.setButton(AlertDialog.BUTTON_NEGATIVE, "ֹͣ", Main.this.listener_stopbutton);
					Main.this.dialog_copyfile.setCancelable(false);
					Main.this.dialog_copyfile.setCanceledOnTouchOutside(false);
					Main.this.dialog_copyfile.show();
					Main.this.thread_extractapp.start();					
				}
			}			
			break;
			
			case MESSAGE_COPYFILE_CURRENTFILE:{
				List<AppItemInfo> list;
				Integer [] num=(Integer [])msg.obj;
				if(this.isSearchMode){
					list=listsearch;
					
				}
				else{
					list=listsum;
				}
				
				
				if(this.dialog_copyfile!=null){
					this.dialog_copyfile.setIcon(list.get(num[1]).getIcon());
					this.dialog_copyfile.setTitle("���ڵ���"+num[0]+"/"+num[2]+" "+list.get(num[1]).getAppName());					
				}
				
			}
			break;
			case MESSAGE_COPYFILE_REFRESH_PROGRESS:{
				Long progress = (Long)msg.obj;
				if(this.dialog_copyfile!=null){
					this.dialog_copyfile.setProgress(progress);
				}
				
			}
			break;
			case MESSAGE_COPYFILE_REFRESH_SPEED:{
				Long speed = (Long)msg.obj;
				if(this.dialog_copyfile!=null){
					this.dialog_copyfile.setSpeed(speed);
				}
			}
			break;
			case MESSAGE_COPYFILE_COMPLETE:{
				if(this.dialog_copyfile!=null){
					this.dialog_copyfile.cancel();
				}
				Toast.makeText(this, "APK Kit - Ӧ���ѵ����� "+SAVEPATH,Toast.LENGTH_LONG).show();
				if(!this.isSearchMode) {
					Main.sendEmptyMessage(Main.MESSAGE_SET_NORMAL_TEXTATT);
				}
				
				if(!this.isExtractSuccess){
					new AlertDialog.Builder(this).setTitle("��ʾ")
					.setIcon(R.drawable.ic_warn)
					.setMessage("����Ӧ��δ�ɹ�������\n"+this.errorMessage+"��������Ӧ���Ѿ�ж�أ���������δ֪����")
					.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
					})
					.show();
				}
				this.isExtractSuccess=true;
				this.errorMessage="";
				//ȷ�����ε������Ƿ���Ҫ������ЩӦ��
			//	if(this.msg!=null){
				//	if((this.msg.what==Main.MESSAGE_SHARE_SINGLE_APP||this.msg.what==Main.MESSAGE_SHARE_MULTI_APP)&&this.isExtractSuccess){
				//		processShareMsg(this.msg);						
			//		}
			//		this.msg=null;
			//	}
					
			}
			break;
			
			case MESSAGE_SHARE_SINGLE_APP:{
				if(this.listadapter!=null){
					Intent intent=new Intent(Intent.ACTION_SEND);
					Integer position = (Integer)msg.obj;
					int pos = position.intValue();
					List<AppItemInfo> list=this.listadapter.getAppList();
					String apkpath=list.get(pos).getResourcePath();
				/*	if(Build.VERSION.SDK_INT>=24){
						apkpath=SAVEPATH+"/"+list.get(position).getPackageName()+"-"+list.get(position).getVersionCode()+".apk";
					}
					else{
						apkpath=list.get(position).getResourcePath();
					}  */
					File apk=new File(apkpath);												
					Uri uri = Uri.fromFile(apk);
					/*if(Build.VERSION.SDK_INT>=24&&apk.exists()&&!apk.isDirectory()){
						uri=FileProvider.getUriForFile(this,"app.mxr.apkextractor.provider", apk);
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); 
						
						
						
						intent.setType("application/vnd.android.package-archive");
						intent.putExtra(Intent.EXTRA_STREAM,uri);
						intent.putExtra(Intent.EXTRA_SUBJECT, "����Ӧ��"); 
						intent.putExtra(Intent.EXTRA_TEXT, "����Ӧ��");  
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(Intent.createChooser(intent,  "����Ӧ��"   )  );
					}
					else if(apk.exists()&&!apk.isDirectory()){
						uri=Uri.fromFile(apk);
						
						
						intent.setType("application/vnd.android.package-archive");
						intent.putExtra(Intent.EXTRA_STREAM,uri);
						intent.putExtra(Intent.EXTRA_SUBJECT, "����Ӧ��"); 
						intent.putExtra(Intent.EXTRA_TEXT, "����Ӧ��");  
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(Intent.createChooser(intent,  "����Ӧ��"   )  );
					}   */
					
					
					
					
					
					intent.setType("application/vnd.android.package-archive");
					intent.putExtra(Intent.EXTRA_STREAM,uri);
					intent.putExtra(Intent.EXTRA_SUBJECT, "���� "+list.get(pos).getAppName()); 
					intent.putExtra(Intent.EXTRA_TEXT, "���� "+list.get(pos).getAppName());  
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(Intent.createChooser(intent,  "���� "+list.get(pos).getAppName()   )  );
					
				}
			}
			break;
			
			case MESSAGE_SHARE_MULTI_APP:{
				if(this.listadapter!=null){
					List<AppItemInfo> list=this.listadapter.getAppList();
					boolean isSelected[] = this.listadapter.getIsSelected();
					Intent intent;
					if(this.listadapter.getSelectedNum()>1){
						intent=new Intent(Intent.ACTION_SEND_MULTIPLE);
						ArrayList<Uri> uris= new ArrayList<Uri>();
						for(int i=0;i<list.size();i++){
							if(isSelected[i]){																
							//	if(Build.VERSION.SDK_INT>=24){
								//	File file = new File(SAVEPATH+"/"+list.get(i).getPackageName()+"-"+list.get(i).getVersionCode()+".apk");
								//	if(file.exists()&&!file.isDirectory()){
								//		uris.add(FileProvider.getUriForFile(Main.this, "app.mxr.apkextractor.provider", file));
								//	}
										
							//	}
								//else{
									File file = new File(list.get(i).getResourcePath());
									if(file.exists()&&!file.isDirectory()){
										uris.add(Uri.fromFile(file));
									}
									
								//}								
							}
						}
						//if(Build.VERSION.SDK_INT>=24){
						//	intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); 
						//}
						intent.putExtra(Intent.EXTRA_STREAM, uris);
						intent.setType("application/vnd.android.package-archive");
						intent.putExtra(Intent.EXTRA_SUBJECT, "����Ӧ��"); 
						intent.putExtra(Intent.EXTRA_TEXT, "����Ӧ��");  
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(Intent.createChooser(intent,  "����Ӧ��"   )  );
						
					}
					else if(this.listadapter.getSelectedNum()==1){
						intent = new Intent(Intent.ACTION_SEND);
						String path="",appname="";
						for(int j=0;j<list.size();j++){
							if(isSelected[j]){
								path=list.get(j).getResourcePath();
								appname=list.get(j).getAppName();
								break;
							}
						}
						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
						intent.setType("application/vnd.android.package-archive");
						intent.putExtra(Intent.EXTRA_SUBJECT, "���� "+appname); 
						intent.putExtra(Intent.EXTRA_TEXT, "���� "+appname);  
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(Intent.createChooser(intent,  "���� "+appname   )  );
					}
															
				}
			}
			break;
			
			case MESSAGE_COPYFILE_FILE_NOTFOUND_EXCEPTION:{
				this.isExtractSuccess=false;
				this.errorMessage+=(String)msg.obj;
				this.errorMessage+="\n";
			break;					
			}
			
			case MESSAGE_COPYFILE_IOEXCEPTION:{
				this.isExtractSuccess=false;
				//Toast.makeText(Main.this, "����IO�쳣", Toast.LENGTH_SHORT).show();
			}
			break;
			
			case MESSAGE_PERMISSION_DENIED:{
				Main.this.isCheckPermissionReady=false;
				//Toast.makeText(Main.this, "δ��ȡ����дȨ�ޣ����鱾Ӧ��Ȩ��", Toast.LENGTH_LONG).show();
				new AlertDialog.Builder(Main.this)
				.setTitle("δ��ö�дȨ��")
				.setIcon(R.drawable.ic_warn)
				.setMessage("δ��ȡ����дȨ�ޣ����鱾Ӧ��Ȩ��Ȼ�����ԡ�")
				.setCancelable(true)
				.setPositiveButton("ǰ������", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Intent appdetail = new Intent();  
						appdetail.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);  
						appdetail.setData(Uri.fromParts("package", Main.this.getApplicationContext().getPackageName(), null));   
						startActivity(appdetail); 
						//Main.this.finish();
						
					}
				})			
				.show();
			}
			break;
			case MESSAGE_PERMISSION_GRANTED:{
				this.isCheckPermissionReady=true;
				if(this.msg!=null){
					if(this.msg.what==MESSAGE_EXTRACT_SINGLE_APP||this.msg.what==MESSAGE_EXTRACT_MULTI_APP){
						processExtractMsg(this.msg);
						this.msg=null;
					}
					else if(this.msg.what==MESSAGE_SET_NORMAL_TEXTATT){
						sendMessage(this.msg);
						this.msg=null;
					}
					
				}
			}
			break;
		}
	
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {    
            if (this.isMultiSelectMode){
            	closeMultiSelectMode();
            } else if(this.isSearchMode){
            	closeSearchView();
            }
            else{           	
            	this.finish();            	
            }
            return true;
        }
		else {    
           return super.onKeyDown(keyCode, event);    
       }  
	}
	
	private void setMenuVisible(boolean isVisible){
		if(this.menu!=null){
			for(int i=0;i<this.menu.size();i++){
				this.menu.getItem(i).setEnabled(isVisible);
				this.menu.getItem(i).setVisible(isVisible);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.		
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu=menu;
		return true;
	}
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu)  {  
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {  
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {  
                try {  
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);  
                    m.setAccessible(true);  
                    m.invoke(menu, true);  
                } catch (Exception e) {  
                }  
            }  
        }  
        return super.onMenuOpened(featureId, menu);  
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_about) {
			AlertDialog dialog_about=new AlertDialog.Builder(Main.this)
					.setTitle(this.getResources().getString(R.string.dialog_about_title))
					.setIcon(R.drawable.ic_apkext)
					.setCancelable(true)			
					.setMessage(this.getResources().getString(R.string.dialog_about_message))
					.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							//Do nothing
						}
					}).create();
			
			dialog_about.show();
			return true;
		}
		if(id==android.R.id.home){
			if(isMultiSelectMode){
				closeMultiSelectMode();
			}else if(isSearchMode){
				closeSearchView();
			}
		
			return true;
		}
		
		if(id==R.id.action_search){
			//this.isSearchMode=true;
			if(this.isMultiSelectMode){
				this.closeMultiSelectMode();
			}			
			showSearchView();
			
		}
		if(id==R.id.action_sort){
			this.dialog_sort=new SortDialog(this);
			this.dialog_sort.show();
			
			this.dialog_sort.r_default.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(Main.this.isMultiSelectMode){
						Main.this.closeMultiSelectMode();
					}					
					AppItemInfoSortConfig.SortConfig=0;
					Main.this.applist.setAdapter(null);
					Main.this.refreshList(true);					
					Main.this.dialog_sort.cancel();
				}
			});
			
			
			this.dialog_sort.r_a_appname.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfoSortConfig.SortConfig=1;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			
			this.dialog_sort.r_d_appname.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfoSortConfig.SortConfig=2;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_a_size.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfoSortConfig.SortConfig=3;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_d_size.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfoSortConfig.SortConfig=4;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_a_date.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfoSortConfig.SortConfig=5;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
			this.dialog_sort.r_d_date.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					AppItemInfoSortConfig.SortConfig=6;
					Main.this.sortList();
					Main.this.dialog_sort.cancel();
				}
			});
			
		}
		
		if(id==R.id.action_editpath){
			if(!PermissionChecker.isHaveRWPermissions(this)){
				PermissionChecker.requestRWPermissions(this);
			}
			else{
				Intent i = new Intent();
				i.setClass(this, FolderSelector.class);
				startActivity(i);
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void sortList(){
		if(Main.this.listadapter!=null&&!Main.this.isSearchMode){
			if(Main.this.isMultiSelectMode){
				Main.this.closeMultiSelectMode();
			}
			Collections.sort(listsum);
			Main.this.listadapter=new AppListAdapter(this,listsum,true);
			if(this.applist!=null){
				this.applist.setAdapter(this.listadapter);
			}
		}
	}
	
	@Override
	public void finish(){
		super.finish();
		AppItemInfoSortConfig.SortConfig=0;
	}
	
}