package com.blackberrykeyboard;

import android.widget.AdapterView;

abstract class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public abstract void onItemSelected(int position);

    @Override
    public final void onItemSelected(AdapterView<?> parent, android.view.View view,
                                      int position, long id) {
        onItemSelected(position);
    }
}
