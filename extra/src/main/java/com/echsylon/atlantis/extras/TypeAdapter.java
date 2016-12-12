package com.echsylon.atlantis.extras;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * This class manages the configuration source types, available to use in the
 * {@link AtlantisConfigurationPreference}.
 */
class TypeAdapter extends ArrayAdapter<TypeAdapter.Type> {

    /**
     * This enum describes all configuration source types supported.
     */
    enum Type {
        ASSET(R.string.asset, "asset://"),
        FILE(R.string.file, "file://"),
        URL(R.string.url, "http://");

        final int labelResId;
        final String value;

        Type(int label, String value) {
            this.labelResId = label;
            this.value = value;
        }
    }

    TypeAdapter(Context context) {
        super(context, View.NO_ID, Type.values());
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Type type = getItem(position);
        TextView textView = getView(convertView, android.R.layout.simple_spinner_item, parent);
        textView.setText(type != null ? type.value : null);
        return textView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        Type type = getItem(position);
        TextView textView = getView(convertView, android.R.layout.simple_spinner_dropdown_item, parent);
        textView.setText(type != null ? type.labelResId : -1);
        return textView;
    }

    /**
     * Returns a {@code TextView} to use in the type spinner. If the provided
     * recycler view is null, then a new view will be inflated and returned
     * instead.
     *
     * @param recyclerView The view to return if not null.
     * @param layoutId     The id of the view layout to inflate if recycler view
     *                     is null.
     * @param parent       The parent view group to inflate the new layout in.
     * @return A non-null text view.
     */
    private TextView getView(View recyclerView, int layoutId, ViewGroup parent) {
        return recyclerView == null ?
                (TextView) LayoutInflater.from(getContext()).inflate(layoutId, parent, false) :
                (TextView) recyclerView;
    }

}
