package com.echsylon.atlantis.extras;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * This custom {@link DialogPreference} enables configuring a configuration
 * source for Atlantis. Valid sources are {@code "asset://..."} references,
 * {@code "file://..."} paths or {@code "http://..."} URL's.
 */
class AtlantisConfigurationPreference extends DialogPreference {

    /**
     * A custom parcelable class persisting the current instance state of this
     * preference object.
     */
    private static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        private String text;

        private SavedState(Parcel source) {
            super(source);
            text = source.readString();
        }

        private SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(text);
        }
    }

    private TypeAdapter typeAdapter;
    private Spinner typeSelector;
    private EditText pathTextBox;

    private String summaryPattern;
    private String value;

    AtlantisConfigurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        CharSequence summary = getSummary();
        summaryPattern = summary != null ?
                summary.toString() :
                null;
        setSummary(getPersistedString(""));
        return view;
    }

    @Override
    protected View onCreateDialogView() {
        View view = super.onCreateDialogView();
        typeAdapter = new TypeAdapter(getContext());
        typeSelector = (Spinner) view.findViewById(R.id.type);
        typeSelector.setAdapter(typeAdapter);
        pathTextBox = (EditText) view.findViewById(R.id.text);
        return view;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        setValue(getPersistedString(""));
    }


    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            TypeAdapter.Type type = (TypeAdapter.Type) typeSelector.getSelectedItem();
            String path = pathTextBox.getText().toString();
            String value = type.value + path;

            if (callChangeListener(value)) {
                setValue(value);
                setSummary(String.format(summaryPattern, value));
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean doRestore, Object defaultValue) {
        setValue(doRestore ?
                getPersistedString(value) :
                (String) defaultValue);
    }

    @Override
    public boolean shouldDisableDependents() {
        return value == null || value.isEmpty() || super.shouldDisableDependents();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.text = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.text);
    }

    /**
     * Updates the UI and persists the given value.
     *
     * @param text The new configuration source to show and persist.
     */
    void setValue(String text) {
        value = text;
        persistString(value);
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();

        int splitIndex = text.indexOf("://");
        if (splitIndex > 0) {
            splitIndex += 3;

            String path = text.substring(splitIndex);
            if (pathTextBox != null)
                pathTextBox.setText(path);

            String scheme = text.substring(0, splitIndex);
            if (typeSelector != null)
                for (int i = 0, c = typeAdapter.getCount(); i < c; i++) {
                    TypeAdapter.Type type = typeAdapter.getItem(i);
                    if (type != null && scheme.equals(type.value))
                        typeSelector.setSelection(i);
                }
        }
    }

    /**
     * Returns the current value of this preference.
     *
     * @return The current configuration source.
     */
    String getValue() {
        return value;
    }

}
