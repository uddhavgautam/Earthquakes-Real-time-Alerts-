package com.odoo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by uddhav on 3/26/17.
 */

public class FavoriteNumberBean {
    private ArrayList<String> mobileNumber = new ArrayList<>();
    private boolean isToClear = false;

    public FavoriteNumberBean(boolean isToClear) {
        this.isToClear = isToClear;
    }

    public ArrayList<String> getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(ArrayList<String> mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public FavoriteNumberBean() {
        mobileNumber.clear();
    }

    public void addToArrayList(String stringMobile) {
        mobileNumber.add(stringMobile);
    }
}
