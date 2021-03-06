package com.oandmdigital.radioplayer.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.oandmdigital.radioplayer.R;
import com.oandmdigital.radioplayer.event.CategoryOnClickEvent;
import com.oandmdigital.radioplayer.event.DownloadCategoriesEvent;
import com.oandmdigital.radioplayer.model.Category;
import com.oandmdigital.radioplayer.network.DownloadCategoryThread;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class CategoryFragment extends Fragment {

    OnCategoryItemSelectedListener callback;

    private static final String SAVED_CATEGORY_LIST = "list";
    private ListView listview;
    private List<Category> items;
    private CategoryAdapter adapter;


    public interface OnCategoryItemSelectedListener {
        void OnCategoryItemClickSelected(Category category);
    }


    public static CategoryFragment newInstance() {

        return new CategoryFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new DownloadCategoryThread("Download_category_thread", getActivity().getApplicationContext()).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        listview = (ListView) inflater.inflate(R.layout.list_view, container, false);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Category category = (Category) parent.getItemAtPosition(position);

                // pass the onClick event up to the hosting activity to deal with
                // EventBus.getDefault().post(new CategoryOnClickEvent(category));

                // pass the onClick event up to the hosting activity via the interface
                callback.OnCategoryItemClickSelected(category);

            }
        });


        // restore the category list from saved state on device rotation
        if(savedInstanceState != null) {
            List<Category> list = savedInstanceState.getParcelableArrayList(SAVED_CATEGORY_LIST);
            if(list != null) {
                adapter = new CategoryAdapter(list);
                listview.setAdapter(adapter);
            }
        }

        return listview;
    }


    @Override
    public void onStart() {
        super.onStart();
        if(getActivity().getSupportFragmentManager().findFragmentById(R.id.station_list) != null) {
            // in dual pane layout, enable single list items to be selected
            // set choice_mode_single on ListView in xml to affect phone/tablet
            listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }


    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }


    @SuppressWarnings("unused")
    public void onEventMainThread(DownloadCategoriesEvent event) {
        // bind the category list to the adapter and display
        items = event.getCategoryList();
        adapter = new CategoryAdapter(items);
        listview.setAdapter(adapter);
    }


    // save the items list on device configuration change
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_CATEGORY_LIST, (ArrayList<? extends Parcelable>) items);
    }


    // onAttach() only called in API <23 when using support.v4.app.Fragment
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // ensure that the hosting activity implements the interface
        Activity activity;
        if(context instanceof Activity) {
            activity = (Activity) context;

            try {
                callback = (OnCategoryItemSelectedListener) activity;
            }
            catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement OnCategoryItemSelectedListener");
            }
        }

    }

    // ArrayAdapter which implements a custom list item and view holder pattern
    private class CategoryAdapter extends ArrayAdapter<Category>{

        // pass in the context and data set to the constructor
        public CategoryAdapter(List<Category> items) {
            super(getActivity(), 0, items);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView == null)
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item, parent, false);

            ViewHolder holder = (ViewHolder) convertView.getTag();
            if(holder == null) {
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            }

            // populate the holder elements
            Category item = getItem(position);
            holder.name.setText(item.getTitle());

            String description = item.getDescription();
            if(description == null || description.isEmpty())
                description = item.getSlug();
            holder.description.setText(description);

            return convertView;
        }
    }



    private class ViewHolder {

        TextView name;
        TextView description;

        public ViewHolder(View row) {
            name = (TextView) row.findViewById(R.id.name);
            description = (TextView) row.findViewById(R.id.description);
        }

    }



}
