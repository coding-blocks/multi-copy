package com.condingblocks.multicopy.views.Fragments;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.condingblocks.multicopy.Adapters.NotesAdapter;
import com.condingblocks.multicopy.Interfaces.RecyclerClick_Listener;
import com.condingblocks.multicopy.Interfaces.onNewNote;
import com.condingblocks.multicopy.Interfaces.onNotesEdit;
import com.condingblocks.multicopy.R;

import com.condingblocks.multicopy.Utils.Constants;
import com.condingblocks.multicopy.Utils.RecyclerTouchListener;
import com.condingblocks.multicopy.Utils.Toolbar_ActionMode_Callback;
import com.condingblocks.multicopy.model.NotesModel;
import com.condingblocks.multicopy.views.Activities.NewNoteActivity;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * A simple {@link Fragment} subclass.
 */
public class NotesFragment extends Fragment implements onNotesEdit,onNewNote{

    RecyclerView recyclerView;
    NotesAdapter notesAdapter;
    FloatingActionButton fabNewNote;
    ActionMode actionMode;
    ArrayList<NotesModel> notesList = new ArrayList<>();
    public static final String TAG = "NotesFragment";
    public static final boolean notesFragment_LOG = false;
    public NotesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_notes, container, false);
        fabNewNote = (FloatingActionButton) root.findViewById(R.id.fabNewNote);
        recyclerView = (RecyclerView) root.findViewById(R.id.rvNotesList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        notesList = new ArrayList<>();
        if (notesFragment_LOG)
        Log.d(TAG, "onCreateView: " + notesList.toString());
        notesAdapter = new NotesAdapter(notesList,getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(notesAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        fabNewNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(),NewNoteActivity.class);
                startActivityForResult(i,Constants.NEW_NOTE_ACTIVTY_KEY);
            }
        });
        refreshData();
        implementREcyclerViewClickListener();
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //since adapter is calling an activity
        if (notesFragment_LOG)
        Log.d(TAG, "onActivityResult: ");
        if(requestCode == Constants.NOTES_RESULT){
            Log.d(TAG, "onActivityResult: ");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData(){
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<NotesModel> notesModelRealmQuery = realm.where(NotesModel.class);
        RealmResults<NotesModel> query = notesModelRealmQuery.findAll();
        notesList.clear();
        notesList.addAll(query);
        notesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onNotesEdit(String notes, int position) {
        if (notesFragment_LOG)
        Log.d(TAG, "onNotesEdit: " + notes);

        //vasudev13: Avoiding editing of existing note to blank note

        if(!notes.isEmpty()) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            notesList.get(position).setNote(notes);
            realm.copyToRealm(notesList.get(position));
            notesAdapter.notifyItemChanged(position);
            realm.commitTransaction();
        }
    }

    @Override
    public void onNewNote(String note,String createdAt) {
        if (notesFragment_LOG)
        Log.d(TAG, "onNewNote: " + note  +  " " + createdAt);

        //vasudev13: Avoiding addition of blank notes to list

        if(!note.isEmpty() && !createdAt.isEmpty()) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            notesList.add(new NotesModel(note, createdAt));
            realm.copyToRealmOrUpdate(notesList);
            realm.commitTransaction();
            notesAdapter.notifyDataSetChanged();
        }
        Log.d("onNewNote",notesList.size()+"");
    }

    //deletefrom Notes List
    public void deleteFromNoteRealm(final int position){
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                NotesModel thisNote = notesList.get(position);
                thisNote.deleteFromRealm();
            }
        });
        notesList.remove(position);
    }

    //Implement long item clicked and on long item clicked
    public void implementREcyclerViewClickListener(){
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new RecyclerClick_Listener() {
            @Override
            public void onClick(View view, int position) {
                    if(actionMode != null ){
                        onListItemSelect(position);
                    }
            }

            @Override
            public void onLongClick(View view, int position) {
                onListItemSelect(position);

            }
        }));
    }

    //ListItem select method
    private void onListItemSelect(int position){
        notesAdapter.toggleSelection(position);
        boolean hasCheckedItems = notesAdapter.getSelectedCount() > 0 ; //check if any items are already selected

        if(hasCheckedItems && actionMode == null){
            //there are some items selected start the action mode
            actionMode = ((AppCompatActivity)getActivity()).startSupportActionMode(new Toolbar_ActionMode_Callback(getActivity(),notesAdapter,notesList,this));
        }else if(!hasCheckedItems && actionMode != null){
            //there are no items selected finish the action mode
            actionMode.finish();
        }
        if(actionMode != null)
            actionMode.setTitle(String.valueOf(notesAdapter.getSelectedCount()) + " selected");
    }

    //set Action mode null after use
    public void setNullToActionMode(){
        if (actionMode != null){
            actionMode = null;
        }
    }

    //Delete selected rows
    public void deleteRows(){
        //get Selected items
        SparseBooleanArray selected = notesAdapter.getmSelectedItems();

        //loop through selected items
        for (int i = (selected.size()-1) ;i >=0 ; i--){
            if (selected.valueAt(i)){
                //If the current id is selected remove the item via key
                deleteFromNoteRealm(selected.keyAt(i));
                notesAdapter.notifyDataSetChanged();
             }
        }

//        Toast.makeText(getContext(), selected.size() + " items deleted", Toast.LENGTH_SHORT).show();
        Snackbar snackbar = Snackbar.make(recyclerView,selected.size() + " Notes deleted",Snackbar.LENGTH_SHORT);
        snackbar.show();
        actionMode.finish();

    }

}
