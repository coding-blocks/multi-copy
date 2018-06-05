package com.condingblocks.multicopy.views.Custom;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.condingblocks.multicopy.Adapters.CopyDataAdapter;
import com.condingblocks.multicopy.Interfaces.RemoveCallback;
import com.condingblocks.multicopy.R;

import com.condingblocks.multicopy.Services.TextCaptureService;
import com.condingblocks.multicopy.Utils.Constants;
import com.condingblocks.multicopy.Utils.Serializer;
import com.condingblocks.multicopy.model.ClipboardTextModel;
import com.condingblocks.multicopy.model.NotesModel;
import com.condingblocks.multicopy.views.Activities.BaseActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by rishabhkhanna on 08/05/17.
 */

public class MultiCopy extends View {
    private Context mContext;
    private View view;
    private AdView adView;
    RecyclerView recyclerView;
    TextView tvJustCopied;
    ImageView imClear;
    FrameLayout flNewClip;
    FrameLayout flSaveNotes;
    FrameLayout flSmartCopy;
    ArrayList<String> list;
    CopyDataAdapter copyDataAdapter;
    LinearLayoutManager linearLayoutManager;
    ImageView settingIV;
    boolean smartCopyToggle = false;
    TextView tvsmartCopy;
    ClipboardManager clipboardManager;
    SharedPreferences sharedPreferences;
    public static final String TAG = "multi copy view";
    public static final boolean custom_LOG = false;

    public MultiCopy(Context context) {
        super(context);
        mContext = context;

    }

    public View addToWindowManager(final String copiedText, final RemoveCallback removeCallback) {
        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        clipboardManager = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        view = li.inflate(R.layout.dialog_layout, null);
        adView = (AdView) view.findViewById(R.id.adView);
        recyclerView = (RecyclerView) view.findViewById(R.id.rvList);
        tvJustCopied = (TextView) view.findViewById(R.id.multicopy);
        imClear = (ImageView) view.findViewById(R.id.ivClear);
        flNewClip = (FrameLayout) view.findViewById(R.id.flNewClip);
        flSaveNotes = (FrameLayout) view.findViewById(R.id.flTakeNotes);
        flSmartCopy = (FrameLayout) view.findViewById(R.id.flSmartCopy);
        tvsmartCopy = (TextView) view.findViewById(R.id.tvSmartCopy);
        settingIV = (ImageView) view.findViewById(R.id.IVsetting);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        checkSmartCopy();
        Realm.init(mContext);


        flSmartCopy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                smartCopyToggle = sharedPreferences.getBoolean(Constants.SMART_COPY_PREFS,false);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Intent i = new Intent(mContext, TextCaptureService.class);
                if (smartCopyToggle){
                    // smart copy is on
                    smartCopyToggle = false;
                    if(custom_LOG)
                    Log.d(TAG, "onClick: true");

                    mContext.startService(i);
                }else{
                    //smart copy is off
                    smartCopyToggle = true;

                    Log.d(TAG, "onClick: false");
                    mContext.stopService(i);
                }
                editor.putBoolean(Constants.SMART_COPY_PREFS,smartCopyToggle);
                if(editor.commit()){
                    checkSmartCopy();
                };
                if(custom_LOG)
                Log.d(TAG, "onClick: SmartCopy");

            }
        });

        imClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCallback.onViewRemoved();
            }
        });

        final Realm realm = Realm.getDefaultInstance();
        final ClipboardTextModel thisText = Serializer.getStringFromSharedPrefs(mContext);


        flNewClip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearArrayData();
            }
        });

        flSaveNotes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {


                realm.beginTransaction();
                //do all the realm saving work here
                NotesModel notesModel = realm.createObject(NotesModel.class);
                notesModel.setNote(thisText.getText());
                notesModel.setCreatedAt(DateFormat.getDateTimeInstance().format(new Date()));
                realm.commitTransaction();
                if(custom_LOG)
                Log.d(TAG, "onClick: saved to DB ");
            }
        });

        settingIV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, BaseActivity.class);
                mContext.startActivity(i);
            }
        });

        tvJustCopied.setText(copiedText);

        list = thisText.getTextArrayList();
        ClipData thisClip = ClipData.newPlainText(Constants.label, thisText.getText());
        clipboardManager.setPrimaryClip(thisClip);
        copyDataAdapter = new CopyDataAdapter(list, mContext);
        linearLayoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(copyDataAdapter);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);



        return view;
    }

    public void clearArrayData() {
        list.clear();
        Serializer.setStringToArrayPrefs(mContext, list);
        if(custom_LOG)
        Log.d(TAG, "clearArrayData: " + list.toString());
        copyDataAdapter.notifyDataSetChanged();
    }

    public void checkSmartCopy(){
        boolean copyToggle = sharedPreferences.getBoolean(Constants.SMART_COPY_PREFS,true);
        if(custom_LOG)
        Log.d(TAG, "checkSmartCopy: " + checkServiceRunning());
        if(copyToggle){
            tvsmartCopy.setText("Enable\nSmart Copy");
        }else{
            tvsmartCopy.setText("Disable\nSmart Copy");
        }
    }

    public boolean checkServiceRunning(){
        Boolean check = false;
            ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if(custom_LOG)
                Log.d(TAG, "checkServiceRunning: " + service.service.getPackageName());
            }
        return check;
    }



}
