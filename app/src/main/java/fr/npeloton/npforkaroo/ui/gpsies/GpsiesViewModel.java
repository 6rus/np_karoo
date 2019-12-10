package fr.npeloton.npforkaroo.ui.gpsies;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GpsiesViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public GpsiesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}