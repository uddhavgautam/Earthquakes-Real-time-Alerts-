package com.liveEarthquakesAlerts.controller.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.liveEarthquakesAlerts.R;
import com.liveEarthquakesAlerts.model.database.EarthQuakes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by  Uddhav Gautam  on 7.3.2016. upgautam@ualr.edu
 */
public class ListViewAdapter extends ArrayAdapter<EarthQuakes> { // just to GUI present for earthquake records in ListView

    private static LayoutInflater inflater = null;
    private List<EarthQuakes> list;
    private Activity act;

    public ListViewAdapter(Activity activity, List<EarthQuakes> datas) {
        super(activity, R.layout.list_row, datas);
        list = datas;
        act = activity;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = convertView;


        if (convertView == null)
            vi = inflater.inflate(R.layout.list_row, null);

        TextView tvLocation = (TextView) vi.findViewById(R.id.tv_location);
        TextView tvDate = (TextView) vi.findViewById(R.id.tv_date);
        TextView tvDepth = (TextView) vi.findViewById(R.id.tv_depth);
        TextView tvMag = (TextView) vi.findViewById(R.id.tv_mag);

        EarthQuakes earthQuake = list.get(position);
        tvLocation.setText(earthQuake.getLocationName());
        Long milis = earthQuake.getDateMilis();

//        DateFormat format = DateFormat.getDateInstance();
//        String date = format.format(milis);
//        long x = milis / 1000;
//        long seconds = x % 60;
//        x /= 60;
//        long minutes = x % 60;
//        x /= 60;
//        long hours = x % 24;

        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss a",
                java.util.Locale.getDefault());

        tvDate.setText(": " + format.format(new Date(milis)));
        tvDepth.setText(": " + Float.toString(earthQuake.getDepth()) + " KM");
        tvMag.setText(Float.toString(earthQuake.getMagnitude()));

        float magnitude = earthQuake.getMagnitude();

        if (magnitude < 3) {
            tvMag.setBackgroundColor(act.getResources().getColor(R.color.COLOR_GREEN));
        } else if (magnitude >= 3 && magnitude < 5) {
            tvMag.setBackgroundColor(act.getResources().getColor(R.color.COLOR_YELLOW));
        } else if (magnitude >= 5) {
            tvMag.setBackgroundColor(act.getResources().getColor(R.color.COLOR_RED));
        }

        return vi;
    }

    @Override
    public EarthQuakes getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getPosition(EarthQuakes item) {
        return super.getPosition(item);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

}