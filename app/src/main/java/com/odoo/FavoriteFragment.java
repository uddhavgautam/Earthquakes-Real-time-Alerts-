package com.odoo;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.liveEarthquakesAlerts.R;
import com.odoo.orm.ListRow;
import com.odoo.orm.OListAdapter;
import com.odoo.table.RecentContact;
import com.odoo.table.ResPartner;
import com.odoo.utils.BitmapUtils;


/**
 * A simple {@link Fragment} subclass.
 */
public class FavoriteFragment extends Fragment implements OListAdapter.OnViewBindListener,
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private ResPartner resPartner;
    private OListAdapter oListAdapter;
    private ListView favContactList;
    private RecentContact recentContact;

    public FavoriteFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Nullable
    @Override
    public View getView() {
        return super.getView();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourite, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        resPartner = new ResPartner(getContext());
        recentContact = new RecentContact(getContext());
        favContactList = (ListView) view.findViewById(R.id.favContactList);
        oListAdapter = new OListAdapter(getContext(), null, R.layout.favourite_list_item);
        oListAdapter.setOnViewBindListener(this);
        favContactList.setAdapter(oListAdapter);
        favContactList.setOnItemClickListener(this);
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public void onViewBind(View view, Cursor cursor, ListRow row) {
        TextView textContactName, textContactEmail, textContactCity, textContactNumber;
        ImageView profileImage, isCompany;

        textContactName = (TextView) view.findViewById(R.id.textViewName);
        textContactEmail = (TextView) view.findViewById(R.id.textViewEmail);
        textContactCity = (TextView) view.findViewById(R.id.textViewCity);
        textContactNumber = (TextView) view.findViewById(R.id.textViewContact);
        profileImage = (ImageView) view.findViewById(R.id.profile_image);
//        isCompany = (ImageView) view.findViewById(R.id.isCompany);

        String stringName, stringEmail, stringCity, stringMobile, stringImage, stringCompanyType;

        stringName = row.getString("name");
        stringEmail = row.getString("email");
        stringCity = row.getString("city");
        stringMobile = row.getString("mobile");
        Log.i("Mobile", stringMobile);
        //write mobile number to bean and get it from there
        FavoriteNumberBean favoriteNumberBean = new FavoriteNumberBean(); //clears the list first
        favoriteNumberBean.addToArrayList(stringMobile);
        stringImage = row.getString("image_medium");
        stringCompanyType = row.getString("company_type");

        textContactName.setText(stringName);
        textContactEmail.setText(stringEmail);
        textContactEmail.setVisibility(stringEmail.equals("false") ? View.GONE : View.VISIBLE);

        textContactCity.setText(stringCity);
        textContactCity.setVisibility(stringCity.equals("false") ? View.GONE : View.VISIBLE);

        textContactNumber.setText(stringMobile);
        textContactNumber.setVisibility(stringMobile.equals("false") ? View.GONE : View.VISIBLE);

        if (stringImage.equals("false")) {
            profileImage.setImageBitmap(BitmapUtils.getAlphabetImage(getContext(), stringName));
        } else {
            profileImage.setImageBitmap(BitmapUtils.getBitmapImage(getContext(),
                    stringImage));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) { //loader are threads, therefore, we see various callbacks
        Uri uri = Uri.parse("content://com.odoo.contacts.res_partner/res_partner");
        return new CursorLoader(getContext(), uri, null, "isEmergency = ? ", new String[]{"true"}, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        oListAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        oListAdapter.changeCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cr = (Cursor) oListAdapter.getItem(position);

        Intent intent = new Intent(getActivity(), ContactDetailActivity.class);
        intent.putExtra("id", cr.getInt(cr.getColumnIndex("_id")));
        ContentValues values = new ContentValues();
        values.put("contact_id", cr.getInt(cr.getColumnIndex("_id")));
        recentContact.update_or_create(values, "contact_id = ? ", cr.getInt(cr.getColumnIndex("_id")) + "");
        getContext().getContentResolver().notifyChange(resPartner.uri(), null);
        startActivity(intent);
    }
}
