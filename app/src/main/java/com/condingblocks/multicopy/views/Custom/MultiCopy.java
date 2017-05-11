package com.condingblocks.multicopy.views.Custom;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.condingblocks.multicopy.Adapters.CopyDataAdapter;
import com.condingblocks.multicopy.Interfaces.RemoveCallback;
import com.condingblocks.multicopy.R;
import com.condingblocks.multicopy.Utils.Serializer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;

import java.util.ArrayList;

/**
 * Created by rishabhkhanna on 08/05/17.
 */

public class MultiCopy extends View {
    private Context mContext;
    private View view;
    private NativeExpressAdView adView;
    RecyclerView rvText;
    TextView tvJustCopied;
    ImageView imClear;
    FrameLayout flNewClip;
    ArrayList<String> list;
    CopyDataAdapter copyDataAdapter;
    LinearLayoutManager linearLayoutManager;
    public static final String TAG = "multi copy view";

    public MultiCopy(Context context) {
        super(context);
        mContext = context;

    }

    public View addToWindowManager(String copiedText, final RemoveCallback removeCallback) {
        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = li.inflate(R.layout.dialog_layout, null);
        adView = (NativeExpressAdView) view.findViewById(R.id.adView);
        rvText = (RecyclerView) view.findViewById(R.id.rvList);
        tvJustCopied = (TextView) view.findViewById(R.id.multicopy);
        imClear = (ImageView) view.findViewById(R.id.ivClear);
        flNewClip = (FrameLayout) view.findViewById(R.id.flNewClip);
        imClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCallback.onViewRemoved();
            }
        });

        flNewClip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearArrayData();
            }
        });

        tvJustCopied.setText(copiedText);
        list = Serializer.getStringFromSharedPrefs(mContext);

        copyDataAdapter = new CopyDataAdapter(list, mContext);
        linearLayoutManager = new LinearLayoutManager(mContext);
        rvText.setLayoutManager(linearLayoutManager);
        rvText.setAdapter(copyDataAdapter);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("YOUR_DEVICE_ID")
                .build();
        adView.loadAd(adRequest);
        return view;
    }

    public void clearArrayData() {
        list.clear();
        Serializer.setStringToArrayPrefs(mContext, list);
        Log.d(TAG, "clearArrayData: " + list.toString());
        copyDataAdapter.notifyDataSetChanged();
    }
}
